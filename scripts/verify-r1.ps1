param(
    [string]$ProjectName = "jhon-academic",
    [string]$Database = "design-software-develop",
    [string]$DatabaseUser = "design_software_user"
)

$ErrorActionPreference = "Stop"
$rabbitAuth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("app:app"))
$headers = @{ Authorization = "Basic $rabbitAuth" }
$eventId = [guid]::NewGuid().ToString()

Write-Host "1/5 API health"
Invoke-RestMethod "http://localhost:8003/actuator/health"

Write-Host "2/5 Duplicate delivery: the receipt must be stored once"
$duplicate = @{
    properties = @{ message_id = $eventId; headers = @{ event_type = "academic.r1.duplicate.practice" } }
    routing_key = "academic.r1.duplicate"
    payload = "{}"
    payload_encoding = "string"
} | ConvertTo-Json -Depth 5
foreach ($n in 1..2) {
    Invoke-RestMethod -Method Post -Uri "http://localhost:15672/api/exchanges/%2F/academic-events/publish" `
        -Headers $headers -ContentType "application/json" -Body $duplicate | Out-Null
}
Start-Sleep -Seconds 3
docker exec "$ProjectName-postgres-1" psql -U $DatabaseUser -d $Database -c "SELECT event_id, event_type, count(*) AS total FROM academic_management.processed_event WHERE event_id='$eventId' GROUP BY event_id, event_type;"

Write-Host "3/5 Force consumer failure so RabbitMQ moves it to the DLQ after 1s, 4s and 16s"
$failed = @{
    properties = @{ message_id = ([guid]::NewGuid().ToString()); headers = @{ event_type = "academic.r1.chaos"; force_failure = $true } }
    routing_key = "academic.r1.chaos"
    payload = "{}"
    payload_encoding = "string"
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri "http://localhost:15672/api/exchanges/%2F/academic-events/publish" `
    -Headers $headers -ContentType "application/json" -Body $failed | Out-Null
$dlq = $null
foreach ($second in 1..45) {
    Start-Sleep -Seconds 1
    $dlq = Invoke-RestMethod "http://localhost:15672/api/queues/%2F/academic-events.r1.dlq" -Headers $headers
    if ($dlq.messages -ge 1) { break }
}
"$($dlq.name) | messages=$($dlq.messages) | ready=$($dlq.messages_ready)"
if ($dlq.messages -lt 1) { throw "The forced failure did not reach academic-events.r1.dlq" }

Write-Host "4/5 Manual, authorized reprocess"
$reprocessed = Invoke-RestMethod -Method Post -Uri "http://localhost:8003/api/v1/events/dlq/reprocess-next" `
    -Headers @{ Authorization = "Bearer local-practice-token" }
if (-not $reprocessed.reprocessed) { throw "The DLQ endpoint did not return a message to the primary route" }

Write-Host "5/5 The DLQ must be empty and the reprocessed event must have one receipt"
foreach ($second in 1..20) {
    Start-Sleep -Seconds 1
    $dlq = Invoke-RestMethod "http://localhost:15672/api/queues/%2F/academic-events.r1.dlq" -Headers $headers
    if ($dlq.messages -eq 0) { break }
}
"$($dlq.name) | messages=$($dlq.messages) | ready=$($dlq.messages_ready)"
if ($dlq.messages -ne 0) { throw "The DLQ still contains a message after reprocesing" }
docker exec "$ProjectName-postgres-1" psql -U $DatabaseUser -d $Database -c "SELECT consumer_name, count(*) AS total FROM academic_management.processed_event GROUP BY consumer_name ORDER BY consumer_name;"
