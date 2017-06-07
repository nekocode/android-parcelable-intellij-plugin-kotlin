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

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public abstract class TypeSerializer {
    private final ValueParameterDescriptor field;
    private final String fieldName;
    private final String type;
    private final String noneNullType;
    private final boolean isTypeNullable;
    private final String projectionType;
    private final boolean isProjectionTypeNullable;
    private final String noneNullProjectionType;

    public TypeSerializer(ValueParameterDescriptor field) {
        this.field = field;
        fieldName = field.getName().toString();
        type = field.getType().toString();
        isTypeNullable = type.endsWith("?");
        noneNullType = isTypeNullable ? type.substring(0, type.length() - 1) : type;
        if (field.getType().getArguments().size() > 0) {
            projectionType = field.getType().getArguments().get(0).getType().toString();
            isProjectionTypeNullable  = projectionType.endsWith("?");
            noneNullProjectionType = isProjectionTypeNullable ?
                    projectionType.substring(0, projectionType.length() - 1) : projectionType;

        } else {
            projectionType = null;
            isProjectionTypeNullable = true;
            noneNullProjectionType = null;
        }
    }

    abstract public String generateReadValue();

    abstract public String generateWriteValue();

    public ValueParameterDescriptor getField() {
        return field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getType() {
        return type;
    }

    public String getNoneNullType() {
        return noneNullType;
    }

    public boolean isTypeNullable() {
        return isTypeNullable;
    }

    public String getProjectionType() {
        return projectionType;
    }

    public boolean isProjectionTypeNullable() {
        return isProjectionTypeNullable;
    }

    public String getNoneNullProjectionType() {
        return noneNullProjectionType;
    }
}
