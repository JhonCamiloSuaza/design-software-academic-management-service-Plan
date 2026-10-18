param(
    [string]$ProjectName = "jhon-academic",
    [string]$Database = "design-software-develop",
    [string]$DatabaseUser = "design_software_user"
)

$ErrorActionPreference = "Stop"
$postgres = "$ProjectName-postgres-1"
$rabbit = "$ProjectName-rabbitmq-1"
$eventId = [guid]::NewGuid().ToString()

function Query-Outbox {
    docker exec $postgres psql -U $DatabaseUser -d $Database -At -F '|' -c @"
SELECT event_id, retry_count, published_at IS NOT NULL, dead_lettered_at IS NOT NULL
FROM academic_management.outbox
WHERE event_id = '$eventId';
"@
}

Write-Host "1/5 Stop RabbitMQ temporarily"
docker stop $rabbit | Out-Null
$rabbitState = docker inspect -f '{{.State.Status}}' $rabbit
if ($rabbitState -ne 'exited') { throw "RabbitMQ did not stop before the outage test" }

try {
    Write-Host "2/5 Register a pending outbox event while RabbitMQ is down"
    docker exec $postgres psql -U $DatabaseUser -d $Database -v ON_ERROR_STOP=1 -c @"
INSERT INTO academic_management.outbox
  (event_id, event_type, topic, routing_key, payload)
VALUES
  ('$eventId', 'academic.r1.broker-outage.practice', 'academic-events', 'academic.r1.broker-outage', '{}'::jsonb);
"@ | Out-Null

    Write-Host "3/5 Verify that the API keeps the event pending and schedules a retry"
    $pending = $null
    foreach ($second in 1..20) {
        Start-Sleep -Seconds 1
        $pending = Query-Outbox
        if ($pending -and ([int]($pending -split '\|')[1]) -ge 1) { break }
    }
    Write-Host "outbox while broker is down: $pending"
    if (-not $pending) { throw "The test event was not found in the outbox" }
    $fields = $pending -split '\|'
    if ([int]$fields[1] -lt 1 -or $fields[2] -ne 'f') {
        throw "Expected a pending event with retry_count >= 1 while RabbitMQ is down"
    }
}
finally {
    Write-Host "4/5 Start RabbitMQ again"
    docker start $rabbit | Out-Null
}

Write-Host "5/5 Verify publication after the broker recovers"
$published = $null
foreach ($second in 1..45) {
    Start-Sleep -Seconds 1
    $published = Query-Outbox
    if ($published -and (($published -split '\|')[2] -eq 't')) { break }
}
Write-Host "outbox after broker recovery: $published"
if (-not $published -or (($published -split '\|')[2] -ne 't')) {
    throw "The pending event was not published after RabbitMQ recovered"
}

Write-Host "Broker outage evidence completed for event $eventId"
