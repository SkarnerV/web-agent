#!/usr/bin/env pwsh
# =====================================================================
# Agent Platform Backend E2E Validation Script
# Covers: Task 1 (common-core), Task 3 (Agent module), Task 4 (Chat module)
# Usage:
#   1. Start infra:  docker compose -f docker/docker-compose.infra.yml up -d
#   2. Start app:    ./mvnw spring-boot:run -pl ap-app -Dspring-boot.run.profiles=local
#   3. Run script:   pwsh scripts/e2e-validate.ps1
# =====================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$SseWaitSeconds = 5
)

$ErrorActionPreference = "Continue"
$PassCount = 0
$FailCount = 0
$SkipCount = 0

# ── Helpers ─────────────────────────────────────────────────────────

function Write-Section($title) {
    Write-Host "`n" -NoNewline
    Write-Host ("=" * 65) -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host ("=" * 65) -ForegroundColor Cyan
}

function Pass($name) {
    $script:PassCount++
    Write-Host "  [PASS] $name" -ForegroundColor Green
}

function Fail($name, $reason) {
    $script:FailCount++
    Write-Host "  [FAIL] $name" -ForegroundColor Red
    Write-Host "         → $reason" -ForegroundColor DarkRed
}

function Skip($name, $reason) {
    $script:SkipCount++
    Write-Host "  [SKIP] $name" -ForegroundColor Yellow
    Write-Host "         → $reason" -ForegroundColor DarkYellow
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Body = $null,
        [int]$ExpectedStatus = 200,
        [string]$ContentType = "application/json"
    )
    $url = "$BaseUrl$Path"
    $params = @{
        Method  = $Method
        Uri     = $url
        Headers = @{ "Content-Type" = $ContentType; "Accept" = "application/json" }
        ErrorAction = "Stop"
    }
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    try {
        $response = Invoke-WebRequest @params -UseBasicParsing
        return $response
    } catch [System.Net.WebException] {
        $errResp = $_.Exception.Response
        if ($null -ne $errResp) {
            $errStream = $errResp.GetResponseStream()
            $errReader = New-Object System.IO.StreamReader($errStream)
            $errContent = $errReader.ReadToEnd()
            return [PSCustomObject]@{
                StatusCode = [int]$errResp.StatusCode
                Content    = $errContent
                Headers    = $errResp.Headers
            }
        }
        return $null
    } catch {
        return $null
    }
}

function Invoke-WebSafe {
    param([hashtable]$Params)
    try {
        return Invoke-WebRequest @Params -UseBasicParsing
    } catch [System.Net.WebException] {
        $errResp = $_.Exception.Response
        if ($null -ne $errResp) {
            $errStream = $errResp.GetResponseStream()
            $errReader = New-Object System.IO.StreamReader($errStream)
            $errContent = $errReader.ReadToEnd()
            return [PSCustomObject]@{
                StatusCode = [int]$errResp.StatusCode
                Content    = $errContent
                Headers    = $errResp.Headers
            }
        }
        return $null
    } catch {
        return $null
    }
}

function Assert-Status($response, $expected, $testName) {
    if ($null -eq $response) {
        Fail $testName "HTTP request failed (connection error)"
        return $false
    }
    if ($response.StatusCode -eq $expected) {
        return $true
    } else {
        Fail $testName "Expected HTTP $expected, got $($response.StatusCode)`n         Body: $($response.Content.Substring(0, [Math]::Min(300, $response.Content.Length)))"
        return $false
    }
}

function Assert-JsonField($json, $field, $testName) {
    if ($json.PSObject.Properties.Name -contains $field) {
        return $true
    }
    $keys = ($json.PSObject.Properties.Name) -join ", "
    Fail $testName "Field '$field' missing in response. Got keys: $keys"
    return $false
}

function Get-Json($response) {
    try { return $response.Content | ConvertFrom-Json } catch { return $null }
}

# ── Wait for application startup ─────────────────────────────────────

Write-Section "Checking Application Availability"
$maxRetries = 20
$ready = $false
for ($i = 1; $i -le $maxRetries; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/v1/health" -ErrorAction Stop -TimeoutSec 3 -UseBasicParsing
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch {}
    Write-Host "  Waiting for app (attempt $i/$maxRetries)..." -ForegroundColor DarkYellow
    Start-Sleep -Seconds 3
}
if (-not $ready) {
    Write-Host "`n  ERROR: Application not reachable at $BaseUrl. Start with:" -ForegroundColor Red
    Write-Host "  ./mvnw spring-boot:run -pl ap-app -Dspring-boot.run.profiles=local" -ForegroundColor Yellow
    exit 1
}
Pass "Application is reachable"

