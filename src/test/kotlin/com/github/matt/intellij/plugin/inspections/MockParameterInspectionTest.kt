package com.github.matt.intellij.plugin.inspections

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class MockParameterInspectionTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MockParameterInspection())

        addJunitTestAnnotation()
        addExtendWithAnnotation()
        addMockitoExtensionClass()
        addMockAnnotation()
    }

    // -------------------------------------------------------------------------
    // Stubs
    // -------------------------------------------------------------------------

    private fun addJunitTestAnnotation() {
        myFixture.addClass(
            """
            package org.junit.jupiter.api;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Test {}
            """.trimIndent()
        )
    }

    private fun addExtendWithAnnotation() {
        myFixture.addClass(
            """
            package org.junit.jupiter.api.extension;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface ExtendWith {
                Class[] value();
            }
            """.trimIndent()
        )
    }

    private fun addMockitoExtensionClass() {
        myFixture.addClass(
            """
            package org.mockito.junit.jupiter;
            public class MockitoExtension {}
            """.trimIndent()
        )
    }

    private fun addMockAnnotation() {
        myFixture.addClass(
            """
            package org.mockito;
            import java.lang.annotation.*;
            @Target({ElementType.FIELD, ElementType.PARAMETER})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Mock {}
            """.trimIndent()
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Collects only the WARNING-severity highlights produced by our inspection. */
    private fun ourWarnings(): List<String> =
        myFixture.doHighlighting(HighlightSeverity.WARNING)
            .map { it.description ?: "" }
            .filter { it.isNotEmpty() }

    // -------------------------------------------------------------------------
    // Missing @ExtendWith
    // -------------------------------------------------------------------------

    fun `test warns when test class with parameterised methods is missing ExtendWith`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            class MyTest {
                @Test
                void myTest(String service) {}
            }
            """.trimIndent()
        )
        val warnings = ourWarnings()
        assertTrue("Expected ExtendWith warning", warnings.any { "@ExtendWith" in it })
    }

    fun `test no warning when test method has no parameters and class lacks ExtendWith`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            class MyTest {
                @Test
                void myTest() {}
            }
            """.trimIndent()
        )
        assertTrue("Expected no warnings", ourWarnings().isEmpty())
    }

    // -------------------------------------------------------------------------
    // Missing @Mock on parameter
    // -------------------------------------------------------------------------

    fun `test warns when ExtendWith is present but parameter is missing Mock`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.junit.jupiter.MockitoExtension;
            @ExtendWith(MockitoExtension.class)
            class MyTest {
                @Test
                void myTest(String service) {}
            }
            """.trimIndent()
        )
        val warnings = ourWarnings()
        assertTrue("Expected @Mock warning", warnings.any { "service" in it && "@Mock" in it })
    }

    fun `test no warning when Mock annotation is already present`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.Mock;
            import org.mockito.junit.jupiter.MockitoExtension;
            @ExtendWith(MockitoExtension.class)
            class MyTest {
                @Test
                void myTest(@Mock String service) {}
            }
            """.trimIndent()
        )
        val warnings = ourWarnings()
        assertTrue("Expected no @Mock warnings", warnings.none { "@Mock" in it })
    }

    // -------------------------------------------------------------------------
    // Quick-fix: add @ExtendWith
    // -------------------------------------------------------------------------

    fun `test fix adds ExtendWith annotation with imports`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            class My<caret>Test {
                @Test
                void myTest(String service) {}
            }
            """.trimIndent()
        )
        val action = myFixture.findSingleIntention("Add @ExtendWith(MockitoExtension.class)")
        myFixture.launchAction(action)
        myFixture.checkResult(
            """
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.junit.jupiter.MockitoExtension;

            @ExtendWith(MockitoExtension.class)
            class MyTest {
                @Test
                void myTest(String service) {}
            }
            """.trimIndent()
        )
    }

    // -------------------------------------------------------------------------
    // Quick-fix: add @Mock
    // -------------------------------------------------------------------------

    fun `test fix adds Mock annotation with import`() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.junit.jupiter.MockitoExtension;
            @ExtendWith(MockitoExtension.class)
            class MyTest {
                @Test
                void myTest(String ser<caret>vice) {}
            }
            """.trimIndent()
        )
        val action = myFixture.findSingleIntention("Annotate parameter 'service' as '@Mock'")
        myFixture.launchAction(action)
        myFixture.checkResult(
            """
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.Mock;
            import org.mockito.junit.jupiter.MockitoExtension;
            @ExtendWith(MockitoExtension.class)
            class MyTest {
                @Test
                void myTest(@Mock String service) {}
            }
            """.trimIndent()
        )
    }
}
