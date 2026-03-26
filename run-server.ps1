Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "[1/2] Installing shared module..."
mvn -q -f ".\werewolf-shared\pom.xml" clean install -DskipTests

Write-Host "[2/2] Starting server..."
mvn -f ".\werewolf-server\pom.xml" exec:java
