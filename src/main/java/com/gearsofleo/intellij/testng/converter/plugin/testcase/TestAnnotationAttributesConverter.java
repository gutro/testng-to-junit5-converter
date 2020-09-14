package com.gearsofleo.intellij.testng.converter.plugin.testcase;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiConstantEvaluationHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

import static com.siyeh.ig.junit.JUnitCommonClassNames.ORG_HAMCREST_MATCHER_ASSERT;
import static com.siyeh.ig.junit.JUnitCommonClassNames.ORG_JUNIT_JUPITER_API_ASSERTIONS;

/**
 * Converts TestNG @Test annotation attributes to corresponding JUnit 5 constructs.
 * All converted attributes are removed, but the unconverted attributes are left lingering,
 * for the user to manually take care of.
 */
public class TestAnnotationAttributesConverter {

	public static void processTestAnnotation(JavaCodeStyleManager javaCodeStyleManager, PsiAnnotation annotation) {
		convertAssertThrows(javaCodeStyleManager, annotation);
		convertDisabled(javaCodeStyleManager, annotation);
	}

	private static void convertDisabled(JavaCodeStyleManager javaCodeStyleManager, PsiAnnotation annotation) {
		PsiNameValuePair enabledAttribute = AnnotationUtil.findDeclaredAttribute(annotation, "enabled");
		if (enabledAttribute != null) {
			PsiAnnotationMemberValue enabled = enabledAttribute.getValue();
			if (enabled != null) {
				PsiConstantEvaluationHelper evaluationHelper = JavaPsiFacade.getInstance(enabled.getProject())
						.getConstantEvaluationHelper();
				Object constValue = evaluationHelper.computeConstantExpression(enabled);
				if (constValue instanceof Boolean) {
					if (!(Boolean) constValue) {
						// @Test is marked with "enabled = false"
						PsiAnnotationOwner owner = annotation.getOwner();
						if (owner != null) {
							PsiAnnotation disabled = owner.addAnnotation("org.junit.jupiter.api.Disabled");
							javaCodeStyleManager.shortenClassReferences(disabled);
						}
					}
				}
			}
			enabledAttribute.delete();
			//delete empty parenthesis if there are no attributes left
			if (annotation.getParameterList().getAttributes().length == 0) {
				for (PsiElement child : annotation.getParameterList().getChildren()) {
					child.delete();
				}
			}

		}
	}

	private static void convertAssertThrows(JavaCodeStyleManager javaCodeStyleManager, PsiAnnotation annotation) {
		PsiAnnotationMemberValue expectedExceptions = annotation.findDeclaredAttributeValue("expectedExceptions");
		if (expectedExceptions != null) {
			PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
			if (method == null) {
				return;
			}
			PsiClassObjectAccessExpression excClass;
			if (expectedExceptions instanceof PsiClassObjectAccessExpression) {
				excClass = (PsiClassObjectAccessExpression) expectedExceptions;
			}
			else {
				excClass = PsiTreeUtil.getChildOfType(expectedExceptions, PsiClassObjectAccessExpression.class);
			}
			if (excClass == null) {
				return;
			}

			PsiAnnotationMemberValue expectedMessageValue = annotation
					.findDeclaredAttributeValue("expectedExceptionsMessageRegExp");

			if (method.getBody() == null) {
				return;
			}
			PsiStatement[] statements = method.getBody().getStatements();
			if (statements.length == 0) {
				return;
			}
			PsiStatement lastStatement = statements[statements.length - 1];
			String lastStatementText = lastStatement.getText().trim();
			if (lastStatementText.endsWith(";")) {
				lastStatementText = lastStatementText.substring(0, lastStatementText.length() - 1);
			}

			PsiElementFactory psiElementFactory = PsiElementFactory.getInstance(annotation.getProject());
			PsiElement assertThrowsStatement = (PsiExpressionStatement) psiElementFactory
					.createStatementFromText(
							ORG_JUNIT_JUPITER_API_ASSERTIONS + ".assertThrows(" + excClass.getText()
									+ ", () -> " + lastStatementText + ");", method);
			assertThrowsStatement = javaCodeStyleManager.shortenClassReferences(assertThrowsStatement);


			if (expectedMessageValue == null) {
				lastStatement.replace(assertThrowsStatement);
			}
			else {
				lastStatement.replace(psiElementFactory.createStatementFromText(
						excClass.getOperand().getText() + " expectedException = "
								+ assertThrowsStatement.getText(), method
				));
				PsiExpression matchesPatternExpression = psiElementFactory.createExpressionFromText(
						"org.hamcrest.Matchers.matchesPattern(" + expectedMessageValue.getText() + ")", method);
				PsiExpression assertThatExpression = psiElementFactory
						.createExpressionFromText(ORG_HAMCREST_MATCHER_ASSERT +
								".assertThat(expectedException.getMessage(), " + matchesPatternExpression
								.getText() + ")", method);
				PsiElement assertThatStatement = psiElementFactory
						.createStatementFromText(assertThatExpression.getText() + ";", method);

				assertThatStatement = javaCodeStyleManager.shortenClassReferences(assertThatStatement);

				method.getBody().add(assertThatStatement);


				//delete expectedExceptionsMessageRegExp attribute
				PsiElement expectedMessageAttribute = PsiTreeUtil
						.findFirstParent(expectedMessageValue, e -> e instanceof PsiNameValuePair);
				if (expectedMessageAttribute != null) {
					expectedMessageAttribute.delete();
				}
			}
			//delete expectedExceptions attribute
			PsiElement expectedExceptionsAttribute = PsiTreeUtil
					.findFirstParent(expectedExceptions, e -> e instanceof PsiNameValuePair);
			if (expectedExceptionsAttribute != null) {
				expectedExceptionsAttribute.delete();
			}
			//delete empty parenthesis if there are no attributes left
			if (annotation.getParameterList().getAttributes().length == 0) {
				for (PsiElement child : annotation.getParameterList().getChildren()) {
					child.delete();
				}
			}
		}
	}
}
