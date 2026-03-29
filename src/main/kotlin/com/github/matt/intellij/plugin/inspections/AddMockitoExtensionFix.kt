package com.github.matt.intellij.plugin.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class AddMockitoExtensionFix : LocalQuickFix {

    override fun getFamilyName() = "Add @ExtendWith(MockitoExtension.class)"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        // descriptor.psiElement is the class nameIdentifier; parent is the PsiClass
        val cls = descriptor.psiElement.parent as? PsiClass ?: return
        val modifierList = cls.modifierList ?: return

        val factory = JavaPsiFacade.getElementFactory(project)
        val annotation = factory.createAnnotationFromText(
            "@org.junit.jupiter.api.extension.ExtendWith(" +
                "org.mockito.junit.jupiter.MockitoExtension.class)",
            cls
        )
        modifierList.addBefore(annotation, modifierList.firstChild)
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(cls)
    }
}