# ═══════════════════════════════════════════════════════════════════
# SECTION 1: Task 1 — Common infrastructure
# ═══════════════════════════════════════════════════════════════════
Write-Section "Task 1 — Common Infrastructure"

# 1.1 Health endpoint (Virtual Threads)
$r = Invoke-Api -Path "/api/v1/health"
if (Assert-Status $r 200 "GET /api/v1/health returns 200") {
    $j = Get-Json $r
    if ($j.thread -match "VirtualThread") {
        Pass "Health: thread field confirms Virtual Thread"
    } else {
        Fail "Health: Virtual Thread check" "thread='$($j.thread)' does not contain 'VirtualThread'"
    }
    if ($j.status -eq "ok") { Pass "Health: status=ok" } else { Fail "Health: status=ok" "status='$($j.status)'" }
}

# 1.2 ApiResponse wrapper format
$r = Invoke-Api -Path "/api/v1/agents" -Method "GET"
if ($null -ne $r -and $r.StatusCode -eq 200) {
    $j = Get-Json $r
    if ((Assert-JsonField $j "success" "ApiResponse has 'success' field") -and
        (Assert-JsonField $j "data" "ApiResponse has 'data' field") -and
        (Assert-JsonField $j "requestId" "ApiResponse has 'requestId' field")) {
        if ($j.success -eq $true) { Pass "ApiResponse: success=true" } else { Fail "ApiResponse: success=true" "success=$($j.success)" }
    }
} else {
    Skip "ApiResponse format" "Agent list returned $($r?.StatusCode)"
}

# 1.3 Error format (404 for non-existent resource)
$fakeId = [System.Guid]::NewGuid()
$r = Invoke-Api -Path "/api/v1/agents/$fakeId" -Method "GET"
if (Assert-Status $r 404 "GET non-existent agent → 404") {
    $j = Get-Json $r
    if ($null -ne $j.error) {
        if (Assert-JsonField $j.error "code" "Error body has 'code'") { Pass "Error format: code field" }
        if (Assert-JsonField $j.error "message" "Error body has 'message'") { Pass "Error format: message field" }
        if (Assert-JsonField $j.error "request_id" "Error body has 'request_id'") { Pass "Error format: request_id field" }
    } else {
        Fail "Error format" "Response body has no 'error' wrapper. Body: $($r.Content.Substring(0, 200))"
    }
}

# 1.4 Validation errors return 400
$r = Invoke-Api -Method "POST" -Path "/api/v1/agents" -Body @{
    name = ("X" * 31)   # exceeds 30 char limit
    maxSteps = 10
}
if (Assert-Status $r 400 "Create agent with name>30 chars → 400") {
    $j = Get-Json $r
    if ($null -ne $j.error) {
        Pass "Validation error wrapped in 'error' field"
    } else {
        Fail "Validation error format" "No 'error' wrapper. Body: $($r.Content.Substring(0, 200))"
    }
}

$r = Invoke-Api -Method "POST" -Path "/api/v1/agents" -Body @{ name = "Test"; maxSteps = 51 }
Assert-Status $r 400 "Create agent with maxSteps=51 → 400" | Out-Null

$r = Invoke-Api -Method "POST" -Path "/api/v1/agents" -Body @{ name = "Test"; maxSteps = 0 }
Assert-Status $r 400 "Create agent with maxSteps=0 → 400" | Out-Null

$r = Invoke-Api -Method "POST" -Path "/api/v1/agents" -Body @{ description = "no name" }
Assert-Status $r 400 "Create agent without name → 400" | Out-Null

# ═══════════════════════════════════════════════════════════════════
# SECTION 2: Task 3 — Agent Module
# ═══════════════════════════════════════════════════════════════════
Write-Section "Task 3.1 — Agent CRUD"

