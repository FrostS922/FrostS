param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [string]$ApiUrl = "http://localhost:8080/api/error-report/sourcemaps",
    
    [string]$DistDir = "./dist/assets",
    
    [string]$Token
)

if (-not $Token) {
    Write-Host "[ERROR] Token is required. Usage: .\upload-sourcemaps.ps1 -Version v1.0.0 -Token <admin-jwt-token>" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $DistDir)) {
    Write-Host "[ERROR] Dist directory not found: $DistDir" -ForegroundColor Red
    exit 1
}

$mapFiles = Get-ChildItem -Path $DistDir -Filter "*.map" -Recurse

if ($mapFiles.Count -eq 0) {
    Write-Host "[WARN] No .map files found in $DistDir" -ForegroundColor Yellow
    exit 0
}

Write-Host "[INFO] Found $($mapFiles.Count) Source Map file(s) for version $Version" -ForegroundColor Cyan

$success = 0
$failed = 0

foreach ($file in $mapFiles) {
    $relativePath = $file.FullName.Substring((Resolve-Path $DistDir).Path.Length + 1)
    Write-Host "[UPLOAD] $relativePath ..." -NoNewline

    try {
        $boundary = [System.Guid]::NewGuid().ToString()
        $fileBytes = [System.IO.File]::ReadAllBytes($file.FullName)
        $fileName = [System.IO.Path]::GetFileName($file.FullName)

        $headers = @{
            "Authorization" = "Bearer $Token"
        }

        $form = @{
            file   = $file.FullName
            version = $Version
        }

        $response = Invoke-RestMethod -Uri "$ApiUrl" -Method Post -Headers $headers -Form $form -ContentType "multipart/form-data"

        Write-Host " OK" -ForegroundColor Green
        $success++
    }
    catch {
        Write-Host " FAILED" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor DarkRed
        $failed++
    }
}

Write-Host ""
Write-Host "[DONE] Uploaded: $success, Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })

if ($failed -gt 0) {
    exit 1
}
