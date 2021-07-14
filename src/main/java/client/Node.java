package client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import client.enums.MessageCodesEnum;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

@Data
@NoArgsConstructor
@Slf4j
public class Node implements Runnable {
    private String myIP;
    private int myPort;
    private String myUsername;
    private ArrayList<Node> myNeighbours = new ArrayList<>();
    private HashMap<String, Node> myActiveNeighbours = new HashMap<>();
    private ArrayList<Node> myBlacklist = new ArrayList<>();
    private ArrayList<String> myResources = new ArrayList<>();
    DatagramSocket ds;
    private int routingTableStatus = 0;
    private int gossipSendingStatus = 0;
    private DatagramSocket socket = null;

    private static final int BOOTSTRAP_SERVER_PORT = 55555;
    private static final String BOOTSTRAP_SERVER_IP = "192.168.43.157";

    @Autowired
    ServletContext context;

    public Node(String myIP, int myPort, String myUsername) {
        this.myIP = myIP;
        this.myPort = myPort;
        this.myUsername = myUsername;
    }

    public Node(String myIP, int myPort) {
        this.myIP = myIP;
        this.myPort = myPort;
    }

    public String getIpPort() {
        return myIP + "/" + myPort;
    }

    public void addResource(String name, String url) {
        this.myResources.add(name);
    }

    public boolean compareMeWithAnotherNode(Node otherNode) {
        return myIP.equals(otherNode.getMyIP()) && otherNode.getMyPort() == myPort;
    }

    public boolean isNeighbour(String ip, int port) {
        boolean isFound = false;
        for (Node neighbour : myNeighbours) {
            if (neighbour.compareMeWithAnotherNode(new Node(ip, port))) {
                isFound = true;
                break;
            }
        }

        return isFound;
    }

    public boolean isBlacklisted(Node receivedNode) {
        boolean isFound = false;
        for (Node node : myBlacklist) {
            if (node.compareMeWithAnotherNode(receivedNode)) {
                isFound = true;
                break;
            }
        }

        return isFound;
    }

    @Override
    public String toString() {
        return "Node{" +
                "My IP = '" + myIP + '\'' +
                ", My Port = " + myPort +
                '}';
    }


