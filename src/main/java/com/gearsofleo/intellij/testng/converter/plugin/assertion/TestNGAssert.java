package com.gearsofleo.intellij.testng.converter.plugin.assertion;

import java.util.Arrays;

import com.gearsofleo.intellij.testng.converter.plugin.TestNGAssertClass;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;

import static com.gearsofleo.intellij.testng.converter.plugin.TestNGAssertClass.ASSERT;
import static com.gearsofleo.intellij.testng.converter.plugin.TestNGAssertClass.ASSERT_JUNIT;
import static com.gearsofleo.intellij.testng.converter.plugin.TestNGAssertClass.FILE_ASSERT;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.ACTUAL_BEFORE_EXPECTED;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.EXPECTED_BEFORE_ACTUAL;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.ExpectedActualParamOrder.NONE;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.MessageParamOrder.MESSAGE_FIRST;
import static com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGAssert.MessageParamOrder.MESSAGE_LAST;

public enum TestNGAssert {

	ASSERT_ASSERT_EQUALS(ASSERT, MESSAGE_LAST, ACTUAL_BEFORE_EXPECTED, "assertEquals", 2),
	ASSERT_ASSERT_NOT_EQUALS(ASSERT, MESSAGE_LAST, ACTUAL_BEFORE_EXPECTED, "assertNotEquals", 2),
	ASSERT_ASSERT_SAME(ASSERT, MESSAGE_LAST, ACTUAL_BEFORE_EXPECTED, "assertSame", 2),
	ASSERT_ASSERT_NOT_SAME(ASSERT, MESSAGE_LAST, ACTUAL_BEFORE_EXPECTED, "assertNotSame", 2),
	ASSERT_ASSERT_TRUE(ASSERT, MESSAGE_LAST, NONE, "assertTrue", 1),
	ASSERT_ASSERT_FALSE(ASSERT, MESSAGE_LAST, NONE, "assertFalse", 1),
	ASSERT_ASSERT_NULL(ASSERT, MESSAGE_LAST, NONE, "assertNull", 1),
	ASSERT_ASSERT_NOT_NULL(ASSERT, MESSAGE_LAST, NONE, "assertNotNull", 1),
	ASSERT_FAIL(ASSERT, MESSAGE_FIRST, NONE, "fail", 0),

	ASSERT_JUNIT_ASSERT_EQUALS(ASSERT_JUNIT, MESSAGE_FIRST, EXPECTED_BEFORE_ACTUAL, "assertEquals", 2),
	ASSERT_JUNIT_ASSERT_NOT_EQUALS(ASSERT_JUNIT, MESSAGE_FIRST, EXPECTED_BEFORE_ACTUAL, "assertNotEquals", 2),
	ASSERT_JUNIT_ASSERT_SAME(ASSERT_JUNIT, MESSAGE_FIRST, EXPECTED_BEFORE_ACTUAL, "assertSame", 2),
	ASSERT_JUNIT_ASSERT_NOT_SAME(ASSERT_JUNIT, MESSAGE_FIRST, EXPECTED_BEFORE_ACTUAL, "assertNotSame", 2),
	ASSERT_JUNIT_ASSERT_TRUE(ASSERT_JUNIT, MESSAGE_FIRST, NONE, "assertTrue", 1),
	ASSERT_JUNIT_ASSERT_FALSE(ASSERT_JUNIT, MESSAGE_FIRST, NONE, "assertFalse", 1),
	ASSERT_JUNIT_ASSERT_NULL(ASSERT_JUNIT, MESSAGE_FIRST, NONE, "assertNull", 1),
	ASSERT_JUNIT_ASSERT_NOT_NULL(ASSERT_JUNIT, MESSAGE_FIRST, NONE, "assertNotNull", 1),
	ASSERT_JUNIT_FAIL(ASSERT_JUNIT, MESSAGE_FIRST, NONE, "fail", 0),

