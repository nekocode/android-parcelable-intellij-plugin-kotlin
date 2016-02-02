package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public class CharSerializer extends TypeSerializer {

    public CharSerializer(ValueParameterDescriptor field) {
        super(field);
    }

    public String readValue() {
        return "source.readByte().toChar()";
    }

    public String writeValue() {
        return "dest?.writeByte(" + field.getName() + ".toByte())";
    }
}
