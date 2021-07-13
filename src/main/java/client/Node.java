package client;

import client.enums.MessageCodesEnum;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.origin.SystemEnvironmentOrigin;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Node implements Runnable {
    private String myIP;
    private int myPort;
    private String myUsername;
    public ArrayList<Node> myNeighbours = new ArrayList<>();
    public HashMap<String, Node> myActiveNeighbours = new HashMap<>();
    public ArrayList<String> myBlacklist = new ArrayList<>();
    private ArrayList<String> myResources = new ArrayList<>();
    DatagramSocket ds;
    public int routingTableStatus = 0;
    public int gossipSendingStatus = 0;
    public DatagramSocket socket = null;

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

    public String getMyIP() {
        return myIP;
    }

    public int getMyPort() {
        return myPort;
    }

    public String getIpPort() {
        return myIP + "/" + myPort;
    }

    public void addResource(String name, String url) {
        this.myResources.add(name);
    }

    public boolean compareWithAnotherNode(String ip, int port) {
        return myIP.equals(ip) && port == myPort;
    }

    public boolean isNeighbour(String ip, int port) {
        boolean isFound = false;
        for (Node node : myNeighbours) {
            if (node.getMyIP().equals(ip) && node.getMyPort() == port) {
                isFound = true;
                break;
            }
        }

        return isFound;
    }

    @Override
    public String toString() {
        return "Node {" +
                "myIP ='" + myIP + '\'' +
                ", myPort =" + myPort +
                '}';
    }

    @Override
    public void run() {
        System.out.println("I am running");

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(this.myPort);
        } catch (BindException ex){
            System.out.println("Already in use. Please re-register and try again !");
        }catch (SocketException e) {
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

    public void register() throws IOException {
        ds = new DatagramSocket();
        String message = MessageCodesEnum.REG + " " + myIP + " " + myPort + " " + myUsername;
        String messageLen = String.format("%4s", String.valueOf(message.length() + 5).replace(' ', '0'));
        message = messageLen + " " + message;

        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,
                InetAddress.getByName(BOOTSTRAP_SERVER_IP), BOOTSTRAP_SERVER_PORT);
        ds.send(packet);

        addInitialNeighbours();
        printMyRoutingTable();


    }

    public void join() throws IOException {
        String message = MessageCodesEnum.JOIN + " " + myIP + " " + myPort;
        String messageLen = String.valueOf(message.length() + 5).replace(' ', '0');

        message = message + " " + messageLen;

        for (Node myNeighbour : myNeighbours) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,
                    InetAddress.getByName(myNeighbour.getMyIP()), myNeighbour.getMyPort());
            ds.send(packet);
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

                    if (!isNeighbour(neighbourIp, neighbourPort))
                        addToMyRoutingTable(new Node(neighbourIp, neighbourPort));
                }

            }
        }
    }

    public void addToMyRoutingTable(Node node) {
        for (Node myNeighbour : this.myNeighbours) {
            if (myNeighbour.compareWithAnotherNode(node.getMyIP(), node.getMyPort())) return;
            else this.myNeighbours.add(node);
        }
    }

    public void printMyRoutingTable() {
        System.out.println("============My Neighbours===========");
        for (Node myNeighbour : myNeighbours) {
            System.out.println(myNeighbour.toString());
        }
    }
}
