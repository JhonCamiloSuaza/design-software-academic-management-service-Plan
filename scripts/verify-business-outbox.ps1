param(
    [string]$ProjectName = 'jhon-academic',
    [string]$Database = 'design-software-develop',
    [string]$DatabaseUser = 'design_software_user'
)

$ErrorActionPreference = 'Stop'
$api = "$ProjectName-academic-api-1"
$postgres = "$ProjectName-postgres-1"
$mock = "$ProjectName-reference-data-mock"
$mockFile = Join-Path $PSScriptRoot 'mock-reference-data-r1.py'
$programId = 'b0000000-0000-0000-0000-000000000004'
$centerId = 'a0000000-0000-0000-0000-000000000005'
$mockStarted = $false

if (-not (Test-Path -LiteralPath $mockFile)) { throw "Falta $mockFile" }
$apiRunning = docker inspect -f '{{.State.Running}}' $api 2>$null
if ($LASTEXITCODE -ne 0 -or $apiRunning -ne 'true') { throw "La API $api debe estar encendida." }
$existing = docker ps -a --filter "name=^/$mock$" --format '{{.Names}}'
if ($existing) { throw "Ya existe el contenedor temporal $mock. Revísalo antes de repetir la prueba." }
$program = docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -At -c `
    "SELECT is_active::text FROM academic_management.training_program WHERE id='$programId' AND deleted_at IS NULL;"
if ($LASTEXITCODE -ne 0 -or ($program -ne 'true' -and $program -ne 't')) {
    throw "El programa de práctica $programId no está activo en PostgreSQL."
}

try {
    Write-Host '1/4 Iniciar referencia temporal del centro de formación' -ForegroundColor Cyan
    docker run -d --rm --name $mock --network "container:$api" python:3.12-alpine sleep 600 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el contenedor temporal de Python.' }
    $mockStarted = $true
    docker cp $mockFile "${mock}:/server.py" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el servidor temporal.' }
    docker exec -d $mock python /server.py
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el servidor temporal.' }

    $referenceReady = $false
    foreach ($second in 1..10) {
        Start-Sleep -Seconds 1
        try {
            docker exec $mock python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8002/api/v1/training-centers/$centerId', timeout=2).read()" 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { $referenceReady = $true; break }
        } catch {
            # El servidor puede tardar unos segundos en iniciar.
        }
    }
    if (-not $referenceReady) { throw 'La referencia temporal no respondió dentro del contenedor.' }

    Write-Host '2/4 Crear una ficha mediante el POST real de la API' -ForegroundColor Cyan
    $fichaNumber = 'R1-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
    $body = @{
        ficha_number = $fichaNumber
        program_id = $programId
        training_center_id = $centerId
        start_date = (Get-Date).AddDays(10).ToString('yyyy-MM-dd')
        training_shift = 'DAY'
        training_modality = 'IN_PERSON'
        max_capacity = 30
    } | ConvertTo-Json -Compress
    $response = Invoke-WebRequest -UseBasicParsing -Method Post -Uri 'http://localhost:8003/api/v1/fichas' `
        -Headers @{ Authorization = 'Bearer local-practice-token' } -ContentType 'application/json' -Body $body
    if ($response.StatusCode -ne 201) { throw "Se esperaba HTTP 201; llegó $($response.StatusCode)." }
    $created = $response.Content | ConvertFrom-Json
    $fichaId = [string]$created.id
    if (-not $fichaId) { throw 'La API no devolvió el ID de la ficha.' }
    Write-Host "HTTP 201 | ficha_number=$fichaNumber | ficha_id=$fichaId" -ForegroundColor Green

    Write-Host '3/4 Verificar la ficha persistida en PostgreSQL' -ForegroundColor Cyan
    $ficha = docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -At -F '|' -c `
        "SELECT id, ficha_number FROM academic_management.enrollment_ficha WHERE id='$fichaId';"
    if ($LASTEXITCODE -ne 0 -or $ficha -ne "$fichaId|$fichaNumber") {
        throw 'La ficha creada no coincide con la fila guardada en PostgreSQL.'
    }
    Write-Host "enrollment_ficha: $ficha"

    Write-Host '4/4 Verificar el evento de esa ficha en outbox' -ForegroundColor Cyan
    $event = $null
    foreach ($second in 1..10) {
        $event = docker exec $postgres psql -v ON_ERROR_STOP=1 -U $DatabaseUser -d $Database -At -F '|' -c `
            "SELECT event_id, event_type, payload::text LIKE '%$fichaId%' AS references_ficha, published_at IS NOT NULL AS published FROM academic_management.outbox WHERE event_type='academic.ficha.opened' AND payload::text LIKE '%$fichaId%' ORDER BY created_at DESC LIMIT 1;"
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar outbox.' }
        if ($event) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $event -or ($event -split '\|')[2] -ne 't') { throw 'No se encontró el evento de apertura vinculado a la ficha.' }
    Write-Host "outbox: $event"
    Write-Host 'Prueba de operación de negocio y outbox completada.' -ForegroundColor Green
}
finally {
    if ($mockStarted) {
        Write-Host 'Deteniendo solo la referencia temporal.'
        docker stop $mock | Out-Null
    }
}
