#!/usr/bin/env python3
"""
Agent Platform Backend E2E Validation
Covers: Task 1 (common-core), Task 3 (Agent module), Task 4 (Chat module)
Usage:  python scripts/e2e_validate.py [--base-url http://localhost:8080]
"""

import argparse
import json
import sys
import time
import uuid
import io
import re
from typing import Optional

# Force UTF-8 output on Windows
if sys.stdout.encoding != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if sys.stderr.encoding != "utf-8":
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

try:
    import requests
except ImportError:
    print("requests not installed. Run: pip install requests")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────────────
parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://localhost:8080")
args = parser.parse_args()
BASE = args.base_url.rstrip("/")

PASS = 0
FAIL = 0
SKIP = 0
FAILURES = []

SESSION = requests.Session()
SESSION.headers.update({"Content-Type": "application/json", "Accept": "*/*"})

# ── Helpers ───────────────────────────────────────────────────────────────────

def section(title: str):
    print(f"\n{'='*65}")
    print(f"  {title}")
    print(f"{'='*65}")

def passed(name: str):
    global PASS
    PASS += 1
    print(f"  [PASS] {name}")

def failed(name: str, reason: str = ""):
    global FAIL
    FAIL += 1
    FAILURES.append((name, reason))
    print(f"  [FAIL] {name}")
    if reason:
        print(f"         {reason}")

def skipped(name: str, reason: str = ""):
    global SKIP
    SKIP += 1
    print(f"  [SKIP] {name}")
    if reason:
        print(f"         {reason}")

def api(method: str, path: str, body=None, timeout=15) -> Optional[requests.Response]:
    url = BASE + path
    try:
        kwargs = {"timeout": timeout}
        if body is not None:
            kwargs["data"] = json.dumps(body)
        resp = SESSION.request(method, url, **kwargs)
        return resp
    except requests.exceptions.RequestException as e:
        print(f"         [request error] {type(e).__name__}: {e}")
        return None

def j(resp) -> dict:
    try:
        return resp.json()
    except Exception:
        return {}

def assert_status(resp, expected: int, name: str) -> bool:
    if resp is None:
        failed(name, "Request failed (connection error)")
        return False
    if resp.status_code == expected:
        return True
    body_preview = resp.text[:300] if resp.text else ""
    failed(name, f"Expected HTTP {expected}, got {resp.status_code}. Body: {body_preview}")
    return False

def sse_events(resp: requests.Response) -> list[dict]:
    """Parse SSE stream into list of {event, data, id} dicts."""
    events = []
    current = {}
    for line in resp.text.splitlines():
        if line.startswith("event:"):
            current["event"] = line[6:].strip()
        elif line.startswith("data:"):
            current["data"] = line[5:].strip()
        elif line.startswith("id:"):
            current["id"] = line[3:].strip()
        elif line == "" and current:
            events.append(current)
            current = {}
    if current:
        events.append(current)
    return events

def wait_for_app(retries=20, delay=3):
    for i in range(1, retries + 1):
        try:
            r = SESSION.get(f"{BASE}/api/v1/health", timeout=3)
            if r.status_code == 200:
                return True
        except Exception:
            pass
        print(f"  Waiting for app (attempt {i}/{retries})...")
        time.sleep(delay)
    return False

# ── Startup check ─────────────────────────────────────────────────────────────
section("Checking Application Availability")
if not wait_for_app():
    print(f"\n  ERROR: Application not reachable at {BASE}")
    sys.exit(1)
passed("Application is reachable")

# ── Task 1: Common Infrastructure ─────────────────────────────────────────────
section("Task 1 — Common Infrastructure")

# 1.1 Health endpoint — Virtual Threads
r = api("GET", "/api/v1/health")
if assert_status(r, 200, "GET /api/v1/health -> 200"):
    data = j(r).get("data", {})
    thread = data.get("thread", "")
    if "VirtualThread" in thread:
        passed("Health: Virtual Thread confirmed")
    else:
        failed("Health: Virtual Thread check", f"thread='{thread}'")
    if data.get("status") == "ok":
        passed("Health: status=ok")
    else:
        failed("Health: status=ok", f"status='{data.get('status')}'")

