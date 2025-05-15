Write-Host "Stopping and removing existing containers..." -ForegroundColor Green
docker-compose down

Write-Host "Building Maven project..." -ForegroundColor Green
mvn clean package

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed. Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding containers..." -ForegroundColor Green
docker-compose build --no-cache

Write-Host "Build succesful ! Starting containers ." -ForegroundColor Green

docker-compose up

Write-Host "Done!" -ForegroundColor Green