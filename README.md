# Chat Decentralizat cu Agenți JADE + Asistent LLM (Proiect Sisteme Inteligente)

**Autor:** Voiașciuc Vlad  
**Grupa:** 3141B  
**Proiect ales:** Proiectul 1 – Ecosistem de agenți de tip chat (implementare descentralizată, peer-to-peer)

## Descriere generală

Aplicație de chat **descentralizat** (P2P) realizată cu platforma **JADE**, în care fiecare utilizator este reprezentat de un agent inteligent `ChatAgent` cu interfață grafică proprie.

Comunicarea se face **direct** între agenți (ACL messages), iar **Director Facilitator (DF)** este folosit doar pentru înregistrare și discovery.  
Suplimentar există:
- un agent **coordonator** pentru închidere centralizată
- un agent **asistent LLM** care răspunde la întrebări folosind **Ollama** (llama3.2) prin intermediul unui mic server **FastAPI**

### Tehnologii principale

- **JADE**  – platforma multi-agent (Java)
- **FlatLAF**  – Look & Feel modern dark pentru GUI
- **FastAPI**  – bridge Python ↔ Ollama
- **Ollama**   – model local llama3.2

## Agenți implementați

| Agent                  | Rol                                                                 | Număr instanțe | GUI?     |
|------------------------|---------------------------------------------------------------------|----------------|----------|
| `ChatAgent`            | Utilizator normal (chat P2P + istoric + Ask AI)                     | multiplu       | ✓        |
| `CoordinatorAgent`     | Închidere centralizată a întregului ecosistem                       | 1              | ✗        |
| `LLMAssistantAgent`    | Primește întrebări → trimite la FastAPI → returnează răspuns Ollama | 1              | ✗        |

## Funcționalități principale

- Descoperire automată a utilizatorilor online (prin DF)
- Chat P2P direct între oricare doi agenți (ACL INFORM)
- Interfață grafică modernă dark (FlatLAF)
- Istoric conversații salvat persistent în fișiere text (`chat_history_NUME.txt`)
- Funcționalitate **„Ask AI”** – întrebări către modelul local llama3.2
- Închidere controlată a întregului sistem prin comanda `shutdown_all` către Coordinator

## Protocol de comunicare (ACL)

| Tip interacțiune               | Performative          | Expeditor → Destinatar              | Conținut exemplu              | Serviciu DF implicat     |
|--------------------------------|-----------------------|--------------------------------------|-------------------------------|---------------------------|
| Înregistrare                   | REQUEST               | ChatAgent → DF                       | –                             | –                         |
| Căutare utilizatori            | REQUEST + INFORM      | ChatAgent ↔ DF                       | chat-service                  | ✓                         |
| Mesaj chat normal              | INFORM                | ChatAgent → ChatAgent                | „Salut, ce mai faci?”         | ✗ (direct)                |
| Întrebare către AI             | QUERY_IF → INFORM     | ChatAgent → LLMAssistantAgent        | „Explică-mi teoria relativității” | llm-service          |
| Shutdown sistem                | INFORM                | CoordinatorAgent → toți ChatAgent    | „shutdown”                    | ✓ (pentru discovery)      |

## Capturi Sniffer recomandate (pentru notare)

- Mesaje REQUEST/INFORM cu DF (search/register)
- Mesaje INFORM directe între ChatAgent-i
- QUERY_IF → INFORM ChatAgent ↔ LLMAssistantAgent
- INFORM „shutdown” de la Coordinator către toți agenții

## Cerințe îndeplinite (grilă orientativă)

- [x] Proiect funcțional și rulat (1p)
- [x] Protocol ACL clar + capturi Sniffer (2p)
- [x] Chat P2P, GUI, selecție destinatar, istoric persistent (2p)
- [x] >3 agenți JADE, ≥1 cu GUI, DF corect, închidere centralizată (2p)
- [x] GUI modernă, istoric fișier, Ask AI, coordonator dedicat (2p)
- [x] Agent LLM cu bridge FastAPI + Ollama (1p)

## Instrucțiuni de rulare

### 1. Pornire server LLM (obligatoriu pentru Ask AI)

```bash
# 1. Asigură-te că ai Ollama instalat și modelul descărcat
ollama pull llama3.2