# CREATE
$r = Invoke-Api -Method "POST" -Path "/api/v1/agents" -Body @{
    name        = "E2E-Test-Agent"
    description = "E2E validation test"
    systemPrompt = "You are a test assistant"
    maxSteps    = 5
} -ExpectedStatus 201
$agentId = $null
if (Assert-Status $r 201 "POST /api/v1/agents → 201 Created") {
    $j = Get-Json $r
    if ($j.success -and $j.data.id) {
        $agentId = $j.data.id
        Pass "Create agent: returns agent with id=$agentId"
        if ($j.data.status -eq "draft")   { Pass "Create agent: status=draft" } else { Fail "Create agent: status=draft" "status=$($j.data.status)" }
        if ($j.data.visibility -eq "private") { Pass "Create agent: visibility=private" } else { Fail "Create agent: visibility=private" "visibility=$($j.data.visibility)" }
        if ($j.data.hasUnpublishedChanges -eq $false) { Pass "Create agent: hasUnpublishedChanges=false" } else { Fail "Create agent: hasUnpublishedChanges" "value=$($j.data.hasUnpublishedChanges)" }
    } else {
        Fail "Create agent: response data structure" "success=$($j.success), data.id missing"
    }
}

# LIST
$r = Invoke-Api -Path "/api/v1/agents"
if (Assert-Status $r 200 "GET /api/v1/agents → 200") {
    $j = Get-Json $r
    if ($j.data.data.Count -ge 1) { Pass "List agents: at least 1 agent returned" } else { Fail "List agents: count" "Expected ≥1 agents, got $($j.data.data.Count)" }
    if ($null -ne $j.data.total)   { Pass "List agents: total field present" } else { Fail "List agents: total field" "Missing 'total' in response" }
}

# DETAIL
if ($agentId) {
    $r = Invoke-Api -Path "/api/v1/agents/$agentId"
    if (Assert-Status $r 200 "GET /api/v1/agents/{id} → 200") {
        $j = Get-Json $r
        if ($j.data.id -eq $agentId)         { Pass "Detail agent: correct id" } else { Fail "Detail agent: id mismatch" "$($j.data.id) != $agentId" }
        if ($j.data.name -eq "E2E-Test-Agent") { Pass "Detail agent: name correct" } else { Fail "Detail agent: name" "$($j.data.name)" }
        if ($j.data.maxSteps -eq 5)          { Pass "Detail agent: maxSteps=5" } else { Fail "Detail agent: maxSteps" "$($j.data.maxSteps)" }
    }
}

# UPDATE
if ($agentId) {
    $r = Invoke-Api -Method "PUT" -Path "/api/v1/agents/$agentId" -Body @{
        name    = "E2E-Test-Agent-Updated"
        version = 0
    }
    if (Assert-Status $r 200 "PUT /api/v1/agents/{id} → 200") {
        $j = Get-Json $r
        if ($j.data.name -eq "E2E-Test-Agent-Updated") { Pass "Update agent: name changed" } else { Fail "Update agent: name" "$($j.data.name)" }
    }
}

# OPTIMISTIC LOCK
if ($agentId) {
    $r = Invoke-Api -Method "PUT" -Path "/api/v1/agents/$agentId" -Body @{
        name    = "should fail"
        version = 0  # stale version
    }
    if ($r.StatusCode -eq 409) {
        Pass "Update agent: stale version → 409 ASSET_OPTIMISTIC_LOCK"
    } else {
        Fail "Update agent: optimistic lock" "Expected 409, got $($r.StatusCode)"
    }
}

# SEARCH
$r = Invoke-Api -Path "/api/v1/agents?search=E2E"
if (Assert-Status $r 200 "GET /api/v1/agents?search=E2E") {
    $j = Get-Json $r
    $found = $j.data.data | Where-Object { $_.name -like "*E2E*" }
    if ($found.Count -ge 1) { Pass "Search agents: found agent by keyword" } else { Fail "Search agents" "No agent with 'E2E' in name found" }
}

Write-Section "Task 3.2 — Agent Duplicate"

$dupId = $null
if ($agentId) {
    $r = Invoke-Api -Method "POST" -Path "/api/v1/agents/$agentId/duplicate" -ExpectedStatus 201
    if (Assert-Status $r 201 "POST /api/v1/agents/{id}/duplicate → 201") {
        $j = Get-Json $r
        $dupId = $j.data.id
        if ($j.data.name -like "*-Copy*") { Pass "Duplicate: name contains '-Copy'" } else { Fail "Duplicate: name suffix" "name='$($j.data.name)'" }
        if ($j.data.status -eq "draft")   { Pass "Duplicate: status=draft" } else { Fail "Duplicate: status=draft" "status=$($j.data.status)" }
        if ($j.data.id -ne $agentId)      { Pass "Duplicate: new id assigned" } else { Fail "Duplicate: id" "Duplicate has same id as original!" }
    }
}

Write-Section "Task 3.3 — Agent Export/Import"

