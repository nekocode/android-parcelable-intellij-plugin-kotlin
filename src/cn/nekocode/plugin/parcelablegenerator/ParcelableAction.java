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

package cn.nekocode.plugin.parcelablegenerator;

import cn.nekocode.plugin.parcelablegenerator.utils.KtClassHelper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.List;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.*;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class ParcelableAction extends AnAction {
    private KtClass ktClass;

    @Override
    public void update(AnActionEvent e) {
        ktClass = getPsiClassFromEvent(e);

        e.getPresentation().setEnabled(
                ktClass != null &&
                        !ktClass.isEnum() &&
                        !ktClass.isInterface()
        );
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
//        if(!ktClass.isData()) {
//            Messages.showErrorDialog("ParcelableGenerator only support for data class.", "Sorry");
//
//        } else {
//            GenerateDialog dlg = new GenerateDialog(ktClass);
//            dlg.show();
//            if (dlg.isOK()) {
//                generateParcelable(ktClass, dlg.getSelectedFields());
//            }

        generateParcelable(ktClass, KtClassHelper.findParams(ktClass));
//        }
    }

    private void generateParcelable(final KtClass ktClass, final List<ValueParameterDescriptor> fields) {
        new WriteCommandAction.Simple(ktClass.getProject(), ktClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new CodeGenerator(ktClass, fields).generate();
            }
        }.execute();
    }

    private KtClass getPsiClassFromEvent(AnActionEvent e) {
        final Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) return null;

        final Project project = editor.getProject();
        if (project == null) return null;

        final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null || psiFile.getLanguage() != KotlinLanguage.INSTANCE) return null;

        final Location location = Location.fromEditor(editor, project);
        final PsiElement psiElement = psiFile.findElementAt(location.getStartOffset());
        if (psiElement == null) return null;

        return KtClassHelper.getKtClassForElement(psiElement);
    }
}
