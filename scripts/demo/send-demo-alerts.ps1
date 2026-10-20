<#
.SYNOPSIS
    Sends demo monitoring alerts to the local IncidentHub webhook.

.DESCRIPTION
    Manual testing helper for the alert ingestion and correlation flow.
    All timestamps are relative to the moment the script starts and lie in the past,
    so occurredAt never exceeds the time of receipt.

    Scenarios:
      cascade     CORE_DATABASE failure cascading up the demo graph, plus one noise
                  alert on NOTIFICATION_SERVICE that is not reachable from the root.
      late        cascade, then (after the correlation tick) two late alerts:
                  one inside the group interval and one before the group start.
                  The second one becomes the new earliest alert, so the baseline
                  must replace the probable root with EVENT_BUS.
      stale-card  a single SUPPORT_DESK alert, used to make an opened group card
                  outdated (run it while the card of an OPEN group is on screen).

.EXAMPLE
    ./scripts/demo/send-demo-alerts.ps1
    ./scripts/demo/send-demo-alerts.ps1 -Scenario late
    ./scripts/demo/send-demo-alerts.ps1 -Scenario stale-card
#>
param(
    [ValidateSet('cascade', 'late', 'stale-card')]
    [string]$Scenario = 'cascade',
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Token,
    [int]$TickWaitSeconds = 7
)

$ErrorActionPreference = 'Stop'

if (-not $Token) {
    $envFile = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) '.env'
    if (Test-Path $envFile) {
        $line = Get-Content $envFile | Where-Object { $_ -match '^INTEGRATION_TOKEN=' } | Select-Object -First 1
        if ($line) { $Token = $line.Substring('INTEGRATION_TOKEN='.Length).Trim() }
    }
}
if (-not $Token) { $Token = 'incident-hub-local-integration-token' }

$now = [DateTime]::UtcNow
$run = Get-Date -Format 'HHmmss'

function New-Alert($id, $service, $type, $severity, $offsetSeconds, $description) {
    [ordered]@{
        source      = 'prometheus'
        externalId  = "$run-$id"
        serviceCode = $service
        signalType  = $type
        severity    = $severity
        occurredAt  = $now.AddSeconds($offsetSeconds).ToString('yyyy-MM-ddTHH:mm:ssZ')
        description = $description
    }
}

function Send-Alerts($alerts, $title) {
    $body = @{ alerts = @($alerts) } | ConvertTo-Json -Depth 4
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/alerts" `
        -Headers @{ 'X-Integration-Token' = $Token } `
        -ContentType 'application/json; charset=utf-8' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
    Write-Host "`n== $title ==" -ForegroundColor Cyan
    $response.results | Format-Table externalId, status, alertId, reason -AutoSize
}

$cascade = @(
    (New-Alert 'db'       'CORE_DATABASE'        'AVAILABILITY' 'CRITICAL' -150 'Primary DB health-check failed 3 times'),
    (New-Alert 'identity' 'IDENTITY_API'         'ERROR_RATE'   'CRITICAL' -130 '5xx rate 42% on /token'),
    (New-Alert 'payment'  'PAYMENT_API'          'LATENCY'      'CRITICAL' -125 'p99 8.4s, threshold 1.5s'),
    (New-Alert 'gateway'  'API_GATEWAY'          'ERROR_RATE'   'WARNING'  -105 '5xx rate 9% on /auth/*'),
    (New-Alert 'noise'    'NOTIFICATION_SERVICE' 'SATURATION'   'INFO'     -95  'Send queue 78% full'),
    (New-Alert 'portal'   'CUSTOMER_PORTAL'      'LATENCY'      'WARNING'  -85  'Page response 6.2s'),
    (New-Alert 'billing'  'BILLING_WORKER'       'SATURATION'   'WARNING'  -60  'Consumer lag 410k messages')
)

switch ($Scenario) {
    'cascade' {
        Send-Alerts $cascade 'cascade'
    }
    'late' {
        Send-Alerts $cascade 'cascade'
        Write-Host "`nWaiting $TickWaitSeconds s for the correlation tick..."
        Start-Sleep -Seconds $TickWaitSeconds
        Send-Alerts @(
            (New-Alert 'inside' 'BILLING_WORKER' 'LATENCY'      'WARNING'  -100 'Inside the group interval'),
            (New-Alert 'before' 'EVENT_BUS'      'AVAILABILITY' 'CRITICAL' -200 'Earlier than the group start')
        ) 'late alerts'
        Write-Host "`nExpected: 'inside' has zero distance, 'before' is 50 s before the group start,"
        Write-Host "and the probable root of the group is replaced with EVENT_BUS."
    }
    'stale-card' {
        Send-Alerts @(
            (New-Alert 'desk' 'SUPPORT_DESK' 'ERROR_RATE' 'WARNING' -5 '502 on ticket open')
        ) 'stale-card'
    }
}

Write-Host "`nGroups appear after the next correlation tick (ALERT_CORRELATION_INTERVAL, 5 s by default)."
