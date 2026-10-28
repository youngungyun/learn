#!/usr/bin/env python3
"""PostToolUse(Edit|Write|MultiEdit): 자바 파일을 고친 직후 Checkstyle 로 바로 피드백한다.

실패하면 종료 코드 2 + stderr 로 위반 내용을 에이전트에게 돌려줘 즉시 고치게 한다.
빠른 검사만 한다(전체 검증은 Stop 훅의 몫).
"""
import json
import os
import shlex
import subprocess
import sys


def gradle_command(*tasks: str) -> list[str]:
    override = os.environ.get("HARNESS_GRADLE_CMD")  # 테스트에서 가짜 명령으로 바꿔 끼우는 용도
    if override:
        return shlex.split(override)
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    return [wrapper, *tasks, "--console=plain", "-q"]


def main() -> int:
    data = json.load(sys.stdin)
    path = (data.get("tool_input") or {}).get("file_path") or ""
    if not path.endswith(".java"):
        return 0
    project = os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()
    try:
        result = subprocess.run(gradle_command("checkstyleMain", "checkstyleTest"), cwd=project,
                                capture_output=True, text=True, timeout=170)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"[harness] Checkstyle 을 실행하지 못했습니다: {error}", file=sys.stderr)
        return 0  # 도구 문제로 작업을 막지는 않는다.
    if result.returncode != 0:
        tail = "\n".join((result.stdout + result.stderr).strip().splitlines()[-30:])
        print(f"[harness] Checkstyle 위반이 있습니다. 고친 뒤 계속하세요.\n{tail}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
