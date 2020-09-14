package com.gearsofleo.intellij.testng.converter.plugin.testcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.gearsofleo.intellij.testng.converter.plugin.assertion.TestNGToJUnit5AssertionsConverterInspection;
import com.intellij.codeInsight.intention.impl.AddOnDemandStaticImportAction;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.actions.CleanupInspectionUtil;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.java.refactoring.JavaRefactoringBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.migration.MigrationMap;
import com.intellij.refactoring.migration.MigrationProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import static com.gearsofleo.intellij.testng.converter.plugin.testcase.TestAnnotationAttributesConverter.processTestAnnotation;
import static com.intellij.util.ArrayUtil.mergeArrays;

public class TestNGToJUnit5MigrationProcessor extends MigrationProcessor {

	private final Project myProject;
	private final Set<? extends PsiFile> myFiles;
	List<SmartPsiElementPointer<PsiElement>> myReplacedRefs = new ArrayList<>();

	TestNGToJUnit5MigrationProcessor(Project project, MigrationMap migrationMap, Set<? extends PsiFile> files) {
		super(project, migrationMap, GlobalSearchScope
				.filesWithoutLibrariesScope(project, ContainerUtil.map(files, PsiFile::getVirtualFile)));
		setPrepareSuccessfulSwingThreadCallback(EmptyRunnable.INSTANCE);
		myProject = project;
		myFiles = files;
	}

	@Override
	protected @NotNull
	UsageInfo[] findUsages() {
		UsageInfo[] usages = super.findUsages();
		InspectionManager inspectionManager = InspectionManager.getInstance(myProject);
		GlobalInspectionContext globalContext = inspectionManager.createNewGlobalContext();
		LocalInspectionToolWrapper assertionsConverter = new LocalInspectionToolWrapper(new TestNGToJUnit5AssertionsConverterInspection());

		Stream<ProblemDescriptor> stream = myFiles.stream()
				.flatMap(file -> InspectionEngine.runInspectionOnFile(file, assertionsConverter, globalContext)
						.stream());
		UsageInfo[] descriptors = stream.map(MyDescriptionBasedUsageInfo::new)
				.toArray(UsageInfo[]::new);

		UsageInfo[] testNgToSpringBootTestUsages = SpringTestConverter.findUsages(myFiles);
		return mergeArrays(mergeArrays(usages, descriptors), testNgToSpringBootTestUsages);
	}

	@Override
	protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
		if (refUsages.get().length == 0) {
			Messages.showInfoMessage(myProject, JavaRefactoringBundle
					.message("migration.no.usages.found.in.the.project"), "Migration");
			return false;
		}
		setPreviewUsages(false);
		return true;
	}

	@Override
	protected void performRefactoring(@NotNull UsageInfo[] usages) {
		List<UsageInfo> migrateUsages = new ArrayList<>();
		List<ProblemDescriptor> descriptions = new ArrayList<>();
		SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(myProject);
		JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(myProject);

		SpringTestConverter.applyFixes(usages, javaCodeStyleManager);
		for (UsageInfo usage : usages) {
			if (usage instanceof MyDescriptionBasedUsageInfo) {
				ProblemDescriptor descriptor = ((MyDescriptionBasedUsageInfo) usage).myDescriptor;
				descriptions.add(descriptor);
				markUsagesImportedThroughStaticImport(smartPointerManager, descriptor);
			}
			else {
				if (usage.getElement() instanceof PsiJavaCodeReferenceElement) {
					PsiJavaCodeReferenceElement reference = (PsiJavaCodeReferenceElement) usage.getElement();
					if (reference.getParent() instanceof PsiAnnotation) {
						PsiAnnotation annotation = (PsiAnnotation) reference.getParent();
						if ("org.testng.annotations.Test".equals(annotation.getQualifiedName())) {
							processTestAnnotation(javaCodeStyleManager, annotation);
						}
					}
				}
				migrateUsages.add(usage);
			}
		}
		super.performRefactoring(migrateUsages.toArray(UsageInfo.EMPTY_ARRAY));
		if (!descriptions.isEmpty()) {
			CleanupInspectionUtil.getInstance()
					.applyFixes(myProject,
							"Convert Assertions",
							descriptions,
							TestNGToJUnit5AssertionsConverterInspection.ReplaceObsoleteAssertsFix.class,
							false);
		}
	}

	@Override
	protected void performPsiSpoilingRefactoring() {
		super.performPsiSpoilingRefactoring();
		tryToRestoreStaticImportsOnNewAssertions();
	}

	private void markUsagesImportedThroughStaticImport(SmartPointerManager smartPointerManager, ProblemDescriptor descriptor) {
		PsiElement element = descriptor.getPsiElement();
		PsiMethodCallExpression callExpression = PsiTreeUtil
				.getParentOfType(element, PsiMethodCallExpression.class);
		if (callExpression != null) {
			PsiReferenceExpression methodExpression = callExpression.getMethodExpression();
			PsiElement scope = methodExpression.getQualifierExpression() == null
					? methodExpression.advancedResolve(false).getCurrentFileResolveScope()
					: null;
			if (scope instanceof PsiImportStaticStatement && ((PsiImportStaticStatement) scope).isOnDemand()) {
				myReplacedRefs.add(smartPointerManager.createSmartPsiElementPointer(callExpression));
			}
		}
	}

	private void tryToRestoreStaticImportsOnNewAssertions() {
		for (SmartPsiElementPointer<PsiElement> ref : myReplacedRefs) {
			PsiElement element = ref.getElement();
			if (element instanceof PsiMethodCallExpression) {
				PsiExpression qualifierExpression = ((PsiMethodCallExpression) element).getMethodExpression()
						.getQualifierExpression();
				if (qualifierExpression != null) {
					PsiElement referenceNameElement = ((PsiReferenceExpression) qualifierExpression)
							.getReferenceNameElement();
					PsiClass aClass = referenceNameElement != null ? AddOnDemandStaticImportAction
							.getClassToPerformStaticImport(referenceNameElement) : null;
					PsiFile containingFile = element.getContainingFile();
					if (aClass != null && !AddOnDemandStaticImportAction
							.invoke(myProject, containingFile, null, referenceNameElement)) {
						PsiImportStatementBase importReferenceTo = PsiTreeUtil
								.getParentOfType(((PsiJavaFile) containingFile)
										.findImportReferenceTo(aClass), PsiImportStatementBase.class);
						if (importReferenceTo != null) importReferenceTo.delete();
					}
				}
			}
		}
	}

	private static class MyDescriptionBasedUsageInfo extends UsageInfo {
		private final ProblemDescriptor myDescriptor;

		MyDescriptionBasedUsageInfo(ProblemDescriptor descriptor) {
			super(descriptor.getPsiElement());
			myDescriptor = descriptor;
		}
	}
}
