package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public class ParcelableObjectSerializer extends TypeSerializer {

    public ParcelableObjectSerializer(ValueParameterDescriptor field) {
        super(field);
    }

    public String readValue() {
        return "source.readParcelable<" + field.getType() + ">(" + field.getType() + "::class.java.classLoader)";
    }

    public String writeValue() {
        return "dest?.writeParcelable(" + field.getName() + ", 0)";
    }
}