$exportedJson = $null
if ($agentId) {
    # EXPORT — BUG CHECK: should be ApiResponse wrapped or raw JSON?
    $r = Invoke-Api -Path "/api/v1/agents/$agentId/export"
    if (Assert-Status $r 200 "GET /api/v1/agents/{id}/export → 200") {
        $j = Get-Json $r
        # Check if it's wrapped in ApiResponse or raw
        if ($j.PSObject.Properties.Name -contains "success") {
            Pass "Export: wrapped in ApiResponse"
            $exportedJson = $j.data
        } elseif ($j.PSObject.Properties.Name -contains "name") {
            # BUG: raw JSON returned, not ApiResponse
            Fail "Export: ApiResponse wrapping" "Export returns raw Map, not ApiResponse<T> (AGENTS.md violation)"
            $exportedJson = $j
        } else {
            Fail "Export: response format" "Unexpected structure: $($r.Content.Substring(0, 200))"
        }

        if ($null -ne $exportedJson) {
            if ($exportedJson.name)        { Pass "Export: has 'name' field" } else { Fail "Export: name field" "Missing" }
            if ($exportedJson.system_prompt -ne $null) { Pass "Export: has 'system_prompt' field" } else { Fail "Export: system_prompt" "Missing" }
            if ($null -ne $exportedJson.tool_bindings) { Pass "Export: has 'tool_bindings' array" } else { Fail "Export: tool_bindings" "Missing" }
        }
    }
}

