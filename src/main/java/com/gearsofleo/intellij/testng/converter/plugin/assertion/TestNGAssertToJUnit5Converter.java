package com.gearsofleo.intellij.testng.converter.plugin.assertion;

import com.gearsofleo.intellij.testng.converter.plugin.TestNGAssertClass;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.junit.JUnitCommonClassNames;

public class TestNGAssertToJUnit5Converter {

	public static void replaceAssertion(Project project, PsiElement element) {
		if (element instanceof PsiMethodReferenceExpression) {
			TestNGAssertHint testNGAssertHint = TestNGAssertHint.create((PsiMethodReferenceExpression) element);
			if (testNGAssertHint != null) {
				replaceQualifier(project, testNGAssertHint.getMethod().getName(), (PsiReferenceExpression) element);
			}
			return;
		}

		final PsiMethodCallExpression methodCallExpression = PsiTreeUtil
				.getParentOfType(element, PsiMethodCallExpression.class);
		if (methodCallExpression == null) {
			return;
		}

		TestNGAssertHint testNGAssertHint = TestNGAssertHint.create(methodCallExpression);
		if (testNGAssertHint == null) {
			return;
		}

		PsiClass assertClass = testNGAssertHint.getMethod().getContainingClass();
		String methodName = testNGAssertHint.getMethod().getName();
		if (assertClass != null
				&& TestNGAssertClass.ASSERT.classFqn().equals(assertClass.getQualifiedName())
				&& testNGAssertHint.hasExpectedAndActualArg()) {
			PsiExpression expected = testNGAssertHint.getExpected();
			PsiExpression actual = testNGAssertHint.getActual();
			if (expected != null && actual != null) {
				//add expected and actual arguments in correct order
				PsiElement addedActual = methodCallExpression.getArgumentList().addBefore(actual.copy(), actual);
				methodCallExpression.getArgumentList().addBefore(expected.copy(), addedActual);
				//remove original expected and actual arguments
				actual.delete();
				expected.delete();
			}
		}
		//i.e. if org.testng.AssertJUnit is used
		PsiExpression message = testNGAssertHint.getMessage();
		if (message != null && testNGAssertHint.isMessageOnFirstPosition()) {
			methodCallExpression.getArgumentList().add(message);
			message.delete();
		}
		replaceQualifier(project, methodName, methodCallExpression.getMethodExpression());
	}

	private static void replaceQualifier(Project project,
			String methodName,
			final PsiReferenceExpression methodExpression) {
		PsiClass newAssertClass = JavaPsiFacade.getInstance(project).findClass(getNewAssertClassName(methodName),
				methodExpression.getResolveScope());

		if (newAssertClass == null) {
			return;
		}
		String qualifiedName = newAssertClass.getQualifiedName();
		if (qualifiedName == null) {
			return;
		}

		methodExpression.setQualifierExpression(JavaPsiFacade.getElementFactory(project)
				.createReferenceExpression(newAssertClass));
		JavaCodeStyleManager.getInstance(project).shortenClassReferences(methodExpression);
	}

	public static String getNewAssertClassName(String methodName) {
		if ("assertThat".equals(methodName)) {
			return JUnitCommonClassNames.ORG_HAMCREST_MATCHER_ASSERT;
		}
		else if (methodName.startsWith("assume")) {
			return JUnitCommonClassNames.ORG_JUNIT_JUPITER_API_ASSUMPTIONS;
		}
		else {
			return JUnitCommonClassNames.ORG_JUNIT_JUPITER_API_ASSERTIONS;
		}
	}
}
