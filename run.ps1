# ============================================================
#  run.ps1 — Compile & run TypeMaster (no Maven required)
#
#  Usage (from any terminal, inside the project folder):
#    powershell -ExecutionPolicy Bypass -File run.ps1
#
#  Or if you're already in PowerShell:
#    .\run.ps1
# ============================================================
Set-Location $PSScriptRoot

$srcRoot = "src\main\java"
$resRoot = "src\main\resources"
$outDir  = "target\classes"
$m2      = "$env:USERPROFILE\.m2\repository"

# ── JavaFX 21 (Windows) JARs ─────────────────────────────────
$jfxJars = @(
    "$m2\org\openjfx\javafx-base\21\javafx-base-21-win.jar",
    "$m2\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar",
    "$m2\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar",
    "$m2\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar",
    "$m2\org\openjfx\javafx-media\21\javafx-media-21-win.jar"
)

# ── MongoDB driver JARs ───────────────────────────────────────
$mongoJars = @(
    "$m2\org\mongodb\mongodb-driver-sync\4.9.1\mongodb-driver-sync-4.9.1.jar",
    "$m2\org\mongodb\mongodb-driver-core\4.9.1\mongodb-driver-core-4.9.1.jar",
    "$m2\org\mongodb\bson\4.9.1\bson-4.9.1.jar"
)

# ── Verify JARs exist ─────────────────────────────────────────
Write-Host "=== TypeMaster Build ===" -ForegroundColor Cyan
$missing = ($jfxJars + $mongoJars) | Where-Object { -not (Test-Path $_) }
if ($missing.Count -gt 0) {
    Write-Host "ERROR: Missing JARs:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  $_" }
    Write-Host "Install Maven and run 'mvn compile' once to download dependencies." -ForegroundColor Yellow
    exit 1
}
Write-Host "All JARs found: $($jfxJars.Count + $mongoJars.Count)" -ForegroundColor Green

# ── Build classpaths ──────────────────────────────────────────
$allJars     = $jfxJars + $mongoJars
$classpath   = ($allJars + @($outDir)) -join ";"
$modulePath  = $jfxJars -join ";"

# ── Create output directory ───────────────────────────────────
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

# ── Collect all .java sources ─────────────────────────────────
$sources = Get-ChildItem -Path $srcRoot -Recurse -Filter "*.java" |
           Select-Object -ExpandProperty FullName
Write-Host "Compiling $($sources.Count) source files..." -ForegroundColor Yellow

# ── Compile ───────────────────────────────────────────────────
& javac.exe "-cp" ($allJars -join ";") `
            "-d" $outDir `
            "--module-path" $modulePath `
            "--add-modules" "javafx.controls,javafx.fxml,javafx.graphics,javafx.media" `
            $sources 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation FAILED. See errors above." -ForegroundColor Red
    exit 1
}
Write-Host "Compilation successful!" -ForegroundColor Green

# ── Copy resources ────────────────────────────────────────────
Copy-Item -Path "$resRoot\*" -Destination $outDir -Recurse -Force
Write-Host "Resources copied." -ForegroundColor Green

# ── Launch ────────────────────────────────────────────────────
Write-Host "Launching TypeMaster..." -ForegroundColor Cyan
& java.exe "--module-path" $modulePath `
           "--add-modules" "javafx.controls,javafx.fxml,javafx.graphics,javafx.media" `
           "-cp" $classpath `
           "ui.MainUI"
