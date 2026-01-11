package chat.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class CoordinatorAgent extends Agent {

    protected void setup() {
        // Înregistrare în DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("chat-coordinator");
        sd.setName("Coordinator");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Behaviour pentru primire comenzi (în special shutdown_all)
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if ("shutdown_all".equals(msg.getContent())) {
                        System.out.println("Coordinator: Received shutdown_all → closing all chat agents!");
                        try {
                            DFAgentDescription template = new DFAgentDescription();
                            ServiceDescription sdTemplate = new ServiceDescription();
                            sdTemplate.setType("chat-service");
                            template.addServices(sdTemplate);

                            DFAgentDescription[] results = DFService.search(myAgent, template);
                            for (DFAgentDescription dfd : results) {
                                ACLMessage shutdownMsg = new ACLMessage(ACLMessage.INFORM);
                                shutdownMsg.addReceiver(dfd.getName());
                                shutdownMsg.setContent("shutdown");
                                myAgent.send(shutdownMsg);
                            }
                            // Închidem și coordinator-ul după
                            myAgent.doDelete();
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                    }
                    // Poți adăuga și alte comenzi aici în viitor
                } else {
                    block();
                }
            }
        });

        System.out.println("CoordinatorAgent " + getAID().getName() + " is ready.");
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("CoordinatorAgent terminating.");
    }
}