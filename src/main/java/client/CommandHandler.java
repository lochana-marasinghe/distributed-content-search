package client;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CommandHandler {
    private Node node ;

    public CommandHandler(Node node) {
        this.node = node;
    }

    public void execute(String command) throws IOException, NoSuchAlgorithmException {
        switch (command.split(" ")[0]) {
            case "join":
                try {
                    node.join();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Invalid Command");
        }
    }
}
