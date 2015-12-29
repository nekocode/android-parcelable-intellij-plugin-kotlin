/*
 * Copyright (C) 2013 Michał Charmas (http://blog.charmas.pl)
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

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;

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

        StringBuilder sb = new StringBuilder("companion object { @JvmField final val CREATOR: Parcelable.Creator<");

        sb.append(className).append("> = object : Parcelable.Creator<").append(className).append("> {")
                .append("override fun createFromParcel(source: Parcel): ").append(className).append("{return ")
                .append(className).append("(source)}")
                .append("override fun newArray(size: Int): Array<").append(className).append("?> {")
                .append("return arrayOfNulls(size)}")
                .append("}}");
        return sb.toString();
    }

    private String generateConstructor(List<ValueParameterDescriptor> fields) {
        StringBuilder sb = new StringBuilder("constructor(source: Parcel): this(");

        String content = "";
        for (ValueParameterDescriptor field : fields) {
            String type = field.getType().toString();

            if (type.equals("Boolean")) {
                content += "1.toByte().equals(source.readByte()),";
            } else {
                content += "source.read" + type + "(),";
            }
        }

        content = content.substring(0, content.length() - 1);

        sb.append(content).append(")");
        return sb.toString();
    }

    private String generateDescribeContents() {
        return "override fun describeContents(): Int {return 0}";
    }

    private String generateWriteToParcel(List<ValueParameterDescriptor> fields) {
        StringBuilder sb = new StringBuilder("override fun writeToParcel(dest: Parcel?, flags: Int) {");

        for (ValueParameterDescriptor field : fields) {
            String type = field.getType().toString();
            String name = field.getName().toString();

            if (type.equals("Boolean")) {
                sb.append("dest?.writeByte((if(").append(name).append(") 1 else 0).toByte()) \n");
            } else {
                sb.append("dest?.write").append(type).append("(this.").append(name).append(") \n");
            }
        }

        sb.append("}");

        return sb.toString();
    }


    public void generate() {
        KtPsiFactory elementFactory = new KtPsiFactory(mClass.getProject());

        mClass.addBefore(elementFactory.createImportDirective(new ImportPath("android.os.Parcelable")), mClass.getFirstChild());
        mClass.addBefore(elementFactory.createImportDirective(new ImportPath("android.os.Parcel")), mClass.getFirstChild());

        mClass.addAfter(elementFactory.createColon(), mClass.getLastChild());
        mClass.addAfter(elementFactory.createIdentifier("Parcelable"), mClass.getLastChild());
        mClass.addAfter(elementFactory.createWhiteSpace(), mClass.getLastChild());


        String block = generateConstructor(mFields) + "\n\n" +
                generateDescribeContents() + "\n\n" +
                generateWriteToParcel(mFields) + "\n\n" +
                generateStaticCreator(mClass);

        mClass.addAfter(elementFactory.createBlock(block), mClass.getLastChild());

    }

}
