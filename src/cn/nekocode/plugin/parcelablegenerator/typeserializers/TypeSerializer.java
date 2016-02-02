package cn.nekocode.plugin.parcelablegenerator.typeserializers;

import com.intellij.psi.PsiField;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;

/**
 * Created by nekocode on 2016/2/2.
 */
public abstract class TypeSerializer {
    protected final ValueParameterDescriptor field;

    public TypeSerializer(ValueParameterDescriptor field) {
        this.field = field;
    }

    abstract public String readValue();

    abstract public String writeValue();
}
