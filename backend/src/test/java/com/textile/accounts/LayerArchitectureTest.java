package com.textile.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.textile.accounts", importOptions = {ImportOption.DoNotIncludeTests.class})
public class LayerArchitectureTest {

    // Rule 1: Controllers must never access repositories directly (controllers -> services only)
    @ArchTest
    static final ArchRule controllersMustNotAccessRepositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true);

    // Rule 2: Repositories must not depend on controllers or services
    @ArchTest
    static final ArchRule repositoriesMustNotDependOnControllersOrServices =
        noClasses().that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..")
            .allowEmptyShould(true);
}