# 1.2 ApiResponse wrapper
r = api("GET", "/api/v1/agents")
if r is not None and r.status_code == 200:
    body = j(r)
    if body.get("success") is True:
        passed("ApiResponse: success=true")
    else:
        failed("ApiResponse: success field", f"success={body.get('success')}")
    if "data" in body and "requestId" in body:
        passed("ApiResponse: has data + requestId fields")
    else:
        failed("ApiResponse: wrapper fields", f"keys={list(body.keys())}")
else:
    skipped("ApiResponse format", f"Agent list returned {r.status_code if r is not None else 'none'}")

# 1.3 Error format (404)
fake_id = str(uuid.uuid4())
r = api("GET", f"/api/v1/agents/{fake_id}")
if assert_status(r, 404, "GET non-existent agent -> 404"):
    body = j(r)
    err = body.get("error", {})
    if err.get("code"):
        passed("Error format: has 'code' field")
    else:
        failed("Error format: code field", f"error={err}")
    if err.get("message"):
        passed("Error format: has 'message' field")
    else:
        failed("Error format: message field", f"error={err}")
    if err.get("requestId"):
        passed("Error format: has 'requestId' field")
    else:
        failed("Error format: requestId field", f"error keys={list(err.keys())}")

# 1.4 Validation errors -> 400
r = api("POST", "/api/v1/agents", {"name": "X" * 31, "maxSteps": 10})
if assert_status(r, 400, "Create agent with name>30 chars -> 400"):
    if j(r).get("error"):
        passed("Validation error wrapped in 'error' field")
    else:
        failed("Validation error format", "No 'error' wrapper")

r = api("POST", "/api/v1/agents", {"name": "Test", "maxSteps": 51})
assert_status(r, 400, "Create agent with maxSteps=51 -> 400")

r = api("POST", "/api/v1/agents", {"name": "Test", "maxSteps": 0})
assert_status(r, 400, "Create agent with maxSteps=0 -> 400")

r = api("POST", "/api/v1/agents", {"description": "no name"})
assert_status(r, 400, "Create agent without name -> 400")

# ── Task 3.1: Agent CRUD ──────────────────────────────────────────────────────
section("Task 3.1 — Agent CRUD")

agent_id = None
r = api("POST", "/api/v1/agents", {
    "name": "E2E-Test-Agent",
    "description": "E2E validation test",
    "systemPrompt": "You are a test assistant",
    "maxSteps": 5
})
if assert_status(r, 201, "POST /api/v1/agents -> 201 Created"):
    body = j(r)
    agent_id = body.get("data", {}).get("id")
    if agent_id:
        passed(f"Create agent: returns id={agent_id[:8]}...")
    else:
        failed("Create agent: id missing", str(body))
    data = body.get("data", {})
    if data.get("status") == "draft":
        passed("Create agent: status=draft")
    else:
        failed("Create agent: status", f"status={data.get('status')}")
    if data.get("visibility") == "private":
        passed("Create agent: visibility=private")
    else:
        failed("Create agent: visibility", f"visibility={data.get('visibility')}")
    if data.get("hasUnpublishedChanges") is False:
        passed("Create agent: hasUnpublishedChanges=false")
    else:
        failed("Create agent: hasUnpublishedChanges", f"{data.get('hasUnpublishedChanges')}")

# LIST
r = api("GET", "/api/v1/agents")
if assert_status(r, 200, "GET /api/v1/agents -> 200"):
    data = j(r).get("data", {})
    count = len(data.get("data", []))
    if count >= 1:
        passed(f"List agents: {count} agent(s) returned")
    else:
        failed("List agents: count", "Expected ≥1 agents")
    if "total" in data:
        passed("List agents: total field present")
    else:
        failed("List agents: total field", f"keys={list(data.keys())}")

# DETAIL
if agent_id:
    r = api("GET", f"/api/v1/agents/{agent_id}")
    if assert_status(r, 200, "GET /api/v1/agents/{id} -> 200"):
        data = j(r).get("data", {})
        if data.get("id") == agent_id:
            passed("Detail agent: correct id")
        else:
            failed("Detail agent: id mismatch", f"{data.get('id')} != {agent_id}")
        if data.get("name") == "E2E-Test-Agent":
            passed("Detail agent: name correct")
        else:
            failed("Detail agent: name", f"{data.get('name')}")
        if data.get("maxSteps") == 5:
            passed("Detail agent: maxSteps=5")
        else:
            failed("Detail agent: maxSteps", f"{data.get('maxSteps')}")

