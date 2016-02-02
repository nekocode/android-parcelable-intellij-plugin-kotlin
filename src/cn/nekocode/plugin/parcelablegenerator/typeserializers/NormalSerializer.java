package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public class NormalSerializer extends TypeSerializer {

    public NormalSerializer(ValueParameterDescriptor field) {
        super(field);
    }

    public String readValue() {
        return "source.read" + field.getType() + "()";
    }

    public String writeValue() {
        return "dest?.write" + field.getType() + "(" + field.getName() + ")";
    }
}
