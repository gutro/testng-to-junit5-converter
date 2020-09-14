package com.gearsofleo.intellij.testng.converter.plugin.assertion;

import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifier;

import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.ACTUAL_BEFORE_EXPECTED;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.EXPECTED_BEFORE_ACTUAL;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.NONE;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.MessageParamOrder.MESSAGE_FIRST;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.MessageParamOrder.MESSAGE_LAST;

public class TestNGAssertHint {

	private final TestNGAssert assertMethod;
	private final PsiMethod psiMethod;
	private final PsiExpression messageParam;
	private final PsiExpression expectedParam;
	private final PsiExpression actualParam;

	TestNGAssertHint(TestNGAssert assertMethod, PsiMethod psiMethod,
			PsiExpression messageParam, PsiExpression expectedParam, PsiExpression actualParam) {
		this.assertMethod = assertMethod;
		this.psiMethod = psiMethod;
		this.messageParam = messageParam;
		this.expectedParam = expectedParam;
		this.actualParam = actualParam;
	}

	PsiMethod getMethod() {
		return psiMethod;
	}

	public boolean hasExpectedAndActualArg() {
		return assertMethod.getExpectedActualParamOrder() != NONE;
	}

	public boolean isMessageOnFirstPosition() {
		return assertMethod.getMessageParamOrder() == MESSAGE_FIRST;
	}

	public PsiExpression getExpected() {
		return expectedParam;
	}

	public PsiExpression getActual() {
		return actualParam;
	}

	PsiExpression getMessage() {
		return messageParam;
	}

	static TestNGAssertHint create(PsiMethodCallExpression callExpression) {

		final JavaResolveResult resolveResult = callExpression.resolveMethodGenerics();
		final PsiMethod method = (PsiMethod) resolveResult.getElement();
		if (method == null || method.hasModifierProperty(PsiModifier.PRIVATE) || !resolveResult.isValidResult()) {
			return null;
		}
		TestNGAssert assertMethod = TestNGAssert.find(method);
		if (assertMethod == null) {
			return null;
		}

		PsiExpression messageParam = assertMethod.getMessage(callExpression);
		PsiExpression expectedParam = null;
		PsiExpression actualParam = null;
		if (messageParam == null || assertMethod.getMessageParamOrder() == MESSAGE_LAST) {
			if (assertMethod.getExpectedActualParamOrder() == EXPECTED_BEFORE_ACTUAL) {
				expectedParam = callExpression.getArgumentList().getExpressions()[0];
				actualParam = callExpression.getArgumentList().getExpressions()[1];
			}
			else if (assertMethod.getExpectedActualParamOrder() == ACTUAL_BEFORE_EXPECTED) {
				expectedParam = callExpression.getArgumentList().getExpressions()[1];
				actualParam = callExpression.getArgumentList().getExpressions()[0];
			}
		}
		else {
			//message available and first
			if (assertMethod.getExpectedActualParamOrder() == EXPECTED_BEFORE_ACTUAL) {
				expectedParam = callExpression.getArgumentList().getExpressions()[1];
				actualParam = callExpression.getArgumentList().getExpressions()[2];
			}
			else if (assertMethod.getExpectedActualParamOrder() == ACTUAL_BEFORE_EXPECTED) {
				expectedParam = callExpression.getArgumentList().getExpressions()[2];
				actualParam = callExpression.getArgumentList().getExpressions()[1];
			}
		}
		return new TestNGAssertHint(assertMethod, method, messageParam, expectedParam, actualParam);
	}

	static TestNGAssertHint create(PsiMethodReferenceExpression methodExpression) {
		final JavaResolveResult resolveResult = methodExpression.advancedResolve(false);
		final PsiElement element = resolveResult.getElement();
		if (!(element instanceof PsiMethod)) {
			return null;
		}
		final PsiMethod method = (PsiMethod) element;
		if (method.hasModifierProperty(PsiModifier.PRIVATE) || !resolveResult.isValidResult()) {
			return null;
		}
		TestNGAssert assertMethod = TestNGAssert.find(method);
		if (assertMethod == null) {
			return null;
		}
		return new TestNGAssertHint(assertMethod, method, null, null, null);
	}
}
