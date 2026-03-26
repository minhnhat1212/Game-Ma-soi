Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "[1/1] Starting client..."
mvn -f ".\werewolf-client\pom.xml" exec:java

