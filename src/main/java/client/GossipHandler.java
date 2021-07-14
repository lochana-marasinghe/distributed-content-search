package client;

import client.enums.MessageCodesEnum;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

public class GossipHandler extends Thread {
    private static final int STARTING_DELAY = 1000;
    private static final int PERIOD = 1000;

    private static Node node;

    public GossipHandler(Node receivedNode) {
        node = receivedNode;
    }

    @Override
    public void run() {
        sendGossip();
    }

    public static void sendGossip() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendGossipsToNeighbours();
            }
        };
        timer.schedule(task, STARTING_DELAY, PERIOD);
    }

    public static void sendGossipsToNeighbours() {
        if (node.getMyNeighbours().size() < 3) {
            for (Node neighbour : node.getMyNeighbours()) {
                floodGossip(neighbour);
            }
        }
    }

    public static void floodGossip (Node receiver) {
        try {
            node.ds = new DatagramSocket();
            String message = MessageCodesEnum.GOSSIP + " " + node.getMyIP() + " " + node.getMyPort();
            node.ds.send(MessageUtil.createDataPacket(message,receiver.getMyIP(),receiver.getMyPort()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