    @Override
    public void run() {
        System.out.println("I am running");

        DatagramSocket socket;
        try {
            socket = new DatagramSocket(this.myPort);
            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket messageRequest = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(messageRequest);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] receivedData = messageRequest.getData();
                String receivedMessage = new String(receivedData, 0, receivedData.length);
                StringTokenizer st = new StringTokenizer(receivedMessage, " ");
                Node sender = new Node(receivedMessage.split(" ")[2],
                        Integer.parseInt(receivedMessage.split(" ")[3]));


                switch (st.nextToken()) {
                    case "JOIN":
                        log.info("[JOIN] request from " + sender);
                        addToMyRoutingTable(sender);
                        break;
                    case "GOSSIP":
                        log.info("[GOSSIP] from " + sender);
                        sendMyNeighbours(sender);
                        break;
                    case "GOSSIPOK":
                        log.info("[GOSSIPOK] from " + sender);
                        handleGossipOK(st);
                    default:
                        log.warn("Invalid message type");
                        break;
                }
            }
        } catch (BindException ex) {
            log.info("Already in use. Please re-register and try again !");
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void showMyResourcesList() {
        System.out.println(" Files List at" + myIP + "/" + myPort);
        System.out.println("============================");
        for (String name : myResources) {
            System.out.println(name);
        }
    }

    public void register() {
        try {
            ds = new DatagramSocket();
            String message = MessageCodesEnum.REG + " " + myIP + " " + myPort + " " + myUsername;
//        String messageLen = String.format("%4s", String.valueOf(message.length() + 5).replace(' ', '0'));
//        message = messageLen + " " + message;
//
//        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,
//                InetAddress.getByName(BOOTSTRAP_SERVER_IP), BOOTSTRAP_SERVER_PORT);

            ds.send(MessageUtil.createDataPacket(message, BOOTSTRAP_SERVER_IP, BOOTSTRAP_SERVER_PORT));

            addInitialNeighbours();
            printMyRoutingTable();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void join()  {
        try {
        String message = MessageCodesEnum.JOIN + " " + myIP + " " + myPort;
//        String messageLen = String.valueOf(message.length() + 5).replace(' ', '0');

//        message = message + " " + messageLen;

        for (Node myNeighbour : myNeighbours) {
//            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,
//                    InetAddress.getByName(myNeighbour.getMyIP()), myNeighbour.getMyPort());
//            ds.send(packet);
            ds.send(MessageUtil.createDataPacket(message, myNeighbour.getMyIP(), myNeighbour.getMyPort()));
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addInitialNeighbours() throws IOException {
        byte[] byteBuffer = new byte[512];
        DatagramPacket registerResponse = new DatagramPacket(byteBuffer, byteBuffer.length);
        ds.receive(registerResponse);
        String responseMessage = new String(byteBuffer, 0, registerResponse.getLength());
        String[] splitResponse = responseMessage.split(" ");


        if (splitResponse[1].equals(MessageCodesEnum.REGOK.name())) {
            int responseCode = Integer.parseInt(splitResponse[2]);

            if (responseCode == 9999) {
                System.out.println(responseCode + " Node Registration Failed. Error in registration request.");
            } else if (responseCode == 9998) {
                System.out.println(responseCode + " Node Registration Failed. The Node is already registered.");
            } else if (responseCode == 9997) {
                System.out.println(responseCode + " Node Registration Failed. IP and Port is already in use.");
            } else if (responseCode == 9996) {
                System.out.println(responseCode + " Node Registration Failed. The BootStrap server is filled");
            } else if (responseCode == 0) {
                System.out.println("Nodes in the network: " + responseCode + "Node registration successful" +
                        ". No other nodes available in the network yet");
            } else {
                System.out.println("Node in the network: " + responseCode + "Node registration successful.");

                for (int i = 3; i < splitResponse.length; i += 2) {
                    String neighbourIp = splitResponse[i];
                    int neighbourPort = Integer.parseInt(splitResponse[i + 1]);

                    if (isNeighbour(neighbourIp, neighbourPort)) // Will have to remove this check redundant
                        addToMyRoutingTable(new Node(neighbourIp, neighbourPort));
                }

            }
        }
    }

    public void addToMyRoutingTable(Node node) {
        if (isNeighbour(node.getMyIP(), node.getMyPort())) {
            log.warn(node + " is already a neighbour of " + this);
        } else {
            this.myNeighbours.add(node);
            log.info(node + " was added to the routing table of " + this);
        }
    }

    public void printMyRoutingTable() {
        System.out.println("============My Neighbours===========");
        for (Node myNeighbour : myNeighbours) {
            System.out.println(myNeighbour.toString());
        }
    }

    public void sendMyNeighbours(Node sender) {
        addToMyRoutingTable(sender);

        if (this.myNeighbours.size() > 1) {
            StringBuilder nodesToSend = new StringBuilder();
            int noOfNodesToSend = 0;
            for (Node neighbour : this.myNeighbours) {
                if (neighbour.compareMeWithAnotherNode(sender)) {
                    nodesToSend.append(neighbour.getMyIP()).append(" ").append(neighbour.myPort).append(" ");
                    noOfNodesToSend++;
                }
            }
            String message = MessageCodesEnum.GOSSIPOK + " " + sender.getMyIP() + " " + sender.getMyPort() + " " + noOfNodesToSend + " "
                    + nodesToSend.delete(nodesToSend.length() - 1, nodesToSend.length());
            log.info("Sending neighbours to " + sender);
            try {
                ds.send(MessageUtil.createDataPacket(message, sender.getMyIP(), sender.getMyPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log.warn(this + " has only one neighbour. No additional neighbours to send " + sender);
        }
    }

    public void handleGossipOK(StringTokenizer st) {
        if (this.myNeighbours.size() < 3) {

            Node sender = new Node(st.nextToken(), Integer.parseInt(st.nextToken()));
            int noOfReceivedNodes = Integer.parseInt(st.nextToken());

            log.info("[GOSSIP] Trying to add " + sender);
            addToMyRoutingTable(sender);

            for (int i = 0; i < noOfReceivedNodes; i++) {
                Node receivedNode = new Node(st.nextToken(), Integer.parseInt(st.nextToken()));

                if (isBlacklisted(receivedNode)) {
                    log.info("[GOSSIP] Trying to add " + receivedNode);
                    addToMyRoutingTable(receivedNode);
                } else {
                    log.warn(receivedNode + " is blacklisted");
                }
            }
        } else {
            log.warn(this + " already has 2 neighbours");
        }
    }

    public void showRoutingTable() {
    }

    public void unregister() {
    }

    public void search(String searchFileName) {
    }

    public void leave() {
    }

    public void download(String ip, String port, String fileName) {
    }
}
