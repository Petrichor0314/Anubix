Write-Host "Stopping and removing existing containers..." -ForegroundColor Green
docker-compose down

Write-Host "Building Maven project..." -ForegroundColor Green
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed. Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding containers..." -ForegroundColor Green
docker-compose build --no-cache

Write-Host "Build succesful ! Starting containers with scaling..." -ForegroundColor Green

docker-compose up -d --scale feature-service=3 --scale toggle-service=3 --scale analytics-service=3

Write-Host "Done!" -ForegroundColor Green