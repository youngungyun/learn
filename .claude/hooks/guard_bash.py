#!/usr/bin/env python3
"""PreToolUse(Bash): 되돌리기 어렵거나 품질 게이트를 우회하는 명령을 막는다."""
import json
import re
import sys

RULES = [
    (r"\bgit\s+push\b.*(--force\b|--force-with-lease|\s-f\b)", "강제 푸시는 금지입니다."),
    (r"\bgit\s+push\b.*\b(main|master)\b", "main 에 직접 푸시하지 말고 브랜치를 만들어 PR 로 올리세요."),
    (r"--no-verify\b", "훅/검증 우회(--no-verify)는 금지입니다."),
    (r"\bgit\s+reset\s+--hard\b", "작업 내용을 지우는 git reset --hard 는 금지입니다. 필요하면 사람에게 확인하세요."),
    (r"\brm\s+-\w*[rR]\w*\s+(/|~|\$HOME|\.git\b|\*)", "광범위한 rm -r 은 금지입니다. 지울 대상을 정확히 지정하세요."),
    (r"\bgradlew?\b.*\s-x\s*(test|check|jacoco\w*|checkstyle\w*|spotbugs\w*)\b",
     "테스트/품질 검사를 건너뛰는 `-x` 옵션은 금지입니다. 실패 원인을 고치세요."),
    (r"\bdocker\s+compose\b.*\bdown\b.*(\s-v\b|--volumes)", "볼륨을 지우는 `compose down -v` 는 금지입니다(DB/Grafana 데이터 삭제)."),
    (r"\b(curl|wget)\b[^|;]*\|\s*(sudo\s+)?(ba|z)?sh\b", "내려받은 스크립트를 바로 실행하는 파이프는 금지입니다."),
]


def check(command: str) -> str | None:
    for pattern, message in RULES:
        if re.search(pattern, command):
            return message
    return None


def main() -> int:
    data = json.load(sys.stdin)
    command = (data.get("tool_input") or {}).get("command") or ""
    reason = check(command)
    if reason:
        print(f"[harness] 차단된 명령: {command}\n{reason}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
