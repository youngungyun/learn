package com.example.meetlab.harness;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구조 규칙을 문서가 아니라 테스트로 강제한다. 규칙을 어기는 코드는 빌드에서 실패한다.
 * 각 규칙의 근거는 docs/harness/failure-log.md 에 있다.
 */
@DisplayName("아키텍처 규칙")
class ArchitectureRulesTest {

    private static final String ROOT = "com.example.meetlab";
    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
    }

    @Test
    @DisplayName("방 도메인은 서비스·웹·다른 기능 패키지에 의존하지 않는다")
    void domainIsIndependentOfUpperLayers() {
        noClasses().that().resideInAPackage("..room.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..room.service..", "..room.web..", "..signaling..", "..ice..", "..telemetry..")
                .check(classes);
    }

    @Test
    @DisplayName("웹 계층(컨트롤러)은 Repository 를 직접 쓰지 않는다")
    void webLayerDoesNotUseRepositories() {
        noClasses().that().resideInAPackage("..web..")
                .or().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat(simpleNameEndingWith("Repository"))
                .check(classes);
    }

    @Test
    @DisplayName("시그널링은 방 정보를 서비스로만 얻는다(Repository, 웹 계층 사용 금지)")
    void signalingUsesRoomServiceOnly() {
        noClasses().that().resideInAPackage("..signaling..")
                .should().dependOnClassesThat(simpleNameEndingWith("Repository"))
                .check(classes);
        noClasses().that().resideInAPackage("..signaling..")
                .should().dependOnClassesThat().resideInAPackage("..room.web..")
                .check(classes);
    }

    @Test
    @DisplayName("최상위 기능 패키지끼리 순환 의존이 없다")
    void featurePackagesAreFreeOfCycles() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    @DisplayName("필드 주입(@Autowired 필드)을 쓰지 않는다")
    void noFieldInjection() {
        noFields().should().beAnnotatedWith(Autowired.class).check(classes);
    }

    @Test
    @DisplayName("컨트롤러 클래스 이름은 Controller 로 끝난다")
    void controllersAreNamedController() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(classes);
    }

    @Test
    @DisplayName("java.util.logging 을 쓰지 않는다")
    void noJavaUtilLogging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes);
    }

    @Test
    @DisplayName("System.out / System.err 를 쓰지 않는다")
    void noStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes);
    }
}