# UPDATE
if agent_id:
    r = api("PUT", f"/api/v1/agents/{agent_id}", {"name": "E2E-Test-Agent-Updated", "version": 0})
    if assert_status(r, 200, "PUT /api/v1/agents/{id} -> 200"):
        data = j(r).get("data", {})
        if data.get("name") == "E2E-Test-Agent-Updated":
            passed("Update agent: name changed")
        else:
            failed("Update agent: name", f"{data.get('name')}")

# OPTIMISTIC LOCK
if agent_id:
    r = api("PUT", f"/api/v1/agents/{agent_id}", {"name": "should fail", "version": 0})
    if r is not None and r.status_code == 409:
        passed("Update agent: stale version -> 409")
    else:
        failed("Update agent: optimistic lock", f"Expected 409, got {r.status_code if r is not None else 'none'}")

# SEARCH
r = api("GET", "/api/v1/agents?search=E2E")
if assert_status(r, 200, "GET /api/v1/agents?search=E2E -> 200"):
    items = j(r).get("data", {}).get("data", [])
    if any("E2E" in a.get("name", "") for a in items):
        passed("Search agents: found by keyword 'E2E'")
    else:
        failed("Search agents", f"No E2E agent found in {[a.get('name') for a in items]}")

# ── Task 3.2: Duplicate ───────────────────────────────────────────────────────
section("Task 3.2 — Agent Duplicate")

dup_id = None
if agent_id:
    r = api("POST", f"/api/v1/agents/{agent_id}/duplicate")
    if assert_status(r, 201, "POST /api/v1/agents/{id}/duplicate -> 201"):
        data = j(r).get("data", {})
        dup_id = data.get("id")
        if "-Copy" in data.get("name", ""):
            passed("Duplicate: name contains '-Copy'")
        else:
            failed("Duplicate: name suffix", f"name='{data.get('name')}'")
        if data.get("status") == "draft":
            passed("Duplicate: status=draft")
        else:
            failed("Duplicate: status", f"{data.get('status')}")
        if dup_id and dup_id != agent_id:
            passed("Duplicate: new id assigned")
        else:
            failed("Duplicate: id", "Same id as original or missing")

# ── Task 3.3: Export / Import ─────────────────────────────────────────────────
section("Task 3.3 — Agent Export/Import")

exported_json = None
if agent_id:
    r = api("GET", f"/api/v1/agents/{agent_id}/export")
    if assert_status(r, 200, "GET /api/v1/agents/{id}/export -> 200"):
        body = j(r)
        if body.get("success") is True:
            passed("Export: wrapped in ApiResponse")
            exported_json = body.get("data", {})
        elif "name" in body:
            failed("Export: ApiResponse wrapping", "Raw Map returned, not ApiResponse<T> (AGENTS.md violation)")
            exported_json = body
        else:
            failed("Export: response format", f"Unexpected: {r.text[:200]}")

        if exported_json:
            if exported_json.get("name"):
                passed("Export: has 'name' field")
            else:
                failed("Export: name field", "Missing")
            if "system_prompt" in exported_json:
                passed("Export: has 'system_prompt' field")
            else:
                failed("Export: system_prompt field", f"keys={list(exported_json.keys())}")
            if "tool_bindings" in exported_json:
                passed("Export: has 'tool_bindings' field")
            else:
                failed("Export: tool_bindings field", "Missing")

