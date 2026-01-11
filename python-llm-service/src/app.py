# src/app.py
from fastapi import FastAPI
from pydantic import BaseModel
import ollama

app = FastAPI(title="LLM Bridge for JADE Chat")

class Instruction(BaseModel):
    instruction: str

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "jade-chat-llm-bridge"}

@app.post("/agent/solve")
async def solve(inst: Instruction):
    try:
        response = ollama.chat(
            model='llama3.2',  # modelul nou, mai bun
            messages=[
                {
                    'role': 'system',
                    'content': 'Răspunde scurt, precis și doar la întrebare. '
                               'Folosește maxim 2-3 propoziții. '
                               'Dacă nu știi exact răspunsul, spune "Nu știu". '
                               'Nu adăuga explicații inutile și nu inventa informații.'
                },
                {
                    'role': 'user',
                    'content': inst.instruction
                }
            ]
        )
        return {"response": response['message']['content'].strip()}
    except Exception as e:
        return {"error": str(e)}