package com.fileserver.distributedcontentsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

@SpringBootApplication
public class DistributedContentSearchApplication {
    private static int port;

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        SpringApplication.run(DistributedContentSearchApplication.class, args);
    }

	public static String getMyIp() {
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			return socket.getLocalAddress().getHostAddress();
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getPort(){
		return port;
	}
}
