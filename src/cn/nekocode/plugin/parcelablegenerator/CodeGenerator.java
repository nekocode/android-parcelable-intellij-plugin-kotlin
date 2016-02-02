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
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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
        sb.append(content.substring(0, content.length() - 1)).append(")");

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
            }
        }

        if(!importedParcelable) {
            parent.addAfter(elementFactory.createImportDirective(new ImportPath("android.os.Parcelable")), parent.getFirstChild());
        }
        if(!importedParcel) {
            parent.addAfter(elementFactory.createImportDirective(new ImportPath("android.os.Parcel")), parent.getFirstChild());
        }


        // Clean Class Body
        KtClassBody body = mClass.getBody();
        if(body != null) {
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


        List<TypeSerializer> typeSerializers = getTypeSerializers(mFields);
        String block = generateConstructor(typeSerializers) + "\n\n" +
                generateDescribeContents() + "\n\n" +
                generateWriteToParcel(typeSerializers) + "\n\n" +
                generateStaticCreator(mClass);

        mClass.addAfter(elementFactory.createBlock(block), mClass.getLastChild());

    }

    private List<TypeSerializer> getTypeSerializers(List<ValueParameterDescriptor> fields) {
        List<TypeSerializer> typeSerializers = new ArrayList<>();
        for(ValueParameterDescriptor field : fields) {
            KotlinType type = field.getType();
            String typeName = type.toString();

            switch (typeName) {
                case "String":
                case "Byte":
                case "Double":
                case "Float":
                case "Int":
                case "Long":
                    typeSerializers.add(new NormalSerializer(field));
                    break;

                case "Boolean":
                    typeSerializers.add(new BooleanSerializer(field));
                    break;

                case "Char":
                    typeSerializers.add(new CharSerializer(field));
                    break;

                default:
                    Collection<KotlinType> supertypes = type.getConstructor().getSupertypes();
                    for(KotlinType supertype : supertypes) {
                        String supertypeName = supertype.toString();
                        if(supertypeName.equals("Parcelable")) {
                            typeSerializers.add(new ParcelableObjectSerializer(field));
                            break;

                        } else if(supertypeName.equals("Serializable")) {
                            typeSerializers.add(new SerializableObjectSerializer(field));
                            break;
                        }
                    }
            }
        }
        return typeSerializers;
    }
}
