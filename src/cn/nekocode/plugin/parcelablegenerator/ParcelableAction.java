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
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;

import java.util.List;

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.*;

/**
 * Created by nekocode on 2015/12/1.
 */
public class ParcelableAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        KtClass ktClass = getPsiClassFromEvent(e);

        if(ktClass != null) {
//            if(!ktClass.isData()) {
//                Messages.showErrorDialog("ParcelableGenerator only support for data class.", "Sorry");
//
//            } else {
//                GenerateDialog dlg = new GenerateDialog(ktClass);
//                dlg.show();
//                if (dlg.isOK()) {
//                    generateParcelable(ktClass, dlg.getSelectedFields());
//                }

                generateParcelable(ktClass, KtClassHelper.findParams(ktClass));
//            }
        }
    }

    private void generateParcelable(final KtClass ktClass, final List<ValueParameterDescriptor> fields) {
        new WriteCommandAction.Simple(ktClass.getProject(), ktClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new CodeGenerator(ktClass, fields).generate();
            }
        }.execute();
    }

    @Override
    public void update(AnActionEvent e) {
        KtClass ktClass = getPsiClassFromEvent(e);

        e.getPresentation().setEnabled(ktClass != null && !ktClass.isEnum() && !ktClass.isInterface());
    }

    private KtClass getPsiClassFromEvent(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        assert editor != null;

        Project project = editor.getProject();
        if (project == null) return null;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null || !(psiFile instanceof KtFile) || ProjectRootsUtil.isOutsideSourceRoot(psiFile))
            return null;

        Location location = Location.fromEditor(editor, project);
        PsiElement psiElement = psiFile.findElementAt(location.getStartOffset());
        if (psiElement == null) return null;

        return KtClassHelper.getKtClassForElement(psiElement);
    }
}
