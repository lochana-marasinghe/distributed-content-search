package client;

import client.enums.MessageCodesEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Data
@NoArgsConstructor
@Slf4j
public class Node implements Runnable {
    private String myIP;
    private int myPort;
    private String myUsername;
    private ArrayList<Node> myNeighbours = new ArrayList<>();
    private ArrayList<Node> myActiveNeighbours = new ArrayList<>();
    private ArrayList<Node> myBlacklist = new ArrayList<>();
    private ArrayList<String> myResources = new ArrayList<>();
    DatagramSocket ds;
    private int routingTableStatus = 0;
    private int gossipSendingStatus = 0;
    private DatagramSocket socket = null;

    private static final int BOOTSTRAP_SERVER_PORT = 55555;
    private static final String BOOTSTRAP_SERVER_IP = "192.168.8.100";

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
        for (Node blacklistNode : myBlacklist) {
            if (blacklistNode.compareMeWithAnotherNode(receivedNode)) {
                isFound = true;
                break;
            }
        }

        return isFound;
    }

    public boolean isActiveNeighbour(Node receiveNode) {
        boolean isActive = false;
        for (Node activeNeighbour : myActiveNeighbours) {
            if (activeNeighbour.compareMeWithAnotherNode(receiveNode)) {
                isActive = true;
                break;
            }
        }
        return isActive;
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
        System.out.println(this + " started to listen to messages");

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
                String receivedMessage = new String(receivedData, 0, receivedData.length).trim();
                StringTokenizer st = new StringTokenizer(receivedMessage, " ");

                String encodeLength = st.nextToken();

                Node sender = null;
                MessageCodesEnum messageCode = MessageCodesEnum.valueOf(st.nextToken());
                if (messageCode == MessageCodesEnum.SEROK) {
                    sender = new Node(receivedMessage.split(" ")[3],
                            Integer.parseInt(receivedMessage.split(" ")[4]));
                } else if (messageCode != MessageCodesEnum.LEAVEOK){
                    sender = new Node(receivedMessage.split(" ")[2],
                            Integer.parseInt(receivedMessage.split(" ")[3]));
                }
                switch (messageCode) {
                    case JOIN:
//                        log.info("[JOIN] request from " + sender);
                        addToMyRoutingTable(sender, false);
                        break;
                    case GOSSIP:
                        log.info("[GOSSIP] from " + sender);
                        sendMyNeighbours(sender);
                        break;
                    case GOSSIPOK:
                        log.info("[GOSSIPOK] from " + sender);
                        handleGossipOK(st);
                    case ISACTIVE:
//                        log.info("[ISACTIVE] from " + sender);
                        handleHeartBeat(sender);
                        break;
                    case ACTIVE:
//                        log.info("[ACTIVE] from " + sender);
                        addActiveNeighbours(sender);
                        break;
                    case SER:
                        log.info("[SER] from " + sender);
                        handleSearch(sender, receivedMessage);
                        break;
                    case SEROK:
                        log.info("[SEROK] from " + sender);
                        handleSearchOk(sender, receivedMessage);
                        break;
                    case LEAVE:
                        handleLeave(receivedMessage);
                    case LEAVEOK:
                        if (receivedMessage.split(" ")[2].equals("0"))
                            System.out.println(myIP + ":" + myPort + " Leave successfully");
                        else
                            System.out.println("Leave failed!");
                        break;
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

    private void handleLeave(String receivedMessage) {
        String ip = receivedMessage.split(" ")[2];
        int port = Integer.parseInt(receivedMessage.split(" ")[3]);
        int removingIndex = -1;
        //search if current node has the neighbour
        for (int i = 0; i < myNeighbours.size(); i++) {
            if (myNeighbours.get(i).getMyIP().equals(ip) && myNeighbours.get(i).getMyPort() == port) {
                removingIndex = i;
            }
        }
        //if have, remove from routing table
        if (removingIndex >= 0) {
            myNeighbours.remove(removingIndex);
            System.out.println("Removed node " + ip + ":" + port);
            String request = "LEAVEOK 0";
            String length = String.valueOf(request.length() + 5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();

            InetAddress receiverIP = null;
            try {
                receiverIP = InetAddress.getByName(ip);

                DatagramPacket packet = new DatagramPacket(msg, msg.length, receiverIP, port);
                ds.send(packet);        //send response
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("There is no any node in  " + ip + ":" + port + " to remove");
        }
    }

    private void handleSearchOk(Node founder, String receivedMessage) {
        String[] splitReceivedMsg = receivedMessage.split("\"");
        ArrayList<String> cleanedMessage = removeSpaces(splitReceivedMsg);

        String command = cleanedMessage.get(0);
        int fileCount = Integer.parseInt(command.split(" ")[2]);
        String foundHops = command.split(" ")[5];

        System.out.println(fileCount + " results found from " + founder + " in " + foundHops + " hops");

        if (fileCount > 0 && cleanedMessage.size() > 1) {
            System.out.println("Founded File Names:");
            for (int i = 1; i < cleanedMessage.size(); i++) {
                System.out.print(cleanedMessage.get(i) + " ");
            }
            System.out.println("\n----------------------------------------------------");
        }
    }

    private ArrayList<String> removeSpaces(String[] splitReceivedMsg) {
        ArrayList<String> fileLst = new ArrayList<>();
        for (String i : splitReceivedMsg) {
            if (!i.trim().equals(""))
                fileLst.add(i.trim());
        }

        return fileLst;
    }

    private void handleSearch(Node searcher, String receivedMessage) {
        String[] splitReceivedMsg = receivedMessage.split("\"");
        String fileName = splitReceivedMsg[1].trim();
        String hops = splitReceivedMsg[2].trim();
        int newHops = Integer.parseInt(hops) + 1;

        Set<String> myFoundFiles = new HashSet<>();

        //search through all of my resources
        System.out.println("Searching the file: " + fileName + " in my resources..");
        for (String myFileName : myResources) {
            for (String word : fileName.split(" ")) {
   /*              check for each word of the given file name within my file name
                If contains, add to the myFoundFiles and jump to searching within the next my file*/
                if (myFileName.toLowerCase().contains(word.toLowerCase())) {
                    myFoundFiles.add(myFileName);
                    break;
                }
            }
        }

//        If no files found
        if (myFoundFiles.isEmpty()) {
            //If hop limit of flooding not exceeded, ask neighbours to search
            if (newHops <= 4) {
                System.out.println("I do not have the file. Asking neighbours to find the file: " + fileName);
                this.askNeighboursToSearch(fileName, searcher, String.valueOf(newHops));
            } else { // If hop count limit exceeded, send no files found to the searcher
                System.out.println("I do not have the file: " + fileName + ". Hop count limit exceeded");
                sendSearchResultToSearcher(myFoundFiles, searcher, String.valueOf(newHops));
            }
        } else { // If files found
            System.out.println("I have search results matching with requested file: " + fileName);
            sendSearchResultToSearcher(myFoundFiles, searcher, String.valueOf(newHops));
        }
    }

    private void askNeighboursToSearch(String searchFileName, Node searcher, String hops) {
        String message = MessageCodesEnum.SER + " " + searcher.getMyIP() + " " + searcher.getMyPort() + " \"" + searchFileName + "\" " + hops;
        message = MessageUtil.setMessageSend(message);
        try {
            for (Node myNeighbour : myNeighbours) {
                if (!myNeighbour.compareMeWithAnotherNode(searcher)) {
                    System.out.println("[SER] request for file: " + searchFileName + " to " + myNeighbour);
                    ds.send(MessageUtil.createDataPacketFormattedMsg(message, myNeighbour.getMyIP(), myNeighbour.getMyPort()));
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }


    private void sendSearchResultToSearcher(Set<String> myFoundFiles, Node searcher, String hops) {
        String allFilesString = "";
        if (!myFoundFiles.isEmpty()) {
            StringBuilder files = new StringBuilder();
            for (String result : myFoundFiles) {
                files.append("\"").append(result).append("\" ");
            }
            allFilesString = files.toString();
        }

        String message = MessageCodesEnum.SEROK + " " + myFoundFiles.size() + " " + myIP + " " + myPort + " " + hops + " " +
                allFilesString;
        try {
            System.out.println("[SEROK] to " + searcher + ". " + myFoundFiles.size() + " files found.");
            ds.send(MessageUtil.createDataPacket(message, searcher.getMyIP(), searcher.getMyPort()));
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void showMyResourcesList() {
        System.out.println("Files List at " + myIP + "/" + myPort);
        System.out.println("==========================================================");
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
            join();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregister() {

        try {
            ds = new DatagramSocket();
            String message = MessageCodesEnum.UNREG + " " + myIP + " " + myPort + " " + myUsername;
            ds.send(MessageUtil.createDataPacket(message, BOOTSTRAP_SERVER_IP, BOOTSTRAP_SERVER_PORT));

            //handle response by server
            byte[] buffer = new byte[512];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            ds.receive(response);      //get the server response
            String responseMsg = new String(buffer, 0, response.getLength());
            String[] responseMsgArr = responseMsg.split(" ");

            if (MessageCodesEnum.valueOf(responseMsgArr[1]).equals(MessageCodesEnum.UNROK)) {
                if (responseMsgArr[2].equals("0"))
                    log.info(myIP + ":" + myPort + " unregister successfully");
                else
                    log.info("Unregistered successfully!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void join() {
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
                System.out.println(responseCode + " Node Registration Failed. The BootStrap server is filled.");
            } else if (responseCode == 0) {
                System.out.println("Nodes in the network: " + responseCode + " Node registration successful" +
                        ". No other nodes available in the network yet");
            } else {
                System.out.println("Node in the network: " + responseCode + " Node registration successful.");

                for (int i = 3; i < splitResponse.length; i += 2) {
                    String neighbourIp = splitResponse[i];
                    int neighbourPort = Integer.parseInt(splitResponse[i + 1]);

                    if (!isNeighbour(neighbourIp, neighbourPort)) // Will have to remove this check redundant
                        addToMyRoutingTable(new Node(neighbourIp, neighbourPort), false);
                }
                for (Node i : myNeighbours)
                    System.out.println(myPort + ": Neighbours" + i.toString());
            }
        }
    }

    public void addToMyRoutingTable(Node node, boolean fromGossip) {

        if (isNeighbour(node.getMyIP(), node.getMyPort())) {
//            log.warn(node + " is already a neighbour of " + this);
        } else {
            this.myNeighbours.add(node);
            if (fromGossip) {
                System.out.println("Node IP " + node.getMyIP() + " Port " + node.getMyPort() + " was added by Gossip");
            } else {
                System.out.println(node + " was added to the routing table of " + this);
            }
        }
    }

    public void printMyRoutingTable() {
        System.out.println("============My Neighbours===========");
        for (Node myNeighbour : myNeighbours) {
            System.out.println(myNeighbour.toString());
        }
    }

    public void sendMyNeighbours(Node sender) {
        addToMyRoutingTable(sender, false);

        if (this.myNeighbours.size() > 1) {
            StringBuilder nodesToSend = new StringBuilder();
            int noOfNodesToSend = 0;
            for (Node neighbour : this.myNeighbours) {
                if (!neighbour.compareMeWithAnotherNode(sender)) {
                    nodesToSend.append(neighbour.getMyIP()).append(" ").append(neighbour.myPort).append(" ");
                    noOfNodesToSend++;
                }
            }
            String message = MessageCodesEnum.GOSSIPOK + " " + myIP + " " + myPort + " " + noOfNodesToSend + " "
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

            log.info("[GOSSIPOK] Trying to add the sender " + sender);
            addToMyRoutingTable(sender, true);

            for (int i = 0; i < noOfReceivedNodes; i++) {
                Node receivedNode = new Node(st.nextToken(), Integer.parseInt(st.nextToken()));

                if (!isBlacklisted(receivedNode)) {
                    log.info("[GOSSIPOK] Trying to add receiving neighbour " + receivedNode);
                    addToMyRoutingTable(receivedNode, true);
                } else {
                    log.warn("Receiving neighbour " + receivedNode + " is blacklisted");
                }
            }
        } else {
            log.warn(this + " already has 3 neighbours");
        }
    }

    public void handleHeartBeat(Node sender) {
        String message = MessageCodesEnum.ACTIVE + " " + this.myIP + " " + this.myPort;
        try {
            ds.send(MessageUtil.createDataPacket(message, sender.getMyIP(), sender.getMyPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addActiveNeighbours(Node sender) {
        if (!isActiveNeighbour(sender)) {
            this.myActiveNeighbours.add(sender);
//            log.info("Added " + sender + "to Active Neighbour List");
        } else {
//            log.warn(sender + " already in Active Neighbour List");
        }
    }


    public void search(String searchFileName) {
        String message = MessageCodesEnum.SER + " " + myIP + " " + myPort + " \"" + searchFileName + "\" 0";
        message = MessageUtil.setMessageSend(message);
        try {
            for (Node myNeighbour : myNeighbours) {
                ds.send(MessageUtil.createDataPacketFormattedMsg(message, myNeighbour.getMyIP(), myNeighbour.getMyPort()));
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void leave() {
        try {
            String request = MessageCodesEnum.LEAVE + " " + myIP + " " + myPort;

            //send neighbours about leave
            for (Node node : myNeighbours) {
                ds.send(MessageUtil.createDataPacket(request, node.getMyIP(), node.getMyPort()));
            }
            myNeighbours.clear();   //clear my neighbours
            unregister();           //unregister from server
            System.exit(1);  //kill current node
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void download(String ip, String port, String fileName) {
        log.info("Requesting {} file...", fileName);
        try {
            //create request URL to download files
            URL url = new URL("http://" + ip + ":" + port + "/files/download/" + fileName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            //set connection timeouts
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            // Retrieving the response and creating the content
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null) {
                content.append(inputLine);
            }
            bufferedReader.close();

            if (content.toString().length() > 0) {
//                String workingDirectory = System.getProperty("user.dir");
//                String target = workingDirectory + "\\src\\main\\resources\\static\\downloadedFiles\\" +
//                        fileName.replace("%20", " ") + ".txt";

                String target ="downloadedFiles/" +
                        fileName.replace("%20", " ") + ".txt";
                FileOutputStream fileOutputStream = new FileOutputStream(target);
                fileOutputStream.write(content.toString().getBytes());

                Scanner scanner = new Scanner(new FileReader(target));
                StringBuilder fileRead = new StringBuilder();
                while (scanner.hasNext()) {
                    fileRead.append(scanner.next());
                }
                scanner.close();
                log.info("Successfully downloaded the file!");

                //calculate the hash
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(fileRead.toString().getBytes(StandardCharsets.UTF_8));
                String encoded = Base64.getEncoder().encodeToString(hash);
                log.info("Downloaded file hash:" + encoded);
            } else {
                log.warn("No file received! ");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void addBlacklistNode(Node node) {
        if (isBlacklisted(node)) {
            log.warn(node + " Already in the blacklist");
        } else {
            this.myBlacklist.add(node);
            log.info(node + " Added to blacklist");
        }
    }


}