# IMPORT
if ($exportedJson) {
    # Write export data to a temp file for multipart upload
    $tmpFile = [System.IO.Path]::GetTempFileName() + ".json"
    $exportedJson | ConvertTo-Json -Depth 10 | Set-Content $tmpFile -Encoding UTF8

    try {
        $boundary = [System.Guid]::NewGuid().ToString()
        $fileBytes = [System.IO.File]::ReadAllBytes($tmpFile)
        $fileName = "agent_export.json"
        $contentType = "multipart/form-data; boundary=$boundary"

        $body = [System.Text.StringBuilder]::new()
        $body.Append("--$boundary`r`n") | Out-Null
        $body.Append("Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"`r`n") | Out-Null
        $body.Append("Content-Type: application/json`r`n`r`n") | Out-Null
        $utf8 = [System.Text.Encoding]::UTF8.GetString($fileBytes)
        $body.Append($utf8) | Out-Null
        $body.Append("`r`n--$boundary--`r`n") | Out-Null

        $r = Invoke-WebSafe @{
            Uri         = "$BaseUrl/api/v1/agents/import"
            Method      = "POST"
            Body        = $body.ToString()
            ContentType = $contentType
        }
        if (Assert-Status $r 201 "POST /api/v1/agents/import → 201") {
            $j = Get-Json $r
            if ($j.success -and $j.data.agent.id) {
                Pass "Import: returns new agent id"
                if ($null -ne $j.data.unresolvedRefs) { Pass "Import: has unresolvedRefs field" } else { Fail "Import: unresolvedRefs" "Field missing" }
            } else {
                Fail "Import: response structure" "success=$($j.success)"
            }
        }
    } finally {
        Remove-Item $tmpFile -ErrorAction SilentlyContinue
    }
}

# Invalid JSON import — multipart boundary construction complex in PS5, skip
Skip "Import invalid JSON → 400" "Multipart boundary construction in PS is complex; covered by unit tests"

Write-Section "Task 3.4 — Agent Versions"

# Version list (should be empty for new agent, will populate after market publish in tasks 5+)
if ($agentId) {
    $r = Invoke-Api -Path "/api/v1/agents/$agentId/versions"
    if (Assert-Status $r 200 "GET /api/v1/agents/{id}/versions → 200") {
        $j = Get-Json $r
        if ($j.data -is [Array]) { Pass "Versions: returns array" } else { Fail "Versions: array type" "data is not array: $($j.data.GetType().Name)" }
    }
}

# Rollback with non-existent versionId
if ($agentId) {
    $fakeVid = [System.Guid]::NewGuid()
    $r = Invoke-Api -Method "POST" -Path "/api/v1/agents/$agentId/versions/$fakeVid/rollback"
    if ($r.StatusCode -eq 404) {
        Pass "Rollback non-existent version → 404"
    } else {
        Fail "Rollback non-existent version" "Expected 404, got $($r.StatusCode)"
    }
}

Write-Section "Task 3 — Permission Check"

# Non-owner access (stub user is fixed, so we test by accessing non-existent / deleted)
$r = Invoke-Api -Path "/api/v1/agents/$([System.Guid]::NewGuid())"
if ($r.StatusCode -eq 404) {
    Pass "Permission: non-existent agent → 404 (not 403 or 500)"
} else {
    Fail "Permission check" "Expected 404, got $($r.StatusCode)"
}

# ═══════════════════════════════════════════════════════════════════
# SECTION 3: Task 4 — Chat Module
# ═══════════════════════════════════════════════════════════════════
Write-Section "Task 4.1 — Chat Session Management"

$sessionId = $null
if ($agentId) {
    # CREATE SESSION
    $r = Invoke-Api -Method "POST" -Path "/api/v1/chat/sessions" -Body @{ agentId = $agentId } -ExpectedStatus 201
    if (Assert-Status $r 201 "POST /api/v1/chat/sessions → 201") {
        $j = Get-Json $r
        $sessionId = $j.data.id
        if ($sessionId)                         { Pass "Create session: returns session id" } else { Fail "Create session: id" "No id in response" }
        if ($j.data.currentAgentId -eq $agentId) { Pass "Create session: currentAgentId correct" } else { Fail "Create session: currentAgentId" "$($j.data.currentAgentId) != $agentId" }
    }
} else {
    Skip "Chat session tests" "No agent created in Task 3 tests"
}

# LIST SESSIONS
$r = Invoke-Api -Path "/api/v1/chat/sessions"
if (Assert-Status $r 200 "GET /api/v1/chat/sessions → 200") {
    $j = Get-Json $r
    if ($j.data.data -is [Array]) { Pass "List sessions: returns array" } else { Fail "List sessions: array" "data.data is not array" }
}

# SESSION DETAIL
if ($sessionId) {
    $r = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
    if (Assert-Status $r 200 "GET /api/v1/chat/sessions/{id} → 200") {
        $j = Get-Json $r
        if ($j.data.id -eq $sessionId)     { Pass "Session detail: correct id" } else { Fail "Session detail: id" "$($j.data.id)" }
        if ($null -ne $j.data.messages)    { Pass "Session detail: messages field present" } else { Fail "Session detail: messages" "Missing" }
        if ($j.data.messages -is [Array])  { Pass "Session detail: messages is array" } else { Fail "Session detail: messages type" "Not array" }
    }
}

Write-Section "Task 4.4 — Send Message (SSE)"

$msgCount = 0
if ($sessionId) {
    # Send SSE message — note: cannot easily parse SSE in PowerShell, verify HTTP 200 + content-type
    $sendParams = @{
        Method      = "POST"
        Uri         = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages"
        Headers     = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
        Body        = '{"content":"Hello, please introduce yourself"}'
        TimeoutSec  = 30
    }
    try {
        $response = Invoke-WebSafe $sendParams
        if ($response.StatusCode -eq 200) {
            $contentType = $response.Headers["Content-Type"]
            if ($contentType -like "*text/event-stream*") {
                Pass "Send message: Content-Type is text/event-stream"
            } else {
                Fail "Send message: Content-Type" "Expected text/event-stream, got '$contentType'"
            }

            # Parse SSE events
            $sseContent = $response.Content
            $events = $sseContent -split "(\r?\n){2,}" | Where-Object { $_ -match "event:" }
            $eventTypes = @()
            foreach ($evt in $events) {
                if ($evt -match "event:\s*(\w+)") { $eventTypes += $Matches[1] }
            }

            if ("message_start" -in $eventTypes) { Pass "SSE: has message_start event" } else { Fail "SSE: message_start" "Not found in events: $($eventTypes -join ',')" }
            if ("token" -in $eventTypes)         { Pass "SSE: has token event(s)" } else { Fail "SSE: token events" "Not found" }
            if ("message_end" -in $eventTypes)   { Pass "SSE: has message_end event" } else { Fail "SSE: message_end" "Not found in events: $($eventTypes -join ',')" }

            $tokenEvents = $events | Where-Object { $_ -match "event:\s*token" }
            if ($tokenEvents.Count -ge 1)        { Pass "SSE: at least 1 token event" } else { Fail "SSE: token count" "Got $($tokenEvents.Count)" }

            # Check seq field monotonically increases
            $seqs = @()
            foreach ($evt in $tokenEvents) {
                if ($evt -match '"seq"\s*:\s*(\d+)') { $seqs += [int]$Matches[1] }
            }
            if ($seqs.Count -ge 2) {
                $monotonic = $true
                for ($i = 1; $i -lt $seqs.Count; $i++) {
                    if ($seqs[$i] -le $seqs[$i-1]) { $monotonic = $false; break }
                }
                if ($monotonic) { Pass "SSE: token seq is monotonically increasing" } else { Fail "SSE: seq monotonic" "Seqs: $($seqs -join ',')" }
            }

            # Wait for message to be persisted
            Start-Sleep -Seconds 2

            # Verify message was persisted
            $detailR = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
            if ($detailR.StatusCode -eq 200) {
                $detailJ = Get-Json $detailR
                $msgCount = $detailJ.data.messages.Count
                if ($msgCount -ge 2) {
                    Pass "Send message: at least 2 messages in history (user + assistant)"
                    $userMsg = $detailJ.data.messages | Where-Object { $_.role -eq "user" } | Select-Object -First 1
                    $asstMsg = $detailJ.data.messages | Where-Object { $_.role -eq "assistant" } | Select-Object -First 1
                    if ($userMsg)                              { Pass "Message history: user message present" } else { Fail "Message history: user message" "Missing" }
                    if ($asstMsg)                              { Pass "Message history: assistant message present" } else { Fail "Message history: assistant message" "Missing" }
                    if ($asstMsg -and $asstMsg.status -eq "complete") { Pass "Message history: assistant status=complete" } else { Fail "Message history: assistant status" "status=$($asstMsg?.status)" }
                } else {
                    Fail "Message persistence" "Expected ≥2 messages, got $msgCount"
                }
            }
        } else {
            Fail "Send message: HTTP status" "Expected 200, got $($response.StatusCode). Body: $($response.Content.Substring(0, 200))"
        }
    } catch {
        Fail "Send message: request failed" $_.Exception.Message
    }
} else {
    Skip "Send message (SSE)" "No session available"
}

Write-Section "Task 4.4 — SSE Event Fields Validation"

# Validate SSE event data fields
if ($sessionId) {
    try {
        $r2 = Invoke-WebSafe @{
            Uri        = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages"
            Method     = "POST"
            Headers    = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
            Body       = '{"content":"Test again"}'
            TimeoutSec = 30
        }
        if ($r2.StatusCode -eq 200) {
            $events = $r2.Content -split "(\r?\n){2,}" | Where-Object { $_ -match "event:" }

            # Check message_start data fields
            $startEvt = $events | Where-Object { $_ -match "event:\s*message_start" } | Select-Object -First 1
            if ($startEvt -and ($startEvt -match '"agent_id"') -and ($startEvt -match '"model"')) {
                Pass "SSE message_start: has agent_id and model fields"
            } else {
                Fail "SSE message_start fields" "agent_id or model missing in: $startEvt"
            }

            # Check token data fields
            $tokenEvt = $events | Where-Object { $_ -match "event:\s*token" } | Select-Object -First 1
            if ($tokenEvt) {
                $hasRequestId = $tokenEvt -match '"request_id"'
                $hasMessageId = $tokenEvt -match '"message_id"'
                $hasTimestamp = $tokenEvt -match '"timestamp"'
                $hasDelta     = $tokenEvt -match '"delta"'
                $hasSeq       = $tokenEvt -match '"seq"'
                if ($hasRequestId -and $hasMessageId -and $hasTimestamp -and $hasDelta -and $hasSeq) {
                    Pass "SSE token: has all required fields (request_id, message_id, timestamp, delta, seq)"
                } else {
                    Fail "SSE token fields" "Missing: request_id=$hasRequestId message_id=$hasMessageId timestamp=$hasTimestamp delta=$hasDelta seq=$hasSeq"
                }
            }

            # Check message_end data fields
            $endEvt = $events | Where-Object { $_ -match "event:\s*message_end" } | Select-Object -First 1
            if ($endEvt) {
                $hasFinish = $endEvt -match '"finish_reason"'
                $hasUsage  = $endEvt -match '"usage"'
                if ($hasFinish -and $hasUsage) {
                    Pass "SSE message_end: has finish_reason and usage fields"
                } else {
                    Fail "SSE message_end fields" "finish_reason=$hasFinish usage=$hasUsage"
                }
            }
        }
    } catch {
        Skip "SSE event fields validation" "Request failed: $($_.Exception.Message)"
    }
}

Write-Section "Task 4.5 — Tool Dispatch"

# Verify idempotency integration (BUG CHECK)
if ($sessionId) {
    $idemKey = [System.Guid]::NewGuid().ToString()
    $body1 = "{`"content`":`"idempotency-test-message`",`"idempotencyKey`":`"$idemKey`"}"
    
    # First send
    $r1 = Invoke-WebSafe @{
        Uri       = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages"
        Method    = "POST"
        Headers   = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
        Body      = $body1
        TimeoutSec = 30
    }
    
    Start-Sleep -Seconds 2

    # Second send (same key, same body) — should return same message_id
    $r2 = Invoke-WebSafe @{
        Uri       = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages"
        Method    = "POST"
        Headers   = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
        Body      = $body1
        TimeoutSec = 30
    }

    # Extract message_ids from both SSE streams
    $getId1 = { param($content) if ($content -match '"message_id"\s*:\s*"([^"]+)"') { $Matches[1] } else { $null } }
    $mid1 = & $getId1 $r1.Content
    $mid2 = & $getId1 $r2.Content

    if ($mid1 -and $mid2 -and ($mid1 -eq $mid2)) {
        Pass "Idempotency: same key+body returns same message_id"
    } else {
        Fail "Idempotency: duplicate detection NOT working" "mid1='$mid1' mid2='$mid2' (BUG: IdempotencyService not integrated in ChatOrchestrator)"
    }

    # Different body with same key → should return 409
    $body2 = "{`"content`":`"DIFFERENT BODY`",`"idempotencyKey`":`"$idemKey`"}"
    $r3 = Invoke-WebSafe @{
        Uri       = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages"
        Method    = "POST"
        Headers   = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
        Body      = $body2
        TimeoutSec = 15
    }
    if ($null -ne $r3 -and $r3.StatusCode -eq 409) {
        Pass "Idempotency: same key + different body → 409"
    } else {
        Fail "Idempotency: conflict detection" "Expected 409, got $($r3?.StatusCode) (BUG: IdempotencyService not integrated)"
    }
}

