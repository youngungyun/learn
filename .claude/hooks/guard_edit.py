#!/usr/bin/env python3
"""PreToolUse(Edit|Write|MultiEdit): 에이전트가 건드리면 안 되는 파일을 막는다.

종료 코드 2 로 끝내면 도구 호출이 취소되고, stderr 내용이 에이전트에게 전달된다.
"""
import json
import os
import re
import sys

MIGRATION = re.compile(r"^src/main/resources/db/migration/V\d+__.+\.sql$")


def check(rel: str, exists: bool) -> str | None:
    """막아야 하면 이유(문자열)를, 허용이면 None 을 돌려준다."""
    if rel == ".env" or (rel.endswith("/.env")):
        return "`.env` 는 시크릿 파일이라 편집할 수 없습니다. 값은 환경변수로 주입하고 예시는 `.env.example` 에 적으세요."
    if MIGRATION.match(rel) and exists:
        return ("이미 있는 Flyway 마이그레이션은 수정할 수 없습니다(적용된 DB 와 체크섬이 어긋납니다). "
                "새 파일 `V{다음번호}__설명.sql` 을 추가하세요.")
    if rel.startswith("config/checkstyle/") or rel.startswith("config/spotbugs/"):
        return ("품질 게이트 설정(config/checkstyle, config/spotbugs)은 에이전트가 수정할 수 없습니다. "
                "규칙이 틀렸다고 판단되면 변경 이유를 사람에게 설명하고 승인을 받으세요.")
    if rel == ".claude/settings.json" or rel.startswith(".claude/hooks/"):
        return "훅과 에이전트 설정은 스스로 수정할 수 없습니다. 변경이 필요하면 사람에게 요청하세요."
    return None


def main() -> int:
    data = json.load(sys.stdin)
    path = (data.get("tool_input") or {}).get("file_path") or ""
    if not path:
        return 0
    project = os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()
    absolute = os.path.abspath(path)
    rel = os.path.relpath(absolute, project).replace(os.sep, "/")
    if rel.startswith(".."):
        return 0  # 프로젝트 밖은 이 훅의 관심사가 아니다.
    reason = check(rel, os.path.exists(absolute))
    if reason:
        print(f"[harness] 차단: {rel}\n{reason}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
