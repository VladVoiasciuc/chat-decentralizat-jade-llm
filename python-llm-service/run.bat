@echo off
echo Starting LLM bridge server...
cd /d %~dp0
uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload
pause