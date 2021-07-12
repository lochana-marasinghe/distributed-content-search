package client;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;

public class Node {
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

    public String ipPort() {
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
}
