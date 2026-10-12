package com.textile.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.textile.accounts", importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayerArchitectureTest {

    @ArchTest
    static final ArchRule servicesShouldNotDependOnControllers =
        classes().that().resideInAPackage("..service..")
            .should().onlyHaveDependentClassesThat().resideInAnyPackage("..controller..", "..service..", "..config..", "..accounts..")
            .allowEmptyShould(true);
}
