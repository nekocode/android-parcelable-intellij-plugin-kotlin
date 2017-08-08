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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.util.ImportInsertHelper;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class CodeGenerator {
    private final KtClass mClass;
    private final List<ValueParameterDescriptor> mFields;

    public CodeGenerator(KtClass ktClass, List<ValueParameterDescriptor> fields) {
        mClass = ktClass;
        mFields = fields;
    }

    private String generateStaticCreator(KtClass ktClass, KtClassBody oldBodyOfCompanion) {
        String className = ktClass.getName();

        StringBuilder oldBodyText = new StringBuilder();
        if (oldBodyOfCompanion != null) {
            List<KtDeclaration> declarations = oldBodyOfCompanion.getDeclarations();
            for (KtDeclaration declaration : declarations) {
                String declarationName = declaration.getName();
                if (declarationName != null && !declarationName.equals("CREATOR")) {
                    oldBodyText.append(declaration.getText()).append("\n\n");
                }
            }
        }

        return "companion object { " + oldBodyText + "@JvmField val CREATOR: Parcelable.Creator<" +
                className + "> = object : Parcelable.Creator<" + className + "> {" +
                "override fun createFromParcel(source: Parcel): " + className +
                " = " + className + "(source)" +
                "\noverride fun newArray(size: Int): Array<" + className + "?> =" +
                "arrayOfNulls(size)" + "}}";
    }

    private String generateConstructor(List<TypeSerializer> typeSerializers) {
        StringBuilder sb = new StringBuilder("constructor(source: Parcel) : this(");
        String content = "";
        for (TypeSerializer typeSerializer : typeSerializers) {
            content += "\n" + typeSerializer.generateReadValue() + ",";
        }
        if (content.length() > 0) {
            content = content.substring(0, content.length() - 1);
        }
        sb.append(content).append("\n)");

        return sb.toString();
    }

    private String generateDescribeContents() {
        return "override fun describeContents() = 0";
    }

    private String generateWriteToParcel(List<TypeSerializer> typeSerializers) {
        StringBuilder sb = new StringBuilder("override fun writeToParcel(dest: Parcel, flags: Int) = with (dest) {");
        for (TypeSerializer typeSerializer : typeSerializers) {
            sb.append(typeSerializer.generateWriteValue()).append("\n");
        }
        sb.append("}");

        return sb.toString();
    }

    private void insertImport(KtFile ktFile, String fqName) {
        final Collection<DeclarationDescriptor> descriptors =
                ResolutionUtils.resolveImportReference(ktFile, new FqName(fqName));

        if (!descriptors.isEmpty()) {
            ImportInsertHelper.getInstance(ktFile.getProject())
                    .importDescriptor(ktFile, descriptors.iterator().next(), false);
        }
    }

    private void insertImports(KtFile ktFile) {
        // Check if already imported Parcel and Parcelable
        boolean importedParcelable = false;
        boolean importedParcel = false;
        List<KtImportDirective> importList = ktFile.getImportDirectives();
        for (KtImportDirective importDirective : importList) {
            ImportPath importPath = importDirective.getImportPath();
            if (importPath != null) {
                String pathStr = importPath.getPathStr();
                if (pathStr.equals("android.os.Parcelable")) {
                    importedParcelable = true;
                }
                if (pathStr.equals("android.os.Parcel")) {
                    importedParcel = true;
                }
            }
        }

        if (!importedParcelable) {
            insertImport(ktFile, "android.os.Parcelable");
        }
        if (!importedParcel) {
            insertImport(ktFile, "android.os.Parcel");
        }
    }


    public void generate() {
        KtPsiFactory elementFactory = new KtPsiFactory(mClass.getProject());

        // Insert imports
        insertImports(mClass.getContainingKtFile());

        // Save old declarations and clean Class Body
        List<KtDeclaration> oldDeclarations = new ArrayList<KtDeclaration>();
        KtClassBody oldBodyOfCompanion = null;

        KtClassBody body = mClass.getBody();
        if(body != null) {
            List<KtDeclaration> declarations = body.getDeclarations();

            if(declarations.size() != 0) {
                for(KtDeclaration declaration: declarations) {
                    if(declaration instanceof KtSecondaryConstructor) {
                        KtSecondaryConstructor constructor = (KtSecondaryConstructor) declaration;

                        PsiElement[] valueParameters = constructor.getChildren()[0].getChildren();
                        if (valueParameters.length == 1) {
                            KtTypeReference typeReference = ((KtParameter) valueParameters[0]).getTypeReference();
                            if (typeReference != null && typeReference.getText().equals("Parcel")) {
                                // Skip if is the Parcel constructor
                                continue;
                            }
                        }

                        oldDeclarations.add(declaration);

                    } else if(declaration instanceof KtObjectDeclaration) {
                        KtObjectDeclaration objectDeclaration = (KtObjectDeclaration) declaration;
                        if (objectDeclaration.isCompanion()) {
                            oldBodyOfCompanion = objectDeclaration.getBody();

                        } else {
                            oldDeclarations.add(declaration);
                        }

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
        StringBuilder oldDeclarationsStr = new StringBuilder();
        for(KtDeclaration declaration: oldDeclarations) {
            oldDeclarationsStr.append(declaration.getText()).append("\n\n");
        }

        List<TypeSerializer> typeSerializers = TypeSerializerFactory.createTypeSerializers(mFields);
        String block = oldDeclarationsStr +
                generateConstructor(typeSerializers) + "\n\n" +
                generateDescribeContents() + "\n\n" +
                generateWriteToParcel(typeSerializers) + "\n\n" +
                generateStaticCreator(mClass, oldBodyOfCompanion);

        mClass.addAfter(elementFactory.createBlock(block), mClass.getLastChild());
    }
}
