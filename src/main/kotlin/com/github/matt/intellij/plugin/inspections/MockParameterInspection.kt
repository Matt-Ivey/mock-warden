package com.github.matt.intellij.plugin.inspections

import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

private const val EXTEND_WITH_FQN = "org.junit.jupiter.api.extension.ExtendWith"
private const val MOCKITO_EXT_FQN = "org.mockito.junit.jupiter.MockitoExtension"
private const val MOCK_FQN        = "org.mockito.Mock"
private val TEST_ANNOTATIONS = setOf(
    "org.junit.Test",
    "org.junit.jupiter.api.Test"
)

class MockParameterInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                if (!isTestMethod(method)) return
                if (method.parameterList.isEmpty) return

                val containingClass = method.containingClass ?: return

                if (!hasMockitoExtension(containingClass)) {
                    val anchor = containingClass.nameIdentifier ?: return
                    holder.registerProblem(
                        anchor,
                        "Test class uses @Test method parameters but is missing " +
                            "@ExtendWith(MockitoExtension.class)",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        AddMockitoExtensionFix()
                    )
                    return
                }

                for (param in method.parameterList.parameters) {
                    if (param.modifierList?.findAnnotation(MOCK_FQN) == null) {
                        holder.registerProblem(
                            param.nameIdentifier ?: param,
                            "Parameter '${param.name}' in @Test method is missing @Mock",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            AddAnnotationFix(MOCK_FQN, param)
                        )
                    }
                }
            }
        }

    private fun isTestMethod(method: PsiMethod) =
        TEST_ANNOTATIONS.any { method.modifierList.findAnnotation(it) != null }

    private fun hasMockitoExtension(cls: PsiClass): Boolean {
        var current: PsiClass? = cls
        while (current != null) {
            val extendWith = current.modifierList?.findAnnotation(EXTEND_WITH_FQN)
            if (extendWith != null && referencesClass(extendWith, MOCKITO_EXT_FQN)) return true
            current = current.superClass
        }
        return false
    }

    private fun referencesClass(annotation: PsiAnnotation, fqn: String): Boolean {
        val value = annotation.findAttributeValue("value") ?: return false
        val items = if (value is PsiArrayInitializerMemberValue) value.initializers.toList()
                    else listOf(value)
        return items.any { it is PsiClassObjectAccessExpression &&
            it.operand.type.canonicalText == fqn }
    }
}
