package plataforma;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces the boundaries that exist in the current develop package structure. */
@AnalyzeClasses(packages = "plataforma")
class LayerDependencyTest {
    @ArchTest static final ArchRule controllersDoNotReachRepositories = noClasses().that().resideInAPackage("plataforma.controller..").should().dependOnClassesThat().resideInAPackage("plataforma.repository..");
    @ArchTest static final ArchRule dtosDoNotReachPersistence = noClasses().that().resideInAPackage("plataforma.dto..").should().dependOnClassesThat().resideInAnyPackage("plataforma.repository..", "plataforma.service..");

    // Rules below document the allowed direction: controller -> service -> repository -> model.
    @ArchTest static final ArchRule modelDoesNotDependOnOtherLayers = noClasses().that().resideInAPackage("plataforma.model..").should().dependOnClassesThat().resideInAnyPackage("plataforma.controller..", "plataforma.service..", "plataforma.repository..", "plataforma.dto..", "plataforma.mapper..");
    @ArchTest static final ArchRule repositoriesDoNotReachServicesOrControllers = noClasses().that().resideInAPackage("plataforma.repository..").should().dependOnClassesThat().resideInAnyPackage("plataforma.controller..", "plataforma.service..");
    @ArchTest static final ArchRule servicesDoNotReachControllers = noClasses().that().resideInAPackage("plataforma.service..").should().dependOnClassesThat().resideInAPackage("plataforma.controller..");
    @ArchTest static final ArchRule mappersOnlyConvertBetweenModelAndDto = noClasses().that().resideInAPackage("plataforma.mapper..").should().dependOnClassesThat().resideInAnyPackage("plataforma.controller..", "plataforma.service..", "plataforma.repository..");
    @ArchTest static final ArchRule exceptionsDoNotReachApplicationLayers = noClasses().that().resideInAPackage("plataforma.exception..").should().dependOnClassesThat().resideInAnyPackage("plataforma.controller..", "plataforma.service..", "plataforma.repository..");
}
