package com.fileserver.distributedcontentsearch;

import client.CommandHandler;
import client.Node;
import com.fileserver.distributedcontentsearch.service.FileService;
import com.fileserver.distributedcontentsearch.service.impl.FileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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
    public static String[] filesServed;

    @Autowired
	private static FileService fileService;

	public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        SpringApplication.run(DistributedContentSearchApplication.class, args);

        String ip = getMyIp();

		System.out.println("=========== DISTRIBUTED CONTENT SEARCHING APPLICATION=============");
        System.out.println("\nPlease enter the port Number:");
		port = scanner.nextInt();

		SpringApplication.run(DistributedContentSearchApplication.class, args);

		scanner.nextLine();

		System.out.println("Please enter the username:");
		String username = scanner.nextLine();

		Node newNode = new Node(ip, port, username);

		new Thread(newNode).start();

//		set serving files for the node
		fileService.init();
		filesServed = fileService.getServingFiles();

		for (String s : filesServed) {
			newNode.addResource(s, "/" + s);
		}

		newNode.showMyResourcesList();
		System.out.println("Registering " + newNode);
		newNode.register();

		CommandHandler commandHandler = new CommandHandler(newNode);
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
