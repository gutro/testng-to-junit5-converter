package com.gearsofleo.intellij.testng.converter.plugin;

import java.util.Arrays;

import com.intellij.psi.PsiClass;

public enum TestNGAssertClass {

	ASSERT("org.testng.Assert"),
	ASSERT_JUNIT("org.testng.AssertJUnit"),
	ARRAY_ASSERTS("org.testng.internal.junit.ArrayAsserts"),
	FILE_ASSERT("org.testng.FileAssert");

	private final String classFqn;

	TestNGAssertClass(String classFqn) {
		this.classFqn = classFqn;
	}

	public String classFqn() {
		return classFqn;
	}

	public static TestNGAssertClass find(PsiClass psiClass) {
		return Arrays.stream(values())
				.filter(c -> c.classFqn.equals(psiClass.getQualifiedName()))
				.findFirst()
				.orElse(null);
	}
}