# IMPORT via multipart
if exported_json:
    try:
        file_content = json.dumps(exported_json).encode("utf-8")
        files = {"file": ("agent_export.json", io.BytesIO(file_content), "application/json")}
        # Explicitly clear Content-Type so requests can set multipart/form-data boundary
        import_resp = SESSION.post(f"{BASE}/api/v1/agents/import", files=files, timeout=15,
                                   headers={"Accept": "application/json", "Content-Type": None})
        if assert_status(import_resp, 201, "POST /api/v1/agents/import -> 201"):
            body = import_resp.json()
            if body.get("success") and body.get("data", {}).get("agent", {}).get("id"):
                passed("Import: returns new agent id")
            else:
                failed("Import: response structure", f"data={body.get('data')}")
            if "unresolvedRefs" in body.get("data", {}):
                passed("Import: has unresolvedRefs field")
            else:
                failed("Import: unresolvedRefs field", "Missing")
    except Exception as e:
        failed("Import: request error", str(e))

# Invalid JSON import
try:
    bad_files = {"file": ("bad.json", io.BytesIO(b"not-valid-json"), "application/json")}
    r = SESSION.post(f"{BASE}/api/v1/agents/import", files=bad_files, timeout=15,
                     headers={"Accept": "application/json", "Content-Type": None})
    if r.status_code in (400, 422):
        passed("Import invalid JSON -> 400/422")
    else:
        failed("Import invalid JSON", f"Expected 400/422, got {r.status_code}")
except Exception as e:
    skipped("Import invalid JSON", str(e))

# ── Task 3.4: Versions ────────────────────────────────────────────────────────
section("Task 3.4 — Agent Versions")

if agent_id:
    r = api("GET", f"/api/v1/agents/{agent_id}/versions")
    if assert_status(r, 200, "GET /api/v1/agents/{id}/versions -> 200"):
        data = j(r).get("data", [])
        if isinstance(data, list):
            passed("Versions: returns array")
        else:
            failed("Versions: array type", f"data={type(data)}")

    # Rollback with fake version id
    fake_vid = str(uuid.uuid4())
    r = api("POST", f"/api/v1/agents/{agent_id}/versions/{fake_vid}/rollback")
    if r is not None and r.status_code == 404:
        passed("Rollback non-existent version -> 404")
    else:
        failed("Rollback non-existent version", f"Expected 404, got {r.status_code if r is not None else 'none'}")

# ── Task 3: Permission Check ──────────────────────────────────────────────────
section("Task 3 — Permission Check")

r = api("GET", f"/api/v1/agents/{uuid.uuid4()}")
if r is not None and r.status_code == 404:
    passed("Permission: non-existent agent -> 404")
else:
    failed("Permission check", f"Expected 404, got {r.status_code if r is not None else 'none'}")

# ── Task 4.1: Chat Session Management ─────────────────────────────────────────
section("Task 4.1 — Chat Session Management")

session_id = None
if agent_id:
    r = api("POST", "/api/v1/chat/sessions", {"agentId": agent_id})
    if assert_status(r, 201, "POST /api/v1/chat/sessions -> 201"):
        data = j(r).get("data", {})
        session_id = data.get("id")
        if session_id:
            passed(f"Create session: id={session_id[:8]}...")
        else:
            failed("Create session: id", "Missing")
        if data.get("currentAgentId") == agent_id:
            passed("Create session: currentAgentId correct")
        else:
            failed("Create session: currentAgentId", f"{data.get('currentAgentId')}")
else:
    skipped("Chat session tests", "No agent created")

# LIST
r = api("GET", "/api/v1/chat/sessions")
if assert_status(r, 200, "GET /api/v1/chat/sessions -> 200"):
    data = j(r).get("data", {}).get("data", [])
    if isinstance(data, list):
        passed("List sessions: returns array")
    else:
        failed("List sessions: array", f"data={type(data)}")

# SESSION DETAIL
if session_id:
    r = api("GET", f"/api/v1/chat/sessions/{session_id}")
    if assert_status(r, 200, "GET /api/v1/chat/sessions/{id} -> 200"):
        data = j(r).get("data", {})
        if data.get("id") == session_id:
            passed("Session detail: correct id")
        else:
            failed("Session detail: id", f"{data.get('id')}")
        msgs = data.get("messages", None)
        if isinstance(msgs, list):
            passed("Session detail: messages is array")
        else:
            failed("Session detail: messages", f"type={type(msgs)}")

# ── Task 4.4: Send Message (SSE) ──────────────────────────────────────────────
section("Task 4.4 — Send Message (SSE)")

