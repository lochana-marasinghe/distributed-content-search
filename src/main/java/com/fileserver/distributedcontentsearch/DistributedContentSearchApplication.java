package com.fileserver.distributedcontentsearch;

import client.*;
import com.fileserver.distributedcontentsearch.service.impl.FileServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

@SpringBootApplication
public class DistributedContentSearchApplication {
    private static int port;
    public static String[] filesServed;

	public static void main(String[] args)  {
		Scanner scanner = new Scanner(System.in);

		//		set serving files for the node
		new FileServiceImpl();

		String ip = getMyIp();

		System.out.println("=========== DISTRIBUTED CONTENT SEARCHING APPLICATION=============");
        System.out.print("\nPlease enter the port Number:");
		port = scanner.nextInt();

		SpringApplication.run(DistributedContentSearchApplication.class, args);

		scanner.nextLine();

		System.out.print("Please enter the username:");
		String username = scanner.nextLine();

		Node newNode = new Node(ip, port, username);

		new Thread(newNode).start();

		for (String s : filesServed) {
			newNode.addResource(s, "/" + s);
		}

		newNode.showMyResourcesList();
		System.out.println("Registering " + newNode);
		newNode.register();

		CommandHandler commandHandler = new CommandHandler(newNode);

		//Gossip handling thread
		GossipHandler gossip = new GossipHandler(newNode);
		gossip.run();

		//Heart beat thread
		HeartBeat heartBeat = new HeartBeat(newNode);
		heartBeat.run();

		//Neighbour Maintainer thread
		NeighbourMaintainer neighbourMaintainer = new NeighbourMaintainer(newNode);
		neighbourMaintainer.run();

		//start listening to commands
		while (true){
			String command = scanner.nextLine();
			commandHandler.execute(command);
		}
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
