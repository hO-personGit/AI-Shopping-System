@echo off
set "JAVA_HOME=E:\java\tools\jdk-17"
set "ROOT=E:\Microsoft\Visual Studio\source\repos\AI-Shopping-System"

start "AI-8001" cmd /k "chcp 65001 >nul && cd /d %ROOT%\ai-service && .venv\Scripts\python.exe -m uvicorn app.main:app --port 8001"

start "Backend-1234" cmd /k "chcp 65001 >nul && cd /d %ROOT%\springboot && C:\Users\рас╠\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd spring-boot:run"

start "Frontend-8080" cmd /k "chcp 65001 >nul && cd /d %ROOT%\vue && npm run serve"

echo.
echo Three services starting. Wait 30s then open http://localhost:8080
echo.
pause
