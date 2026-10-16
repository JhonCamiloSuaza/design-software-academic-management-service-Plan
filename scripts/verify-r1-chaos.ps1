param(
    [string]$PostgresContainer = "jhon-academic-clean-postgres-1",
    [string]$RabbitContainer = "jhon-academic-clean-rabbitmq-1",
    [string]$Database = "design-software-develop",
    [string]$DatabaseUser = "design_software_user",
    [string]$RabbitManagementUrl = "http://localhost:15672",
    [string]$RabbitUser = "app",
    [string]$RabbitPassword = "app"
)

$ErrorActionPreference = "Stop"
$eventId = [guid]::NewGuid().ToString()

function Invoke-Psql([string]$sql) {
    $value = & docker exec $PostgresContainer psql -U $DatabaseUser -d $Database -At -v ON_ERROR_STOP=1 -c $sql
    if ($LASTEXITCODE -ne 0) { throw "psql failed" }
    return ($value | Out-String).Trim()
}

function Start-RabbitApplication {
    & docker unpause $RabbitContainer | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "RabbitMQ could not be resumed" }
}

Write-Output "event_id=$eventId"
try {
    & docker pause $RabbitContainer | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "RabbitMQ could not be paused" }

    Invoke-Psql "INSERT INTO academic_management.outbox(event_id,event_type,topic,routing_key,payload) VALUES ('$eventId','academic.r1.chaos','academic-events','academic.r1.chaos','{}'::jsonb);" | Out-Null
    Start-Sleep -Seconds 35

    $downState = Invoke-Psql "SELECT retry_count || '|' || (published_at IS NOT NULL) FROM academic_management.outbox WHERE event_id='$eventId';"
    Write-Output "broker_down=$downState"
    $parts = $downState.Split('|')
    if ($parts[1] -notin @('f', 'false')) {
        throw "The event was incorrectly marked as published while RabbitMQ was unavailable"
    }
}
finally {
    Start-RabbitApplication
}

Start-Sleep -Seconds 25
$recoveredState = Invoke-Psql "SELECT (published_at IS NOT NULL) || '|' || (dead_lettered_at IS NOT NULL) FROM academic_management.outbox WHERE event_id='$eventId';"
$processed = Invoke-Psql "SELECT count(*) FROM academic_management.processed_event WHERE event_id='$eventId';"
Write-Output "broker_recovered=$recoveredState"
Write-Output "processed_effects=$processed"

if ($recoveredState -notin @('t|f', 'true|false') -or $processed -ne '1') {
    throw "The pending event was not recovered exactly once"
}

$duplicateId = [guid]::NewGuid().ToString()
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${RabbitUser}:${RabbitPassword}"))
$headers = @{ Authorization = "Basic $credentials" }
$body = @{
    properties = @{ message_id = $duplicateId; headers = @{ event_type = "academic.r1.duplicate" } }
    routing_key = "academic.r1.duplicate"
    payload = "{}"
    payload_encoding = "string"
} | ConvertTo-Json -Depth 5

1..2 | ForEach-Object {
    $response = Invoke-RestMethod -Method Post -Uri "$RabbitManagementUrl/api/exchanges/%2F/academic-events/publish" -Headers $headers -ContentType "application/json" -Body $body
    if (-not $response.routed) { throw "Duplicate test message was not routed" }
}
Start-Sleep -Seconds 5
$duplicateEffects = Invoke-Psql "SELECT count(*) FROM academic_management.processed_event WHERE event_id='$duplicateId';"
Write-Output "duplicate_event_id=$duplicateId"
Write-Output "duplicate_effects=$duplicateEffects"
if ($duplicateEffects -ne '1') { throw "Idempotency verification failed" }

Write-Output "R1_CHAOS_RESULT=PASS"
