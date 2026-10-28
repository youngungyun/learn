#!/usr/bin/env python3
"""Stop: 에이전트가 "끝났다"고 하기 전에 `./gradlew check` 를 강제한다.

- stop_hook_active 가 true 면 이미 한 번 되돌려 보낸 상태다. 또 막으면 무한 루프가 되므로 통과시킨다.
- 소스·빌드 설정이 바뀌지 않았으면 검사하지 않는다(질문에 답만 한 턴까지 막지 않기 위해).
"""
import json
import os
import shlex
import subprocess
import sys

WATCHED = ("src/", "build.gradle.kts", "settings.gradle.kts", "config/", "Jenkinsfile")


def changed_files(project: str) -> list[str]:
    result = subprocess.run(["git", "status", "--porcelain"], cwd=project, capture_output=True, text=True)
    if result.returncode != 0:
        return []
    return [line[3:].strip() for line in result.stdout.splitlines() if len(line) > 3]


def gradle_command() -> list[str]:
    override = os.environ.get("HARNESS_GRADLE_CMD")
    if override:
        return shlex.split(override)
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    return [wrapper, "check", "--console=plain", "-q"]


def main() -> int:
    data = json.load(sys.stdin)
    if data.get("stop_hook_active"):
        return 0
    project = os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()
    if not any(f.startswith(WATCHED) for f in changed_files(project)):
        return 0
    try:
        result = subprocess.run(gradle_command(), cwd=project, capture_output=True, text=True, timeout=840)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"[harness] ./gradlew check 를 실행하지 못했습니다: {error}", file=sys.stderr)
        return 0
    if result.returncode != 0:
        tail = "\n".join((result.stdout + result.stderr).strip().splitlines()[-40:])
        print("[harness] `./gradlew check` 가 실패했습니다. 통과시킨 뒤 끝내세요.\n" + tail, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
