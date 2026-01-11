Proiect Sisteme Inteligente – Chat Decentralizat cu Agenți JADE și Asistent LLM
Autor: Voiașciuc Vlad - GRUPA 3141B
Proiect ales: Proiectul 1 – Ecosistem de agenți de tip chat (implementare descentralizată, peer-to-peer)
Tehnologii: JADE (Java), FastAPI + Ollama (Python bridge pentru LLM), FlatLAF pentru GUI
Descriere generală
Aplicatia implementează un sistem de chat descentralizat folosind platforma JADE. Fiecare utilizator este reprezentat de un agent ChatAgent cu interfață grafică proprie. Descoperirea utilizatorilor și comunicarea se face prin mesaje ACL directe (P2P), cu ajutorul Director Facilitator (DF) doar pentru înregistrare și discovery.
S-a adăugat un agent coordonator pentru închidere centralizată și un agent asistent LLM (bonus) care răspunde la întrebări folosind Ollama prin intermediul unui bridge FastAPI.
Agenți implementați

ChatAgent (multiplu – ex: maria, ion, ana)
Interfață grafică modernă (FlatLAF Dark).
Listă de utilizatori online (actualizată periodic prin DF).
Trimitere/primire mesaje către orice utilizator selectat.
Buton „Ask AI” pentru întrebări către asistentul LLM.
Salvare istoric conversații în fișiere text (chat_history_nume.txt).
Închidere curată la X sau comandă shutdown.

CoordinatorAgent
Primește comanda shutdown_all și trimite mesaj „shutdown” tuturor ChatAgent-ilor (închidere centralizată a ecosistemului).

LLMAssistantAgent (bonus)
Primește întrebări de la ChatAgent prin ACL QUERY_IF.
Trimite request POST către serverul FastAPI local (http://localhost:8000/agent/solve).
Primește și returnă răspunsul de la modelul Ollama (llama3.2).


Protocol de comunicare

Înregistrare/discovery: fiecare ChatAgent se înregistrează în DF cu serviciu tip chat-service și nume = nickname. Lista de utilizatori se actualizează periodic prin căutare DF.
Mesaje chat normale: ACLMessage performativ INFORM, trimis direct către AID-ul destinatarului găsit prin DF.
Întrebări către AI: ACLMessage QUERY_IF către LLMAssistantAgent (găsit prin serviciu llm-service); răspuns INFORM.
Shutdown: Coordinator trimite INFORM cu content „shutdown” către toți ChatAgent-ii (găsiți prin DF).

Capturi Sniffer (recomandat pentru cele 2 puncte):
Porniți Sniffer Agent în containerul JADE. Veți vedea:

Mesaje REQUEST la DF pentru search.
Mesaje INFORM între ChatAgent-i (chat normal).
Mesaje QUERY_IF → INFORM între ChatAgent și LLMAssistantAgent.
Mesaje INFORM de la Coordinator către toți agenții la shutdown.

Cerințe îndeplinite (grilă de notare)

1 pct Oficiu → Proiect funcțional și rulat.
2 pct Protocol + capturi → Protocol ACL clar descris; capturi Sniffer disponibile.
2 pct Funcționalitate principală → Chat P2P, GUI, selecție destinatar, istoric fișier.
2 pct Design → >3 agenți JADE, ≥1 cu GUI, DF corect utilizat, închidere centralizată.
2 pct Structură avansată → GUI modernă, istoric persistent, Ask AI integrat, coordonator dedicat.
1 pct Agent Pydantic/LLM → LLMAssistantAgent complet funcțional cu bridge FastAPI/Ollama.

Instrucțiuni de rulare
1. Server LLM bridge (Python)

Asigurați-vă că Ollama rulează și modelul este descărcat:Bashollama pull llama3.2
Rulați serverul (dublu-click pe run.bat sau):Bashuvicorn src.app:app --host 0.0.0.0 --port 8000 --reload

2. Platforma JADE (Eclipse sau linie de comandă)
Run configuration recomandată (Eclipse → Run Configurations → Arguments):
text-gui -agents coordinator:chat.agents.CoordinatorAgent;llm:chat.agents.LLMAssistantAgent;maria:chat.agents.ChatAgent;ion:chat.agents.ChatAgent;ana:chat.agents.ChatAgent
Sau comandă manuală (din folderul proiectului, după build):
Bashjava -cp "lib/*;bin" jade.Boot -gui -agents coordinator:chat.agents.CoordinatorAgent;llm:chat.agents.LLMAssistantAgent;maria:chat.agents.ChatAgent;ion:chat.agents.ChatAgent;ana:chat.agents.ChatAgent
3. Utilizare

Se deschid ferestrele GUI pentru fiecare ChatAgent.
Selectați destinatar → scrieți mesaj → Send.
Pentru AI: scrieți întrebare → Ask AI.
Închidere: trimiteți mesaj „shutdown_all” către CoordinatorAgent (prin Sniffer sau manual).

Fișiere suplimentare recomandate pentru predare

Capturi de ecran/video cu Sniffer (comunicare ACL).
Video scurt demo (pornire, chat între utilizatori, Ask AI, shutdown).
Arhivă proiect <100 MB (compresată).