Write-Section "Task 4.6 — Step Control (step_limit + continue)"

# Create a new session for step limit test
if ($agentId) {
    $r = Invoke-Api -Method "POST" -Path "/api/v1/chat/sessions" -Body @{ agentId = $agentId } -ExpectedStatus 201
    if ($r.StatusCode -eq 201) {
        $stepTestSessionId = (Get-Json $r).data.id

        # Note: The stub LlmStreamService only returns text tokens, no tool calls
        # So step_limit cannot be triggered without a real LLM returning tool_calls
        Skip "step_limit event trigger" "Stub LlmStreamService returns no tool_calls; need real LLM for step_limit test"

        # Test continue with non-existent state
        $fakeStateId = [System.Guid]::NewGuid()
        $r2 = Invoke-WebSafe @{
            Uri       = "$BaseUrl/api/v1/chat/sessions/$stepTestSessionId/continue"
            Method    = "POST"
            Headers   = @{ "Content-Type" = "application/json"; "Accept" = "text/event-stream" }
            Body      = "{`"sessionStateId`":`"$fakeStateId`"}"
            TimeoutSec = 10
        }
        if ($null -ne $r2 -and $r2.StatusCode -eq 404) {
            Pass "Continue: non-existent session state → 404"
        } else {
            Fail "Continue: non-existent state" "Expected 404, got $($r2?.StatusCode)"
        }
    }
}

