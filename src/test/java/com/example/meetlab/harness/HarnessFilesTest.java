package com.example.meetlab.harness;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 하네스 자체를 테스트한다. 지침 문서·훅 설정·실패 기록이 서로 어긋나면(파일 이름 변경, 삭제 등) 빌드가 실패한다.
 * "문서는 낡는다"는 문제를 테스트로 막는 장치다.
 */
@DisplayName("하네스 파일 정합성")
class HarnessFilesTest {

    private static final Path PROJECT = Path.of("").toAbsolutePath();
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "## 명령어", "## 구조", "## 코드 규칙", "## 테스트 규칙", "## 하지 말 것", "## 완료의 정의");

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT.resolve(relative));
    }

    @Test
    @DisplayName("AGENTS.md 에 필수 섹션이 모두 있다")
    void agentsFileHasRequiredSections() throws IOException {
        String agents = read("AGENTS.md");

        assertThat(agents).contains(REQUIRED_SECTIONS);
    }

    @Test
    @DisplayName("CLAUDE.md 는 AGENTS.md 를 가져와 지침이 한곳에만 있다")
    void claudeFileImportsAgentsFile() throws IOException {
        assertThat(read("CLAUDE.md")).contains("@AGENTS.md");
    }

    @Test
    @DisplayName("settings.json 이 올바른 JSON 이고 훅 세 종류가 모두 등록되어 있다")
    void settingsRegistersAllHookEvents() throws IOException {
        JsonNode hooks = new JsonMapper().readTree(read(".claude/settings.json")).get("hooks");

        assertThat(hooks).isNotNull();
        assertThat(hooks.has("PreToolUse")).isTrue();
        assertThat(hooks.has("PostToolUse")).isTrue();
        assertThat(hooks.has("Stop")).isTrue();
    }

    @Test
    @DisplayName("settings.json 이 가리키는 훅 스크립트가 모두 실제로 존재한다")
    void referencedHookScriptsExist() throws IOException {
        Matcher matcher = Pattern.compile("\\.claude/hooks/([\\w]+\\.py)").matcher(read(".claude/settings.json"));
        int found = 0;
        while (matcher.find()) {
            found++;
            assertThat(PROJECT.resolve(".claude/hooks").resolve(matcher.group(1))).exists();
        }
        assertThat(found).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("실패 기록이 강제 수단으로 적어 둔 테스트 클래스가 실제로 존재한다")
    void failureLogReferencesExistingTests() throws IOException {
        String log = read("docs/harness/failure-log.md");
        Matcher matcher = Pattern.compile("\\b([A-Z][A-Za-z]+Test)\\b").matcher(log);
        List<String> testFiles;
        try (Stream<Path> files = Files.walk(PROJECT.resolve("src/test/java"))) {
            testFiles = files.map(path -> path.getFileName().toString()).toList();
        }
        int found = 0;
        while (matcher.find()) {
            found++;
            assertThat(testFiles).as("실패 기록이 가리키는 %s", matcher.group(1)).contains(matcher.group(1) + ".java");
        }
        assertThat(found).isGreaterThan(0);
    }
}
