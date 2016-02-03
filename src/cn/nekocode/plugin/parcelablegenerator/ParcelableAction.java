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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.KtLightElement;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;

/**
 * Created by nekocode on 2015/12/1.
 */
public class ParcelableAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        KtClass ktClass = getPsiClassFromEvent(e);

        if(ktClass != null) {
            String text = ktClass.getText();

            if(!text.startsWith("data")) {
                Messages.showErrorDialog("ParcelableGenerator only support for data class.", "Sorry");

            } else {
//                GenerateDialog dlg = new GenerateDialog(ktClass);
//                dlg.show();
//                if (dlg.isOK()) {
                    generateParcelable(ktClass, findParams(ktClass));
//                }
            }
        }
    }

    private List<ValueParameterDescriptor> findParams(PsiElement element) {
        List<KtElement> list = new ArrayList<>();
        list.add((KtElement) element);

        ResolveSession resolveSession = KotlinCacheService.getInstance(element.getProject()).
                getResolutionFacade(list).getFrontendService(ResolveSession.class);
        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor((KtClassOrObject) element, NoLookupLocation.FROM_IDE);

        List<ValueParameterDescriptor> valueParameters = new ArrayList<>();
        if (classDescriptor.isData()) {
            ConstructorDescriptor constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();

            if (constructorDescriptor != null) {
                List<ValueParameterDescriptor> allParameters = constructorDescriptor.getValueParameters();

                allParameters.stream().forEach(valueParameters::add);
            }
        }

        return valueParameters;
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
        if (psiFile == null || !(psiFile instanceof KtFile) || !ProjectRootsUtil.isInProjectSource(psiFile))
            return null;

        Location location = Location.fromEditor(editor, project);
        PsiElement psiElement = psiFile.findElementAt(location.getStartOffset());
        if (psiElement == null) return null;

        return getKtClass(psiElement);
    }

    private KtClass getKtClass(@NotNull PsiElement psiElement) {
        if (psiElement instanceof KtLightElement) {
            PsiElement origin = ((KtLightElement) psiElement).getOrigin();
            if (origin != null) {
                return getKtClass(origin);
            } else {
                return null;
            }

        } else if (psiElement instanceof KtClass && !((KtClass) psiElement).isEnum() &&
                !((KtClass) psiElement).isInterface() &&
                !((KtClass) psiElement).isAnnotation() &&
                !((KtClass) psiElement).isSealed()) {
            return (KtClass) psiElement;

        } else {
            PsiElement parent = psiElement.getParent();
            if (parent == null) {
                return null;
            } else {
                return getKtClass(parent);
            }
        }
    }
}
