package client;

public class CommandHandler {
    private final Node node ;

    public CommandHandler(Node node) {
        this.node = node;
    }

    public void execute(String command) {
        switch (command.split(" ")[0]) {
            case "routing":
                node.printMyRoutingTable();
                break;
            case "unregister":
                node.unregister();
                break;
            case "register":
                node.register();
                break;
            case "join":
                node.join();
                break;
            case "search": {
                    String[] commandArray = command.split(" ");
                    StringBuilder fileName = new StringBuilder();
                    for (int i = 1; i < commandArray.length; i++)
                        fileName.append(" ").append(commandArray[i]);
                    System.out.println("Searching file: " + fileName.toString().trim());
                    node.search(fileName.toString().trim());
                }
                break;
            case "files":
                node.showMyResourcesList();
                break;
            case "leave":
                node.leave();
                break;
            case "download": {
                    String[] commandArray = command.split(" ");
                    String reqIp = commandArray[1];
                    String reqPort = commandArray[2];
                    StringBuilder fileName = new StringBuilder();
                    for (int i = 3; i < commandArray.length; i++) {
                        if (i == commandArray.length - 1) {
                            fileName.append(commandArray[i]);
                        } else {
                            fileName.append(commandArray[i]).append("%20");
                        }
                    }
                    node.download(reqIp, reqPort, fileName.toString());
                }
                break;
            case "test": {
                node.runTestQueries();
            }
            break;
            default:
                System.out.println("Invalid Command");
        }
    }
}
