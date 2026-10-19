param(
    [string]$ProjectName = 'jhon-academic',
    [string]$Database = 'design-software-develop',
    [string]$DatabaseUser = 'design_software_user'
)

$ErrorActionPreference = 'Stop'
$api = "$ProjectName-academic-api-1"
$postgres = "$ProjectName-postgres-1"
$rabbit = "$ProjectName-rabbitmq-1"
$eventId = [guid]::NewGuid().ToString()
$eventType = 'academic.r1.chaos.restart-demo'
$rabbitStopped = $false
$auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('app:app'))
$rabbitHeaders = @{ Authorization = "Basic $auth" }

function Read-Outbox {
    $result = docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -At -F '|' -c `
        "SELECT event_id, retry_count, published_at IS NOT NULL, dead_lettered_at IS NOT NULL FROM academic_management.outbox WHERE event_id='$eventId';"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar outbox.' }
    return [string]$result
}

function Read-ProcessedCount {
    $result = docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -At -c `
        "SELECT count(*) FROM academic_management.processed_event WHERE consumer_name='academic-management-r1' AND event_id='$eventId';"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar processed_event.' }
    return [int]$result
}

foreach ($container in @($api, $postgres, $rabbit)) {
    $running = docker inspect -f '{{.State.Running}}' $container
    if ($LASTEXITCODE -ne 0 -or $running -ne 'true') { throw "El contenedor $container debe estar encendido." }
}

Write-Host "Evento de esta prueba: $eventId" -ForegroundColor Yellow
try {
    Write-Host '1/6 Detener RabbitMQ durante el flujo de publicacion' -ForegroundColor Cyan
    docker stop $rabbit | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener RabbitMQ.' }
    $rabbitStopped = $true

    Write-Host '2/6 Guardar un evento pendiente en outbox' -ForegroundColor Cyan
    docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -c `
        "INSERT INTO academic_management.outbox (event_id, event_type, topic, routing_key, payload) VALUES ('$eventId', '$eventType', 'academic-events', '$eventType', '{}'::jsonb);" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo guardar el evento en outbox.' }

    $pending = ''
    foreach ($second in 1..20) {
        Start-Sleep -Seconds 1
        $pending = Read-Outbox
        $parts = $pending -split '\|'
        if ($parts.Length -eq 4 -and [int]$parts[1] -ge 1 -and $parts[2] -eq 'f') { break }
    }
    Write-Host "Broker detenido | outbox: $pending"
    $parts = $pending -split '\|'
    if ($parts.Length -ne 4 -or [int]$parts[1] -lt 1 -or $parts[2] -ne 'f' -or $parts[3] -ne 'f') {
        throw 'No se verifico un evento pendiente mientras el broker estaba caido.'
    }

    Write-Host '3/6 Reiniciar la API mientras RabbitMQ sigue detenido' -ForegroundColor Cyan
    $startedBefore = docker inspect -f '{{.State.StartedAt}}' $api
    docker restart $api | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo reiniciar la API.' }
    $startedAfter = docker inspect -f '{{.State.StartedAt}}' $api
    if ($startedBefore -eq $startedAfter) { throw 'No se pudo confirmar el reinicio de la API.' }
    Write-Host "API reiniciada | antes=$startedBefore | despues=$startedAfter"

    Write-Host '4/6 Recuperar RabbitMQ' -ForegroundColor Cyan
    docker start $rabbit | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar RabbitMQ.' }
    $rabbitStopped = $false
}
finally {
    if ($rabbitStopped) {
        Write-Host 'Recuperacion automatica: iniciando RabbitMQ.' -ForegroundColor Yellow
        docker start $rabbit | Out-Null
    }
}

Write-Host '5/6 Comprobar cero eventos perdidos tras la recuperacion' -ForegroundColor Cyan
$published = ''
$processed = 0
foreach ($second in 1..60) {
    Start-Sleep -Seconds 1
    $published = Read-Outbox
    $processed = Read-ProcessedCount
    $parts = $published -split '\|'
    if ($parts.Length -eq 4 -and $parts[2] -eq 't' -and $processed -eq 1) { break }
}
Write-Host "Broker recuperado | outbox: $published | processed_event=$processed"
$parts = $published -split '\|'
if ($parts.Length -ne 4 -or $parts[2] -ne 't' -or $parts[3] -ne 'f' -or $processed -ne 1) {
    throw 'El evento no se publico y proceso exactamente una vez tras la recuperacion.'
}

Write-Host '6/6 Reenviar el mismo event_id y comprobar cero efectos duplicados' -ForegroundColor Cyan
$duplicate = @{
    properties = @{ message_id = $eventId; headers = @{ event_type = $eventType } }
    routing_key = $eventType
    payload = '{}'
    payload_encoding = 'string'
} | ConvertTo-Json -Depth 5
$publish = Invoke-RestMethod -Method Post -Uri 'http://localhost:15672/api/exchanges/%2F/academic-events/publish' `
    -Headers $rabbitHeaders -ContentType 'application/json' -Body $duplicate
if (-not $publish.routed) { throw 'RabbitMQ no enruto el duplicado de prueba.' }
Start-Sleep -Seconds 3
$processedAfterDuplicate = Read-ProcessedCount
Write-Host "Duplicado entregado | processed_event antes=$processed | despues=$processedAfterDuplicate"
if ($processedAfterDuplicate -ne 1) { throw 'El consumidor produjo un registro duplicado.' }

Write-Host "CAOS R1 APROBADO | event_id=$eventId | publicado=t | procesado=1 | duplicado=1" -ForegroundColor Green
