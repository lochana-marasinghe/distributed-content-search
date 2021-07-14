package client;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MessageUtil {

    public static DatagramPacket createDataPacket(String message, String receiverIp, int receiverPort) throws UnknownHostException {

        String messageLength = String.format("%4s", (message.length() + 5)).replace(' ', '0');
        String messageSend = messageLength + " " + message;

        return new DatagramPacket(messageSend.getBytes(), messageSend.getBytes().length, InetAddress.getByName(receiverIp),
                receiverPort);
    }
}