Write-Section "Task 4.7 — Message Regenerate"

$msgIdForRegen = $null
if ($sessionId) {
    $detailR = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
    if ($detailR.StatusCode -eq 200) {
        $detailJ = Get-Json $detailR
        $asstMsg = $detailJ.data.messages | Where-Object { $_.role -eq "assistant" } | Select-Object -Last 1
        $msgIdForRegen = $asstMsg?.id
    }

    if ($msgIdForRegen) {
        $r = Invoke-WebSafe @{
            Uri       = "$BaseUrl/api/v1/chat/sessions/$sessionId/messages/$msgIdForRegen/regenerate"
            Method    = "POST"
            Headers   = @{ "Accept" = "text/event-stream" }
            TimeoutSec = 30
        }
        if ($null -ne $r -and $r.StatusCode -eq 200) {
            $ct = $r.Headers["Content-Type"]
            if ($ct -like "*text/event-stream*") {
                Pass "Regenerate: returns SSE stream"
            } else {
                Fail "Regenerate: Content-Type" "Expected text/event-stream, got '$ct'"
            }
            Start-Sleep -Seconds 2

            # Check message count - BUG: regenerate currently inserts a NEW message instead of updating
            $r2 = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
            if ($r2.StatusCode -eq 200) {
                $j2 = Get-Json $r2
                $asstMessages = $j2.data.messages | Where-Object { $_.role -eq "assistant" }
                if ($asstMessages.Count -gt 2) {
                    Fail "Regenerate: message count" "BUG DETECTED: Regenerate inserted a NEW message instead of updating. Got $($asstMessages.Count) assistant messages (should be ≤2)"
                } elseif ($asstMessages.Count -le 2) {
                    Pass "Regenerate: message count ok (≤2 assistant messages)"
                }
            }
        } else {
            Fail "Regenerate: HTTP status" "Expected 200, got $($r?.StatusCode)"
        }
    } else {
        Skip "Regenerate" "No assistant message to regenerate"
    }
}