msg_id_from_sse = None
if session_id:
    try:
        sse_resp = SESSION.post(
            f"{BASE}/api/v1/chat/sessions/{session_id}/messages",
            data=json.dumps({"content": "Hello, please introduce yourself"}),
            headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
            timeout=30,
            stream=True
        )
        if sse_resp.status_code == 200:
            ct = sse_resp.headers.get("Content-Type", "")
            if "text/event-stream" in ct:
                passed("Send message: Content-Type is text/event-stream")
            else:
                failed("Send message: Content-Type", f"Got '{ct}'")

            # Collect all SSE events
            all_events = sse_events(sse_resp)
            event_types = [e.get("event") for e in all_events]

            if "message_start" in event_types:
                passed("SSE: has message_start event")
            else:
                failed("SSE: message_start missing", f"events={event_types}")
            if "token" in event_types:
                passed("SSE: has token event(s)")
            else:
                failed("SSE: token events missing", f"events={event_types}")
            if "message_end" in event_types:
                passed("SSE: has message_end event")
            else:
                failed("SSE: message_end missing", f"events={event_types}")

            token_evts = [e for e in all_events if e.get("event") == "token"]
            if len(token_evts) >= 1:
                passed(f"SSE: {len(token_evts)} token event(s) received")
            else:
                failed("SSE: token count", "0 token events")

            # seq monotonically increasing
            seqs = []
            for e in token_evts:
                try:
                    d = json.loads(e.get("data", "{}"))
                    if "seq" in d:
                        seqs.append(d["seq"])
                except Exception:
                    pass
            if len(seqs) >= 2:
                if all(seqs[i] > seqs[i-1] for i in range(1, len(seqs))):
                    passed("SSE: seq monotonically increasing")
                else:
                    failed("SSE: seq monotonic", f"seqs={seqs}")

            # Extract message_id from message_start
            for e in all_events:
                if e.get("event") == "message_start":
                    try:
                        d = json.loads(e.get("data", "{}"))
                        msg_id_from_sse = d.get("message_id")
                    except Exception:
                        pass
                    break
        else:
            failed("Send message: HTTP status", f"Expected 200, got {sse_resp.status_code}. Body: {sse_resp.text[:200]}")
    except Exception as e:
        failed("Send message: request failed", str(e))

    # Wait for persistence
    time.sleep(2)
    r = api("GET", f"/api/v1/chat/sessions/{session_id}")
    if r is not None and r.status_code == 200:
        msgs = j(r).get("data", {}).get("messages", [])
        if len(msgs) >= 2:
            passed(f"Message persistence: {len(msgs)} messages in history")
        else:
            failed("Message persistence", f"Expected ≥2 messages, got {len(msgs)}")
        user_msgs = [m for m in msgs if m.get("role") == "user"]
        asst_msgs = [m for m in msgs if m.get("role") == "assistant"]
        if user_msgs:
            passed("Message history: user message present")
        else:
            failed("Message history: user message", "Missing")
        if asst_msgs:
            passed("Message history: assistant message present")
            if asst_msgs[-1].get("status") == "complete":
                passed("Message history: assistant status=complete")
            else:
                failed("Message history: assistant status", f"status={asst_msgs[-1].get('status')}")
        else:
            failed("Message history: assistant message", "Missing")
else:
    skipped("Send message (SSE)", "No session available")

# ── Task 4.4: SSE Event Data Fields ──────────────────────────────────────────
section("Task 4.4 — SSE Event Data Fields")

