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

package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class TypeSerializerFactory {

    public static List<TypeSerializer> createTypeSerializers(java.util.List<ValueParameterDescriptor> fields) {
        List<TypeSerializer> typeSerializers = new ArrayList<TypeSerializer>();
        for(ValueParameterDescriptor field : fields) {
            KotlinType type = field.getType();
            String typeName = type.toString();
            boolean isNullable = type.isMarkedNullable();
            typeName = isNullable ? typeName.substring(0, typeName.length() - 1) : typeName;

            if (typeName.equals("Byte") || typeName.equals("Double") ||
                    typeName.equals("Float") || typeName.equals("Int") || typeName.equals("Long")) {
                typeSerializers.add(isNullable ?
                        new NullableValueSerializer(field) : new NormalSerializer(field));

            } else if (typeName.equals("String")) {
                typeSerializers.add(isNullable ?
                        new NullableStringSerializer(field) : new NormalSerializer(field));

            } else if (typeName.equals("Boolean")) {
                typeSerializers.add(isNullable ?
                        new NullableValueSerializer(field) : new BooleanSerializer(field));

            } else if (typeName.equals("Char")) {
                typeSerializers.add(isNullable ?
                        new NullableValueSerializer(field) : new CharSerializer(field));

            } else if (typeName.equals("List<String>") || typeName.equals("ArrayList<String>") ||
                    typeName.equals("MutableList<String>")) {
                typeSerializers.add(new StringListSerializer(field));

            } else if (typeName.equals("Array<String>") || typeName.equals("Array<String?>") ||
                    typeName.equals("ByteArray") || typeName.equals("DoubleArray") || typeName.equals("FloatArray") ||
                    typeName.equals("IntArray") || typeName.equals("LongArray") || typeName.equals("CharArray") ||
                    typeName.equals("BooleanArray")) {
                typeSerializers.add(new OriginalArraySerializer(field));

            } else if (typeName.equals("Array<Byte>") || typeName.equals("Array<Double>") || typeName.equals("Array<Float>") ||
                    typeName.equals("Array<Int>") || typeName.equals("Array<Long>") || typeName.equals("Array<Char>") ||
                    typeName.equals("Array<Boolean>")) {
                typeSerializers.add(new NormalArraySerializer(field));

            } else if (typeName.equals("Array<Byte?>") || typeName.equals("Array<Double?>") || typeName.equals("Array<Float?>") ||
                    typeName.equals("Array<Int?>") || typeName.equals("Array<Long?>") || typeName.equals("Array<Char?>") ||
                    typeName.equals("Array<Boolean?>")) {
                typeSerializers.add(new NullableArraySerializer(field));

            } else {
                Collection<KotlinType> supertypes;

                // Check whether type is List or Array
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
                    // Check whether the type inherits from some known types
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
                        } else if (supertypeName.equals("Enum<" + typeName + ">")) {
                            typeSerializers.add(new EnumSerializer(field));
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
