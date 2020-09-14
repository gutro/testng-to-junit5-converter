/*
 * Copyright 2003-2018 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gearsofleo.intellij.testng.converter.plugin.assertion;

import java.util.function.Supplier;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Consumer;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.junit.JUnitCommonClassNames;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNGToJUnit5AssertionsConverterInspection extends BaseInspection {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestNGToJUnit5AssertionsConverterInspection.class);


	@Nullable
	@Override
	public String getStaticDescription() {
		return "Replace TestNG assertions with JUnit assertions";
	}

	@Override
	@NotNull
	protected String buildErrorString(Object... infos) {
		String name = (String) infos[0];
		String targetClassName = (String) infos[1];
		return InspectionGadgetsBundle.message("junit5.assertions.converter.problem.descriptor", name, targetClassName);
	}

	@Override
	protected InspectionGadgetsFix buildFix(Object... infos) {
		boolean disabledFix = (Boolean) infos[2];
		return !disabledFix ? new ReplaceObsoleteAssertsFix((String) infos[1]) : null;
	}

	@Override
	public BaseInspectionVisitor buildVisitor() {
		return new UseOfObsoleteAssertVisitor();
	}

	private static class UseOfObsoleteAssertVisitor extends BaseInspectionVisitor {

		@Override
		public void visitMethodCallExpression(PsiMethodCallExpression expression) {
			doCheck(expression,
					() -> TestNGAssertHint.create(expression),
					psiMethod -> {
						final PsiClass containingClass = psiMethod.getContainingClass();
						if (containingClass == null) {
							return;
						}

						String methodName = psiMethod.getName();
						registerMethodCallError(expression, containingClass.getName(),
								TestNGAssertToJUnit5Converter.getNewAssertClassName(methodName),
								absentInJUnit5(psiMethod, methodName));
					});
		}

		@Override
		public void visitMethodReferenceExpression(PsiMethodReferenceExpression expression) {

			doCheck(expression,
					() -> TestNGAssertHint.create(expression),
					psiMethod -> {
						final PsiClass containingClass = psiMethod.getContainingClass();
						if (containingClass == null) {
							return;
						}

						String methodName = psiMethod.getName();
						registerError(expression, containingClass.getQualifiedName(),
								TestNGAssertToJUnit5Converter.getNewAssertClassName(methodName),
								absentInJUnit5(psiMethod, methodName));
					});
		}

		private void doCheck(PsiElement expression,
				Supplier<? extends TestNGAssertHint> hintSupplier,
				Consumer<? super PsiMethod> registerError) {

			final Project project = expression.getProject();
			final Module module = ModuleUtilCore.findModuleForPsiElement(expression);
			if (module == null) {
				return;
			}
			final PsiClass newAssertClass = JavaPsiFacade.getInstance(project)
					.findClass(JUnitCommonClassNames.ORG_JUNIT_JUPITER_API_ASSERTIONS,
							GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
			if (newAssertClass == null) {
				LOGGER.warn("JUnit5 not found on module classpath");
				return;
			}

			TestNGAssertHint hint = hintSupplier.get();
			if (hint == null) {
				return;
			}

			final PsiMethod psiMethod = hint.getMethod();
			if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
				return;
			}

			registerError.consume(psiMethod);
		}

		private boolean absentInJUnit5(PsiMethod psiMethod, String methodName) {
			return false;
		}
	}

	public static class ReplaceObsoleteAssertsFix extends InspectionGadgetsFix {
		private final String myBaseClassName;

		ReplaceObsoleteAssertsFix(String baseClassName) {
			myBaseClassName = baseClassName;
		}

		@Override
		protected void doFix(Project project, ProblemDescriptor descriptor) {
			PsiElement element = descriptor.getPsiElement();
			TestNGAssertToJUnit5Converter.replaceAssertion(project, element);
		}

		@Nls
		@NotNull
		@Override
		public String getName() {
			return InspectionGadgetsBundle.message("junit5.assertions.converter.quickfix", myBaseClassName);
		}

		@NotNull
		@Override
		public String getFamilyName() {
			return InspectionGadgetsBundle.message("junit5.assertions.converter.familyName");
		}
	}
}