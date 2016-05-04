package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by nekocode on 2016/2/16.
 */
public class TypeSerializerFactory {

    public static List<TypeSerializer> createTypeSerializers(java.util.List<ValueParameterDescriptor> fields) {
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

                case "List<String>":
                case "ArrayList<String>":
                case "MutableList<String>":
                    typeSerializers.add(new StringListSerializer(field));
                    break;

                case "Array<String>":
                case "ByteArray":
                case "DoubleArray":
                case "FloatArray":
                case "IntArray":
                case "LongArray":
                case "CharArray":
                case "BooleanArray":
                    typeSerializers.add(new OriginalArraySerializer(field));
                    break;

                case "Array<Byte>":
                case "Array<Double>":
                case "Array<Float>":
                case "Array<Int>":
                case "Array<Long>":
                case "Array<Char>":
                case "Array<Boolean>":
                    typeSerializers.add(new NormalArraySerializer(field));
                    break;

                default:
                    Collection<KotlinType> supertypes;

                    // Check if type is List or Array
                    if(typeName.startsWith("List") || typeName.startsWith("ArrayList") || typeName.startsWith("MutableList")) {
                        KotlinType typeProjectionType = type.getArguments().get(0).getType();

                        Boolean isParcelable = false;
                        supertypes = typeProjectionType.getConstructor().getSupertypes();
                        for(KotlinType supertype : supertypes) {
                            String supertypeName = supertype.toString();
                            if(supertypeName.equals("Parcelable")) {
                                typeSerializers.add(new ParcelableListSerializer(field));
                                isParcelable = true;
                                break;
                            }
                        }

                        if(!isParcelable) {
                            typeSerializers.add(new NormalListSerializer(field));
                        }


                    } else if(typeName.startsWith("Array")) {
                        KotlinType typeProjectionType = type.getArguments().get(0).getType();

                        boolean found = false;
                        supertypes = typeProjectionType.getConstructor().getSupertypes();
                        for(KotlinType supertype : supertypes) {
                            String supertypeName = supertype.toString();
                            if(supertypeName.equals("Parcelable")) {
                                typeSerializers.add(new ParcelableArraySerializer(field));
                                found = true;
                                break;
                            }
                        }

                        // Not found
                        if(!found) {
                            typeSerializers.add(new NormalSerializer(field));
                        }


                    } else {
                        // Check if supertype is Parcelable or Serializable
                        boolean found = false;
                        supertypes = type.getConstructor().getSupertypes();
                        for(KotlinType supertype : supertypes) {
                            String supertypeName = supertype.toString();
                            if(supertypeName.equals("Parcelable")) {
                                typeSerializers.add(new ParcelableObjectSerializer(field));
                                found = true;
                                break;

                            } else if(supertypeName.equals("Serializable")) {
                                typeSerializers.add(new SerializableObjectSerializer(field));
                                found = true;
                                break;
                            }
                        }

                        // Not found
                        if(!found) {
                            typeSerializers.add(new NormalSerializer(field));
                        }
                    }
            }
        }
        return typeSerializers;
    }
}
