import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    checkstyle
    jacoco
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.spotbugs") version "6.5.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "WebRTC 화상 통화 시그널링 서버 (품질 기반 -> 모니터링 -> 검색 -> 로그를 단계별로 쌓아 가는 학습 프로젝트)"

// 도구 버전은 한곳에 모아 두면 업그레이드할 때 이 블록만 고치면 된다.
val javaVersion = 25
val jacocoVersion = "0.8.15"
val checkstyleVersion = "14.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // stage 3: 지표 수집(Actuator + Micrometer)과 Prometheus 형식 노출
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // stage 2: H2 대신 운영과 같은 PostgreSQL 을 쓰고, 스키마는 Flyway 마이그레이션으로 관리한다.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // JUnit 6, AssertJ, Mockito, Spring Test가 함께 들어 있다.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4부터 슬라이스 테스트(@WebMvcTest, @DataJpaTest)는 기술별 테스트 스타터로 분리되었다.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    // stage 2: 테스트에서 진짜 PostgreSQL 컨테이너를 띄운다. 버전은 Spring Boot BOM 이 관리한다.
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // stage 5: 구조 규칙을 코드로 검사한다. 코어 API만 쓰고 JUnit 엔진 모듈은 쓰지 않는다.
    testImplementation("com.tngtech.archunit:archunit:1.5.1")
    // Gradle 9부터는 JUnit Platform Launcher를 직접 선언해야 한다.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---------------------------------------------------------------------------
// 테스트
// ---------------------------------------------------------------------------
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // JDK 21+에서 Mockito가 에이전트를 동적으로 붙일 때 나오는 경고를 막는다.
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // 테스트가 끝나면 커버리지 리포트를 자동으로 만든다.
    finalizedBy(tasks.jacocoTestReport)
}

// ---------------------------------------------------------------------------
// JaCoCo: 커버리지 리포트와 최소 기준
// ---------------------------------------------------------------------------
jacoco {
    toolVersion = jacocoVersion
}

// 측정에서 제외할 클래스. main 메서드만 있는 진입점 클래스는 테스트 가치가 낮다.
val jacocoExcludes = listOf("**/MeetlabApplication*")

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
    )
    reports {
        xml.required = true // Jenkins, SonarQube 같은 도구가 읽는 형식
        html.required = true // 사람이 읽는 형식
        csv.required = false
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
    )
    violationRules {
        rule {
            // 처음에는 지키기 쉬운 기준으로 시작하고, 테스트가 쌓이면 올린다.
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Checkstyle: 코드 스타일
// ---------------------------------------------------------------------------
checkstyle {
    toolVersion = checkstyleVersion
    configFile = file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required = true // Jenkins 등 CI 도구가 읽는 형식
        html.required = true // 사람이 읽는 형식
    }
}

// ---------------------------------------------------------------------------
// SpotBugs: 바이트코드 기반 버그 패턴 탐지
// ---------------------------------------------------------------------------
spotbugs {
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/spotbugs-exclude.xml"))
}

// SpotBugs 태스크는 리포트 형식을 한 번에 하나만 켜는 것이 안전하다.
// 로컬에서는 사람이 읽는 html, Jenkins에서는 -PspotbugsReport=xml 로 xml을 쓴다.
val spotbugsReportType = providers.gradleProperty("spotbugsReport").getOrElse("html")

tasks.withType<SpotBugsTask>().configureEach {
    reports.create(spotbugsReportType) {
        required.set(true)
    }
}

// 테스트 코드는 Mockito, 리플렉션 사용으로 오탐이 많아 운영 코드만 분석한다.
tasks.named("spotbugsTest") {
    enabled = false
}

// ---------------------------------------------------------------------------
// 품질 게이트: ./gradlew check 한 번으로 테스트 + 커버리지 기준 + 정적 분석을 모두 실행한다.
// ---------------------------------------------------------------------------
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
