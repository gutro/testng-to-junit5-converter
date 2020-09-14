// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gearsofleo.intellij.testng.converter.plugin.testcase;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testIntegration.TestFramework;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.junit.JUnitCommonClassNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestNGToJUnit5ConverterInspection extends BaseInspection {

	@NotNull
	@Override
	protected String buildErrorString(Object... infos) {
		return "#ref can be JUnit 5 test";
	}

	@Override
	public boolean shouldInspect(PsiFile file) {
		if (!JavaVersionService.getInstance().isAtLeast(file, JavaSdkVersion.JDK_1_8)) return false;
		if (JavaPsiFacade.getInstance(file.getProject())
				.findClass(JUnitCommonClassNames.ORG_JUNIT_JUPITER_API_ASSERTIONS, file.getResolveScope()) == null) {
			return false;
		}
		return super.shouldInspect(file);
	}

	@Nullable
	@Override
	protected InspectionGadgetsFix buildFix(Object... infos) {
		return new TestNGToJUnit5MigrationFix();
	}

	@Override
	public BaseInspectionVisitor buildVisitor() {
		return new BaseInspectionVisitor() {

			@Override
			public void visitClass(PsiClass aClass) {
				TestFramework framework = TestFrameworks.detectFramework(aClass);
				if (framework == null || !"TestNG".equals(framework.getName())) {
					return;
				}

				registerClassError(aClass);
			}
		};
	}

}
