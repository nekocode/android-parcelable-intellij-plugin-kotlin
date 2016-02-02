package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public class SerializableObjectSerializer extends TypeSerializer {

    public SerializableObjectSerializer(ValueParameterDescriptor field) {
        super(field);
    }

    public String readValue() {
        return "source.readSerializable() as " + field.getType();
    }

    public String writeValue() {
        return "dest?.writeSerializable(" + field.getName() + ")";
    }
}
