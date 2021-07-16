package client;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Slf4j
public class NeighbourMaintainer extends Thread {
    private final Node node;
    private int counter;

    public NeighbourMaintainer(Node receivedNode) {
        node = receivedNode;
    }

    @Override
    public void run() {
        runScheduledNeighbourMaintainer();
    }

    private void runScheduledNeighbourMaintainer() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                removeInActiveNeighbours();
            }
        };
        int CHECKER_PERIOD = 30000;
        int THREAD_STARTING_DELAY = 3000;
        timer.schedule(task, THREAD_STARTING_DELAY, CHECKER_PERIOD);
    }

    private void removeInActiveNeighbours() {
        counter ++ ;
        if (node.getMyActiveNeighbours().size() > 0) {
            ArrayList<Node> updatedNeighbourArrayList = this.node.getMyNeighbours().stream()
                    .filter(this.node.getMyActiveNeighbours()::contains).collect(Collectors.toCollection(ArrayList::new));

            ArrayList<Node> nodesToBlacklisted = this.node.getMyNeighbours().stream()
                    .filter(node -> !this.node.getMyActiveNeighbours().contains(node))
                    .collect(Collectors.toCollection(ArrayList::new));

//            log.info("Removing inactive neighbours");
            this.node.setMyNeighbours(updatedNeighbourArrayList);

//            log.info("Updating the blacklist");

            for (Node blacklistedNode : nodesToBlacklisted) {
                this.node.addBlacklistNode(blacklistedNode);
            }
//            log.info("Clearing the active neighbours list");
            this.node.setMyActiveNeighbours(new ArrayList<>());

        } else {
//            log.warn("[Active Checker] " + node + " does not have any active nodes");
        }

        if (counter == 5 ) {
//            log.info("Resetting blacklist counter");
            this.node.setMyBlacklist(new ArrayList<>());
        }
    }
}