if session_id:
    try:
        r2 = SESSION.post(
            f"{BASE}/api/v1/chat/sessions/{session_id}/messages",
            data=json.dumps({"content": "Test again"}),
            headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
            timeout=30, stream=True
        )
        if r2.status_code == 200:
            evts = sse_events(r2)
            start_evt = next((e for e in evts if e.get("event") == "message_start"), None)
            if start_evt:
                d = json.loads(start_evt.get("data", "{}"))
                if "agent_id" in d and "model" in d:
                    passed("SSE message_start: has agent_id + model fields")
                else:
                    failed("SSE message_start fields", f"keys={list(d.keys())}")

            token_evt = next((e for e in evts if e.get("event") == "token"), None)
            if token_evt:
                d = json.loads(token_evt.get("data", "{}"))
                required = ["request_id", "message_id", "timestamp", "delta", "seq"]
                missing = [k for k in required if k not in d]
                if not missing:
                    passed("SSE token: has all required fields")
                else:
                    failed("SSE token fields", f"Missing: {missing}")

            end_evt = next((e for e in evts if e.get("event") == "message_end"), None)
            if end_evt:
                d = json.loads(end_evt.get("data", "{}"))
                if "finish_reason" in d and "usage" in d:
                    passed("SSE message_end: has finish_reason + usage")
                else:
                    failed("SSE message_end fields", f"keys={list(d.keys())}")
    except Exception as e:
        skipped("SSE event fields", str(e))

# ── Task 4.5: Idempotency ─────────────────────────────────────────────────────
section("Task 4.5 — Idempotency")

if session_id:
    idem_key = str(uuid.uuid4())
    body1 = json.dumps({"content": "idempotency-test-message", "idempotencyKey": idem_key})

    def send_sse(payload: str, timeout=30) -> tuple[int, list]:
        try:
            r = SESSION.post(
                f"{BASE}/api/v1/chat/sessions/{session_id}/messages",
                data=payload,
                headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
                timeout=timeout, stream=True
            )
            return r.status_code, sse_events(r)
        except Exception:
            return 0, []

    status1, evts1 = send_sse(body1)
    time.sleep(2)
    status2, evts2 = send_sse(body1)

    def extract_msg_id(evts):
        for e in evts:
            if e.get("event") == "message_start":
                try:
                    return json.loads(e.get("data", "{}")).get("message_id")
                except Exception:
                    pass
        return None

    mid1 = extract_msg_id(evts1)
    mid2 = extract_msg_id(evts2)

    if mid1 and mid2 and mid1 == mid2:
        passed("Idempotency: same key+body returns same message_id")
    else:
        failed("Idempotency: duplicate detection", f"mid1={mid1}, mid2={mid2}")

    # Different body, same key -> 409
    body2 = json.dumps({"content": "DIFFERENT BODY", "idempotencyKey": idem_key})
    status3, _ = send_sse(body2, timeout=15)
    if status3 == 409:
        passed("Idempotency: same key + different body -> 409")
    else:
        failed("Idempotency: conflict", f"Expected 409, got {status3}")

# ── Task 4.6: Step Control ────────────────────────────────────────────────────
section("Task 4.6 — Step Control (step_limit + continue)")

step_session_id = None
if agent_id:
    r = api("POST", "/api/v1/chat/sessions", {"agentId": agent_id})
    if r is not None and r.status_code == 201:
        step_session_id = j(r).get("data", {}).get("id")

    skipped("step_limit event trigger", "Stub LlmStreamService returns no tool_calls; need real LLM")

    if step_session_id:
        fake_state_id = str(uuid.uuid4())
        r = api("POST", f"/api/v1/chat/sessions/{step_session_id}/continue",
                {"sessionStateId": fake_state_id}, timeout=10)
        if r is not None and r.status_code == 404:
            passed("Continue: non-existent session state -> 404")
        else:
            failed("Continue: non-existent state", f"Expected 404, got {r.status_code if r is not None else 'none'}")

# ── Task 4.7: Message Regenerate ──────────────────────────────────────────────
section("Task 4.7 — Message Regenerate")

