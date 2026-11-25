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
}
