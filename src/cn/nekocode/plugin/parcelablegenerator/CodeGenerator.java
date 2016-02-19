/*
 * Copyright (C) 2016 Nekocode (https://github.com/nekocode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.nekocode.plugin.parcelablegenerator;

import cn.nekocode.plugin.parcelablegenerator.typeserializers.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;

import java.util.ArrayList;
import java.util.List;

import static cn.nekocode.plugin.parcelablegenerator.typeserializers.TypeSerializerFactory.createTypeSerializers;


/**
 * Created by nekocode on 2015/12/1.
 */
public class CodeGenerator {
    private final KtClass mClass;
    private final List<ValueParameterDescriptor> mFields;

    public CodeGenerator(KtClass ktClass, List<ValueParameterDescriptor> fields) {
        mClass = ktClass;
        mFields = fields;
    }

    private String generateStaticCreator(KtClass ktClass) {
        String className = ktClass.getName();

        return "companion object { @JvmField final val CREATOR: Parcelable.Creator<" +
                className + "> = object : Parcelable.Creator<" + className + "> {" +
                "override fun createFromParcel(source: Parcel): " + className +
                "{return " + className + "(source)}" +
                "override fun newArray(size: Int): Array<" + className + "?> {" +
                "return arrayOfNulls(size)}" + "}}";
    }

    private String generateConstructor(List<TypeSerializer> typeSerializers) {
        StringBuilder sb = new StringBuilder("constructor(source: Parcel): this(");
        String content = "";
        for(TypeSerializer typeSerializer : typeSerializers) {
            content += typeSerializer.readValue() + ",";
        }
        if(content.length() > 0) {
            content = content.substring(0, content.length() - 1);
        }
        sb.append(content).append(")");

        return sb.toString();
    }

    private String generateDescribeContents() {
        return "override fun describeContents(): Int {return 0}";
    }

    private String generateWriteToParcel(List<TypeSerializer> typeSerializers) {
        StringBuilder sb = new StringBuilder("override fun writeToParcel(dest: Parcel?, flags: Int) {");
        for(TypeSerializer typeSerializer : typeSerializers) {
            sb.append(typeSerializer.writeValue()).append("\n");
        }
        sb.append("}");

        return sb.toString();
    }


    public void generate() {
        KtPsiFactory elementFactory = new KtPsiFactory(mClass.getProject());

        KtFile parent = mClass.getContainingKtFile();

        // Check if already imported Parcel and Parcelable
        boolean importedParcelable = false;
        boolean importedParcel = false;
        boolean importedJavaUtil = false;
        List<KtImportDirective> importList = parent.getImportDirectives();
        for(KtImportDirective importDirective : importList) {
            ImportPath importPath = importDirective.getImportPath();
            if(importPath != null) {
                String pathStr = importPath.getPathStr();
                if(pathStr.equals("android.os.Parcelable")) {
                    importedParcelable = true;
                }
                if(pathStr.equals("android.os.Parcel")) {
                    importedParcel = true;
                }
                if(pathStr.equals("java.util.*")) {
                    importedJavaUtil = true;
                }
            }
        }

        if(!importedParcelable) {
            parent.addAfter(elementFactory.createImportDirective(new ImportPath("android.os.Parcelable")), parent.getFirstChild());
        }
        if(!importedParcel) {
            parent.addAfter(elementFactory.createImportDirective(new ImportPath("android.os.Parcel")), parent.getFirstChild());
        }
        if(!importedJavaUtil) {
            parent.addAfter(elementFactory.createImportDirective(new ImportPath("java.util.*")), parent.getFirstChild());
        }


        // Save old declarations and clean Class Body
        List<KtDeclaration> oldDeclarations = new ArrayList<>();
        KtClassBody body = mClass.getBody();

        if(body != null) {
            List<KtDeclaration> declarations = body.getDeclarations();

            if(declarations.size() != 0) {
                for(KtDeclaration declaration: declarations) {
                    if(declaration instanceof KtSecondaryConstructor) {

                    } else if(declaration instanceof KtObjectDeclaration) {

                    } else if(declaration instanceof KtNamedFunction) {
                        String name = declaration.getName();
                        if(name != null && !name.equals("describeContents") && !name.equals("writeToParcel")) {
                            oldDeclarations.add(declaration);
                        }

                    } else {
                        oldDeclarations.add(declaration);
                    }
                }
            }

            body.delete();
        }

        // Add colon
        PsiElement colon = mClass.getColon();
        if(colon == null) {
            mClass.addAfter(elementFactory.createColon(), mClass.getLastChild());
        }

        // Check if already implement Parceable
        Boolean implementedParceable = false;
        List<KtSuperTypeListEntry> superTypeList = mClass.getSuperTypeListEntries();
        for(KtSuperTypeListEntry superTypeListEntry : superTypeList) {
            if(superTypeListEntry.getText().equals("Parcelable")) {
                implementedParceable = true;
            }
        }

        if(!implementedParceable) {
            // Implement Parceable
            if(superTypeList.size() > 0) {
                mClass.addAfter(elementFactory.createComma(), mClass.getLastChild());
            }

            mClass.addAfter(elementFactory.createIdentifier("Parcelable"), mClass.getLastChild());
            mClass.addAfter(elementFactory.createWhiteSpace(), mClass.getLastChild());
        }

        // Add old declarations
        String oldDeclarationsStr = "";
        for(KtDeclaration declaration: oldDeclarations) {
            oldDeclarationsStr += declaration.getText() + "\n\n";
        }

        List<TypeSerializer> typeSerializers = createTypeSerializers(mFields);
        String block = oldDeclarationsStr +
                generateConstructor(typeSerializers) + "\n\n" +
                generateDescribeContents() + "\n\n" +
                generateWriteToParcel(typeSerializers) + "\n\n" +
                generateStaticCreator(mClass);

        mClass.addAfter(elementFactory.createBlock(block), mClass.getLastChild());
    }
}
