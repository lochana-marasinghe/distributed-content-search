package client;

import client.enums.MessageCodesEnum;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

public class HeartBeat extends Thread {
    private static final int STARTING_DELAY = 1000;
    private static final int PERIOD = 1000;

    private Node node;
    private DatagramSocket ds;

    public HeartBeat(Node receivedNode) {
        node = receivedNode;
    }

    @Override
    public void run() {
        sendHeartBeatQuery();
    }

    private void sendHeartBeatQuery() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendHeartBeatQueryToNeighbours();;
            }
        };
        timer.schedule(task, STARTING_DELAY, PERIOD);
    }

    private void sendHeartBeatQueryToNeighbours() {
        if (node.getMyNeighbours().size() > 1) {
            for (Node neighbour: node.getMyNeighbours()) {
                sendHeartBeatMessage(neighbour);
            }
        }
    }

    private void sendHeartBeatMessage(Node receiver) {
        try {
            node.ds = new DatagramSocket();
            String message = MessageCodesEnum.ISACTIVE + " " + node.getMyIP() + " " + node.getMyPort();
            node.ds.send(MessageUtil.createDataPacket(message,receiver.getMyIP(),receiver.getMyPort()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
