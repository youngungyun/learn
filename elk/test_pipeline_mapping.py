#!/usr/bin/env python3
"""Logstash 파이프라인의 필드 이동(rename)이 앱의 ECS 로그 모양과 맞는지 확인한다.

Logstash 를 띄우지 않고도 "Filebeat 가 펼친 구조에 rename 을 적용하면 기대한 필드가 생기는가"를 검증한다.
실행: python3 -m unittest elk/test_pipeline_mapping.py -v
"""
import json
import re
import unittest
from pathlib import Path

ELK = Path(__file__).resolve().parent
RENAME = re.compile(r'"((?:\[[^\]]+\])+)"\s*=>\s*"((?:\[[^\]]+\])+|[A-Za-z_]+)"')


def parts(ref: str) -> list[str]:
    return re.findall(r"\[([^\]]+)\]", ref) or [ref]


def expand_keys(flat: dict) -> dict:
    """Filebeat ndjson 의 expand_keys: true — 'log.level' 같은 키를 {log: {level}} 로 펼친다."""
    result: dict = {}
    for key, value in flat.items():
        node = result
        names = key.split(".") if key != "@timestamp" else [key]
        for name in names[:-1]:
            node = node.setdefault(name, {})
        node[names[-1]] = value
    return result


def get(event: dict, ref: str):
    node = event
    for name in parts(ref):
        if not isinstance(node, dict) or name not in node:
            return None
        node = node[name]
    return node


def set_(event: dict, ref: str, value) -> None:
    node = event
    names = parts(ref)
    for name in names[:-1]:
        node = node.setdefault(name, {})
    node[names[-1]] = value


def remove(event: dict, ref: str) -> None:
    names = parts(ref)
    node = event
    for name in names[:-1]:
        node = node.get(name, {})
    node.pop(names[-1], None)


def apply_renames(event: dict, conf: str) -> dict:
    for source, target in RENAME.findall(conf):
        value = get(event, source)
        if value is not None:
            remove(event, source)
            set_(event, target, value)
    return event


class PipelineMappingTest(unittest.TestCase):
    conf = (ELK / "logstash" / "pipeline" / "logstash.conf").read_text(encoding="utf-8")
    lines = [json.loads(line) for line in (ELK / "sample-ecs.ndjson").read_text(encoding="utf-8").splitlines()]

    def shipped(self, line: dict) -> dict:
        # Filebeat 는 파싱한 앱 JSON 을 target: "app" 아래에 두고, 자기 메타데이터(log.file.path)는 따로 붙인다.
        return {"app": expand_keys(line), "log": {"file": {"path": "/logs/meetlab.log"}}}

    def test_rename_list_is_parsed(self):
        self.assertGreaterEqual(len(RENAME.findall(self.conf)), 12)

    def test_info_line_fields_land_in_ecs_locations(self):
        event = apply_renames(self.shipped(self.lines[0]), self.conf)
        self.assertEqual(get(event, "[message]"), "입장")
        self.assertEqual(get(event, "[log][level]"), "INFO")
        self.assertEqual(get(event, "[log][logger]"), "com.example.meetlab.signaling.SignalingService")
        self.assertEqual(get(event, "[service][name]"), "meetlab")
        self.assertEqual(get(event, "[process][thread][name]"), "http-nio-8080-exec-3")
        self.assertEqual(get(event, "[request_id]"), "4f0c2f40-6c52-4d0e-9a52-6a0f0c1a2b3c")
        self.assertEqual(get(event, "[room]"), "abc-defg-hij")
        self.assertEqual(get(event, "[event_name]"), "peer_joined")

    def test_filebeat_file_path_survives_next_to_app_log_level(self):
        event = apply_renames(self.shipped(self.lines[0]), self.conf)
        self.assertEqual(get(event, "[log][file][path]"), "/logs/meetlab.log")
        self.assertEqual(get(event, "[log][level]"), "INFO")

    def test_error_line_keeps_structured_error_fields(self):
        event = apply_renames(self.shipped(self.lines[1]), self.conf)
        self.assertEqual(get(event, "[log][level]"), "ERROR")
        self.assertEqual(get(event, "[error][type]"), "java.lang.IllegalStateException")
        self.assertEqual(get(event, "[error][message]"), "boom")
        self.assertIn("IllegalStateException", get(event, "[error][stack_trace]"))

    def test_pipeline_has_expected_sections(self):
        for token in ("beats", "5044", "elasticsearch", "meetlab-logs-%{+YYYY.MM.dd}", "date {"):
            self.assertIn(token, self.conf)
        self.assertEqual(self.conf.count("{"), self.conf.count("}"))


if __name__ == "__main__":
    unittest.main()
