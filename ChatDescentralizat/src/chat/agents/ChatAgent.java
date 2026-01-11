package chat.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ChatAgent extends Agent {

    private JFrame frame;
    private JTextArea chatArea;
    private JList<String> userList;
    private DefaultListModel<String> userModel;
    private JTextField messageField;
    private String historyFile;

    @Override
    protected void setup() {
        historyFile = "chat_history_" + getLocalName() + ".txt";

        // Înregistrare în DF
        registerInDF();

        // GUI
        SwingUtilities.invokeLater(this::createAndShowGUI);

        // Primire mesaje
        addBehaviour(new MessageReceiverBehaviour());

        // Actualizare listă utilizatori
        addBehaviour(new TickerBehaviour(this, 8000) {
            @Override
            protected void onTick() {
                updateUserList();
            }
        });

        System.out.println("ChatAgent " + getLocalName() + " started with GUI");
    }

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("chat-service");
            sd.setName(getLocalName());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("Eroare la înregistrarea în DF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createAndShowGUI() {
        FlatDarkLaf.setup();

        frame = new JFrame("Chat Decentralizat - " + getLocalName());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(740, 540);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Lista utilizatori
        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(190, 0));
        mainPanel.add(userScroll, BorderLayout.WEST);

        // Zona chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 36));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        mainPanel.add(chatScroll, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton askAIButton = new JButton("Ask AI");

        inputPanel.add(askAIButton, BorderLayout.WEST);
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        sendButton.addActionListener(this::sendButtonAction);
        messageField.addActionListener(this::sendButtonAction);
        askAIButton.addActionListener(e -> askAI());

        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(mainPanel);
        frame.setVisible(true);

        // Închidere curată la X
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                doDelete();
            }
        });
    }

    private void sendButtonAction(ActionEvent e) {
        String text = messageField.getText().trim();
        String selected = userList.getSelectedValue();
        if (selected == null || text.isEmpty()) {
            appendToChat(">>> Selectează un destinatar și scrie un mesaj!\n");
            return;
        }
        sendMessage(selected, text);
        messageField.setText("");
    }

    private void askAI() {
        String question = messageField.getText().trim();
        if (question.isEmpty()) {
            appendToChat(">>> Scrie o întrebare pentru AI!\n");
            return;
        }

        appendToChat("You → AI: " + question + "\n");
        saveToHistory("You → AI: " + question);
        messageField.setText("");

        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("llm-service");
            template.addServices(sd);

            DFAgentDescription[] results = DFService.search(this, template);
            if (results.length == 0) {
                appendToChat(">>> AI assistant nu este disponibil!\n");
                return;
            }

            ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
            query.addReceiver(results[0].getName());
            query.setContent(question);
            send(query);

        } catch (FIPAException ex) {
            appendToChat(">>> Eroare la contactarea AI: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    public void sendMessage(String receiverName, String content) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("chat-service");
            sd.setName(receiverName);
            template.addServices(sd);

            DFAgentDescription[] results = DFService.search(this, template);
            if (results.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(results[0].getName());
                msg.setContent(content);
                send(msg);

                appendToChat("Me → " + receiverName + ": " + content + "\n");
                saveToHistory("Me → " + receiverName + ": " + content);
            } else {
                appendToChat(">>> Utilizatorul " + receiverName + " nu mai este online\n");
            }
        } catch (FIPAException ex) {
            appendToChat(">>> Eroare la trimitere mesaj: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    private void appendToChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateUserList() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("chat-service");
            template.addServices(sd);

            DFAgentDescription[] results = DFService.search(this, template);

            List<String> names = new ArrayList<>();
            for (DFAgentDescription dfd : results) {
                Iterator<ServiceDescription> services = dfd.getAllServices();
                if (services.hasNext()) {
                    String nick = services.next().getName();
                    if (nick != null && !nick.equals(getLocalName())) {
                        names.add(nick);
                    }
                }
            }

            SwingUtilities.invokeLater(() -> {
                userModel.clear();
                names.forEach(userModel::addElement);
            });
        } catch (FIPAException e) {
            appendToChat(">>> Eroare la actualizarea listei de utilizatori\n");
            e.printStackTrace();
        }
    }

    private class MessageReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    String senderLocal = msg.getSender().getLocalName();

                    // Ignorăm mesaje de sistem
                    if (senderLocal.equals("df") || senderLocal.contains("ams") || senderLocal.contains("rma")) {
                        return;
                    }

                    // Shutdown
                    if ("shutdown".equals(msg.getContent())) {
                        doDelete();
                        return;
                    }

                    String content = msg.getContent();
                    String displaySender;

                    // Detectăm LLMAssistantAgent
                    if (senderLocal.toLowerCase().contains("llm") || senderLocal.toLowerCase().contains("assistant")) {
                        displaySender = "AI";
                    } else {
                        displaySender = senderLocal;
                        if (displaySender.contains("@")) {
                            displaySender = displaySender.substring(0, displaySender.indexOf('@'));
                        }
                    }

                    appendToChat(displaySender + " → Me: " + content + "\n");
                    saveToHistory(displaySender + " → Me: " + content);
                }
            } else {
                block();
            }
        }
    }

    private void saveToHistory(String entry) {
        try (FileWriter fw = new FileWriter(historyFile, true)) {
            fw.write("[" + new Date() + "] " + entry + "\n");
        } catch (IOException e) {
            System.err.println("Eroare la salvarea istoricului: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println(getLocalName() + " terminating.");
        if (frame != null) {
            frame.dispose();
        }
    }
}