Write-Section "Task 4.8 — Agent Switch"

if ($sessionId -and $agentId) {
    $newAgentId = if ($dupId) { $dupId } else { $agentId }
    $r = Invoke-Api -Method "PUT" -Path "/api/v1/chat/sessions/$sessionId/agent" -Body @{
        agentId = $newAgentId
    }
    if (Assert-Status $r 200 "PUT /api/v1/chat/sessions/{id}/agent → 200") {
        # Verify separator message was inserted
        $detailR = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
        if ($detailR.StatusCode -eq 200) {
            $detailJ = Get-Json $detailR
            $sepMsgs = $detailJ.data.messages | Where-Object { $_.role -eq "separator" }
            if ($sepMsgs.Count -ge 1) {
                Pass "Switch agent: separator message inserted in history"
            } else {
                Fail "Switch agent: separator" "No separator message found. Messages: $($detailJ.data.messages.Count)"
            }
            # Verify session currentAgentId updated
            if ($detailJ.data.currentAgentId -eq $newAgentId) {
                Pass "Switch agent: currentAgentId updated"
            } else {
                Fail "Switch agent: currentAgentId" "Expected $newAgentId, got $($detailJ.data.currentAgentId)"
            }
        }
    }
}

Write-Section "Task 4.1 — Clear Messages"

if ($sessionId) {
    $r = Invoke-Api -Method "DELETE" -Path "/api/v1/chat/sessions/$sessionId/messages" -ExpectedStatus 204
    if (Assert-Status $r 204 "DELETE /api/v1/chat/sessions/{id}/messages → 204") {
        # Session should still exist
        $r2 = Invoke-Api -Path "/api/v1/chat/sessions/$sessionId"
        if ($r2.StatusCode -eq 200) {
            $j = Get-Json $r2
            if ($j.data.messages.Count -eq 0) {
                Pass "Clear messages: messages=0 after clear"
            } else {
                Fail "Clear messages: count" "Expected 0 messages, got $($j.data.messages.Count)"
            }
            Pass "Clear messages: session still exists after clear"
        } else {
            Fail "Clear messages: session exists" "Session returned $($r2.StatusCode) after clear"
        }
    }
}

# ═══════════════════════════════════════════════════════════════════
# CLEANUP
# ═══════════════════════════════════════════════════════════════════
Write-Section "Cleanup"

if ($dupId) {
    $r = Invoke-Api -Method "DELETE" -Path "/api/v1/agents/$dupId"
    if ($r.StatusCode -eq 204) { Pass "Cleanup: delete duplicate agent" } else { Skip "Cleanup: delete duplicate" "Status $($r.StatusCode)" }
}
if ($agentId) {
    $r = Invoke-Api -Method "DELETE" -Path "/api/v1/agents/$agentId"
    if ($r.StatusCode -eq 204) { Pass "Cleanup: delete test agent" } else { Fail "Cleanup: delete test agent" "Status $($r.StatusCode)" }
}

# ═══════════════════════════════════════════════════════════════════
# SUMMARY
# ═══════════════════════════════════════════════════════════════════
$total = $PassCount + $FailCount + $SkipCount
Write-Host "`n" -NoNewline
Write-Host ("=" * 65) -ForegroundColor Cyan
Write-Host "  VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host ("=" * 65) -ForegroundColor Cyan
Write-Host "  Total:  $total" -ForegroundColor White
Write-Host "  Pass:   $PassCount" -ForegroundColor Green
Write-Host "  Fail:   $FailCount" -ForegroundColor Red
Write-Host "  Skip:   $SkipCount" -ForegroundColor Yellow
Write-Host ("=" * 65) -ForegroundColor Cyan

if ($FailCount -gt 0) {
    Write-Host "`n  [!] Some checks FAILED. See bug report: scripts/BUG_REPORT.md`n" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`n  [OK] All checks passed!`n" -ForegroundColor Green
    exit 0
}
