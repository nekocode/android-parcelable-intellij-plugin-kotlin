package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public class BooleanSerializer extends TypeSerializer {

    public BooleanSerializer(ValueParameterDescriptor field) {
        super(field);
    }

    public String readValue() {
        return "1.toByte().equals(source.readByte())";
    }

    public String writeValue() {
        return "dest?.writeByte((if(" + field.getName() + ") 1 else 0).toByte())";
    }
}
