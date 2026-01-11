package chat.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LLMAssistantAgent extends Agent {

    private static final String ENDPOINT = "http://localhost:8000/agent/solve";

    @Override
    protected void setup() {
        // Înregistrare în DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("llm-service");
            sd.setName("LLM-Assistant");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.QUERY_IF) {
                    String question = msg.getContent();
                    System.out.println("LLM: Am primit întrebare → " + question);

                    String answer = callPythonOllama(question);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(answer);  // trimitem doar răspunsul curat
                    send(reply);

                    System.out.println("LLM: Am trimis răspuns → " + answer);
                } else {
                    block();
                }
            }
        });

        System.out.println("LLMAssistantAgent " + getAID().getName() + " is ready.");
    }

    private String callPythonOllama(String question) {
        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            String json = "{\"instruction\": \"" + question.replace("\"", "\\\"") + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            String raw = sb.toString().trim();
            String answer = raw.replaceAll(".*\"response\":\"(.*?)\".*", "$1");

            answer = answer.replace("\\n", "\n")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\")
                           .trim();

            if (answer.isEmpty() || answer.equals(raw)) {
                return "Eroare parsing răspuns: " + raw;
            }
            return answer;

        } catch (Exception e) {
            System.out.println("LLM: Eroare: " + e.getMessage());
            e.printStackTrace();
            return "Eroare conexiune Ollama: " + e.getMessage();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception ignored) {}
        System.out.println("LLMAssistantAgent terminating.");
    }
}