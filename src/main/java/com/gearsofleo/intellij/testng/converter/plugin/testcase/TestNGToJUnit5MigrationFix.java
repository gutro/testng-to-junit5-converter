package com.gearsofleo.intellij.testng.converter.plugin.testcase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.codeInspection.BatchQuickFix;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringManager;
import com.intellij.refactoring.migration.MigrationManager;
import com.intellij.refactoring.migration.MigrationMap;
import com.intellij.refactoring.migration.MigrationMapEntry;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class TestNGToJUnit5MigrationFix extends InspectionGadgetsFix implements BatchQuickFix<CommonProblemDescriptor> {

	private static final String MIGRATION_MAP_NAME = "TestNG -> JUnit 5";

	@Nls
	@NotNull
	@Override
	public String getFamilyName() {
		return InspectionGadgetsBundle.message("junit5.converter.fix.name");
	}

	@Override
	protected void doFix(Project project, ProblemDescriptor descriptor) {
		PsiClass psiClass = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiClass.class);
		if (psiClass != null) {
			MigrationMap migrationMap = getOrCreateMigrationMap(project);
			new TestNGToJUnit5MigrationProcessor(project, migrationMap, Collections
					.singleton(psiClass.getContainingFile())).run();
		}
	}

	@Override
	public boolean startInWriteAction() {
		return false;
	}

	@Override
	public void applyFix(@NotNull Project project,
			@NotNull CommonProblemDescriptor[] descriptors,
			@NotNull List psiElementsToIgnore,
			@Nullable Runnable refreshViews) {
		Set<PsiFile> files = Arrays.stream(descriptors)
				.map(descriptor -> ((ProblemDescriptor) descriptor).getPsiElement())
				.filter(Objects::nonNull)
				.map(PsiElement::getContainingFile).collect(Collectors.toSet());
		if (!files.isEmpty()) {
			MigrationMap migrationMap = getOrCreateMigrationMap(project);
			new TestNGToJUnit5MigrationProcessor(project, migrationMap, files).run();
			if (refreshViews != null) {
				refreshViews.run();
			}
		}
	}

	private MigrationMap getOrCreateMigrationMap(@NotNull Project project) {
		MigrationManager manager = RefactoringManager.getInstance(project).getMigrateManager();
		MigrationMap migrationMap = manager.findMigrationMap(MIGRATION_MAP_NAME);
		if (migrationMap == null) {
			migrationMap = new MigrationMap();
			migrationMap.setName(MIGRATION_MAP_NAME);
			migrationMap.addEntry(entry("org.testng.annotations.Test", "org.junit.jupiter.api.Test"));
			migrationMap.addEntry(entry("org.testng.annotations.BeforeMethod", "org.junit.jupiter.api.BeforeEach"));
			migrationMap.addEntry(entry("org.testng.annotations.BeforeClass", "org.junit.jupiter.api.BeforeAll"));
			migrationMap.addEntry(entry("org.testng.annotations.AfterMethod", "org.junit.jupiter.api.AfterEach"));
			migrationMap.addEntry(entry("org.testng.annotations.AfterClass", "org.junit.jupiter.api.AfterAll"));
		}
		return migrationMap;
	}

	private static MigrationMapEntry entry(String from, String to) {
		return new MigrationMapEntry(from, to, MigrationMapEntry.CLASS, false);
	}

}