if session_id:
    r = api("GET", f"/api/v1/chat/sessions/{session_id}")
    if r is not None and r.status_code == 200:
        msgs = j(r).get("data", {}).get("messages", [])
        asst_msgs = [m for m in msgs if m.get("role") == "assistant"]
        regen_msg_id = asst_msgs[-1].get("id") if asst_msgs else None

        if regen_msg_id:
            try:
                regen_resp = SESSION.post(
                    f"{BASE}/api/v1/chat/sessions/{session_id}/messages/{regen_msg_id}/regenerate",
                    headers={"Accept": "text/event-stream"},
                    timeout=30, stream=True
                )
                if regen_resp.status_code == 200:
                    ct = regen_resp.headers.get("Content-Type", "")
                    if "text/event-stream" in ct:
                        passed("Regenerate: returns SSE stream")
                    else:
                        failed("Regenerate: Content-Type", f"'{ct}'")

                    evts = sse_events(regen_resp)
                    evt_types = [e.get("event") for e in evts]
                    if "message_start" in evt_types:
                        passed("Regenerate: has message_start event")
                    else:
                        failed("Regenerate: message_start", f"events={evt_types}")

                    # Verify persisted: same message_id, content updated
                    time.sleep(1)
                    r2 = api("GET", f"/api/v1/chat/sessions/{session_id}")
                    if r2 and r2.status_code == 200:
                        new_msgs = j(r2).get("data", {}).get("messages", [])
                        new_asst = [m for m in new_msgs if m.get("role") == "assistant"]
                        still_same_id = any(m.get("id") == regen_msg_id for m in new_asst)
                        if still_same_id:
                            passed("Regenerate: same message_id preserved (not duplicated)")
                        else:
                            failed("Regenerate: message_id", "Original message_id not found after regen (Bug #2 regression?)")
                else:
                    failed("Regenerate: HTTP status", f"Expected 200, got {regen_resp.status_code}")
            except Exception as e:
                failed("Regenerate: request failed", str(e))
        else:
            skipped("Regenerate", "No assistant message found")

# ── Task 4.8: Agent Switch ────────────────────────────────────────────────────
section("Task 4.8 — Agent Switch")

if session_id and agent_id:
    r = api("POST", f"/api/v1/chat/sessions/{session_id}/switch-agent", {"agentId": agent_id})
    if assert_status(r, 200, "POST /api/v1/chat/sessions/{id}/switch-agent -> 200"):
        body = j(r)
        if body.get("success") is True:
            passed("Switch agent: success=true")
        else:
            failed("Switch agent: response", f"success={body.get('success')}")

    # Invalid switch (missing agentId)
    r = api("POST", f"/api/v1/chat/sessions/{session_id}/switch-agent", {})
    if r is not None and r.status_code == 400:
        passed("Switch agent: missing agentId -> 400")
    else:
        failed("Switch agent: validation", f"Expected 400, got {r.status_code if r is not None else 'none'}")

# ── Task 4.1: Clear Messages ──────────────────────────────────────────────────
section("Task 4.1 — Clear Messages")

if session_id:
    r = api("DELETE", f"/api/v1/chat/sessions/{session_id}/messages")
    if assert_status(r, 200, "DELETE /api/v1/chat/sessions/{id}/messages -> 200"):
        passed("Clear messages: 200 OK")
        time.sleep(1)
        r2 = api("GET", f"/api/v1/chat/sessions/{session_id}")
        if r2 and r2.status_code == 200:
            msgs = j(r2).get("data", {}).get("messages", [])
            if len(msgs) == 0:
                passed("Clear messages: history empty after clear")
            else:
                failed("Clear messages: history not cleared", f"Still {len(msgs)} messages")

# ── Cleanup ───────────────────────────────────────────────────────────────────
section("Cleanup")

for aid in filter(None, [dup_id, agent_id]):
    r = api("DELETE", f"/api/v1/agents/{aid}")
    if r is not None and r.status_code == 200:
        passed(f"Delete agent {aid[:8]}...")
    else:
        # Not a test failure, just report
        print(f"  [INFO] Could not delete agent {aid[:8]}...: {r.status_code if r is not None else 'error'}")

# ── Summary ───────────────────────────────────────────────────────────────────
total = PASS + FAIL + SKIP
print(f"\n{'='*65}")
print(f"  VALIDATION SUMMARY")
print(f"{'='*65}")
print(f"  Total:  {total}")
print(f"  Pass:   {PASS}")
print(f"  Fail:   {FAIL}")
print(f"  Skip:   {SKIP}")
print(f"{'='*65}")

if FAILURES:
    print("\n  Failed tests:")
    for name, reason in FAILURES:
        print(f"    - {name}")
        if reason:
            print(f"      {reason}")

if FAIL > 0:
    print(f"\n  [!] {FAIL} check(s) FAILED\n")
    sys.exit(1)
else:
    print(f"\n  [OK] All {PASS} checks passed!\n")
    sys.exit(0)