	ASSERT_ARRAY_EQUALS(ASSERT_JUNIT, MESSAGE_FIRST, EXPECTED_BEFORE_ACTUAL, "assertArrayEquals", 2),

	FILE_ASSERT_DIRECTORY(FILE_ASSERT, MESSAGE_LAST, NONE, "assertDirectory", 1),
	FILE_ASSERT_FILE(FILE_ASSERT, MESSAGE_LAST, NONE, "assertFile", 1),
	FILE_ASSERT_LENGTH(FILE_ASSERT, MESSAGE_LAST, NONE, "assertLength", 2),
	FILE_ASSERT_MAX_LENGTH(FILE_ASSERT, MESSAGE_LAST, NONE, "assertMaxLength", 2),
	FILE_ASSERT_MIN_LENGTH(FILE_ASSERT, MESSAGE_LAST, NONE, "assertMinLength", 2),
	FILE_ASSERT_READABLE(FILE_ASSERT, MESSAGE_LAST, NONE, "assertReadable", 1),
	FILE_ASSERT_READ_WRITE(FILE_ASSERT, MESSAGE_LAST, NONE, "assertReadWrite", 1),
	FILE_ASSERT_WRITEABLE(FILE_ASSERT, MESSAGE_LAST, NONE, "assertWriteable", 1),
	FILE_ASSERT_FAIL(FILE_ASSERT, MESSAGE_FIRST, NONE, "fail", 0);

	private final TestNGAssertClass assertClass;
	private final MessageParamOrder messageParamOrder;
	private final ExpectedActualParamOrder expectedActualParamOrder;

	private final String name;
	private final int minArgCount;

	TestNGAssert(TestNGAssertClass assertClass, MessageParamOrder messageParamOrder, ExpectedActualParamOrder expectedActualParamOrder, String name, int minArgCount) {
		this.assertClass = assertClass;
		this.messageParamOrder = messageParamOrder;
		this.expectedActualParamOrder = expectedActualParamOrder;
		this.name = name;
		this.minArgCount = minArgCount;
	}

	public TestNGAssertClass getAssertClass() {
		return assertClass;
	}

	public MessageParamOrder getMessageParamOrder() {
		return messageParamOrder;
	}

	public ExpectedActualParamOrder getExpectedActualParamOrder() {
		return expectedActualParamOrder;
	}

	public static TestNGAssert find(PsiMethod psiMethod) {
		PsiClass assertClass = psiMethod.getContainingClass();
		if (assertClass == null) {
			return null;
		}
		TestNGAssertClass testNGAssertClass = TestNGAssertClass.find(assertClass);
		if (testNGAssertClass == null) {
			return null;
		}
		return Arrays.stream(values())
				.filter(a -> a.assertClass.equals(testNGAssertClass))
				.filter(a -> a.name.equals(psiMethod.getName()))
				.findFirst()
				.orElse(null);
	}

	PsiExpression getMessage(PsiMethodCallExpression methodCallExpression) {
		if (methodCallExpression.getArgumentList().getExpressionCount() <= minArgCount) {
			return null;
		}
		final JavaResolveResult resolveResult = methodCallExpression.resolveMethodGenerics();
		final PsiMethod method = (PsiMethod) resolveResult.getElement();
		if (method == null || method.hasModifierProperty(PsiModifier.PRIVATE) || !resolveResult.isValidResult()) {
			return null;
		}
		final PsiParameter[] parameters = method.getParameterList().getParameters();

		int paramIndex;
		if (messageParamOrder == MESSAGE_FIRST) {
			paramIndex = 0;
		}
		else {
			paramIndex = parameters.length - 1;
		}

		if (parameters[paramIndex].getType().equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
			return methodCallExpression.getArgumentList().getExpressions()[paramIndex];
		}
		return null;
	}

	public enum MessageParamOrder {
		MESSAGE_FIRST, MESSAGE_LAST
	}

	public enum ExpectedActualParamOrder {
		NONE, EXPECTED_BEFORE_ACTUAL, ACTUAL_BEFORE_EXPECTED
	}
}
