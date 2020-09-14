package com.gearsofleo.intellij.testng.converter.plugin.testcase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.TestFrameworks;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.testIntegration.TestFramework;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Tries to convert spring aspects of a TestNG test case.
 * I.e. replace inheritance with JUnit 5 / Spring annotations.
 */
public class SpringTestConverter {

	public static final String SPRING_BOOT_TEST_ANNOTATION = "org.springframework.boot.test.context.SpringBootTest";
	public static final String TRANSACTIONAL_ANNOTATION = "org.springframework.transaction.annotation.Transactional";
	public static final String CONTEXT_CONFIGURATION_ANNOTATION = "org.springframework.test.context.ContextConfiguration";

	public static UsageInfo[] findUsages(Set<? extends PsiFile> psiFiles) {

		List<UsageInfo> usages = new ArrayList<>();
		for (PsiFile psiFile : psiFiles) {
			if (psiFile instanceof PsiJavaFile) {
				for (PsiClass psiClass : ((PsiJavaFile) psiFile).getClasses()) {
					usages.addAll(findUsages(psiClass));
				}
			}
		}
		return usages.toArray(new UsageInfo[0]);
	}

	public static List<UsageInfo> findUsages(PsiClass psiClass) {
		List<UsageInfo> usages = new ArrayList<>();

		TestFramework framework = TestFrameworks.detectFramework(psiClass);
		if (framework != null && "TestNG".equals(framework.getName())) {

			if (!psiClass.hasAnnotation(TRANSACTIONAL_ANNOTATION)) {
				if (Arrays.stream(psiClass.getSupers())
						.anyMatch(s -> s.hasAnnotation(TRANSACTIONAL_ANNOTATION))) {
					usages.add(new TransactionalOnSuperUsageInfo(psiClass));
				}
			}

			if (!psiClass.hasAnnotation(SPRING_BOOT_TEST_ANNOTATION)
					&& psiClass.hasAnnotation(CONTEXT_CONFIGURATION_ANNOTATION)) {
				if (JavaPsiFacade.getInstance(psiClass.getProject())
						.findClass(SPRING_BOOT_TEST_ANNOTATION, psiClass.getResolveScope()) != null) {
					usages.add(new AddSpringBootTestUsageInfo(psiClass));
				}
			}
		}
		return usages;
	}

	public static void applyFixes(UsageInfo[] usages, JavaCodeStyleManager javaCodeStyleManager) {
		for (UsageInfo usageInfo : usages) {
			if (usageInfo instanceof TransactionalOnSuperUsageInfo) {
				PsiClass psiClass = (PsiClass) usageInfo.getElement();
				if (psiClass == null || psiClass.getModifierList() == null) {
					continue;
				}
				//move @Transactional to current class (to prepare removal of inheritance)
				PsiAnnotation transactionalAnnotation = psiClass.getModifierList()
						.addAnnotation(TRANSACTIONAL_ANNOTATION);
				javaCodeStyleManager.shortenClassReferences(transactionalAnnotation);
			}
			if (usageInfo instanceof AddSpringBootTestUsageInfo) {
				PsiClass psiClass = (PsiClass) usageInfo.getElement();
				if (psiClass == null || psiClass.getModifierList() == null) {
					continue;
				}
				//add @SpringBootTest
				PsiAnnotation ctxConfigAnnotation = psiClass.getAnnotation(CONTEXT_CONFIGURATION_ANNOTATION);
				if (ctxConfigAnnotation != null) {
					PsiAnnotation springBootTestAnnotation = psiClass.getModifierList()
							.addAnnotation(SPRING_BOOT_TEST_ANNOTATION);
					springBootTestAnnotation = (PsiAnnotation) javaCodeStyleManager
							.shortenClassReferences(springBootTestAnnotation);
					PsiNameValuePair classesAttr = AnnotationUtil.findDeclaredAttribute(ctxConfigAnnotation, "classes");
					if (classesAttr != null) {
						PsiAnnotationMemberValue classesValue = classesAttr.getValue();
						if (classesValue != null) {
							springBootTestAnnotation.setDeclaredAttributeValue("classes", classesValue);
						}
						classesAttr.delete();
						if (ctxConfigAnnotation.getParameterList().getAttributes().length == 0) {
							ctxConfigAnnotation.delete();
						}
					}
				}
			}
		}
	}

	private static class TransactionalOnSuperUsageInfo extends UsageInfo {
		public TransactionalOnSuperUsageInfo(@NotNull PsiClass psiClass) {
			super(psiClass);
		}
	}

	private static class AddSpringBootTestUsageInfo extends UsageInfo {
		public AddSpringBootTestUsageInfo(@NotNull PsiClass psiClass) {
			super(psiClass);
		}
	}
}
