/**
 * @author Bharat M Bhasvar, Braden Herndon, Aditya Borde
 * @version 1.0
 * @since 04/05/2016
 * 
 * This is simulator for FloodMax Leader election algorithm
 * which elects leader among given nodes in bidirectional graph.
 * This is simulation considering asynchronous network with bounded
 * time for message to reach destination. Master is responsible to
 * generate rounds for each node. Each round may or may not consists
 * of receive, process and send event for the node due to asynchronous
 * nature of the architecture. In end, everybody knows about the leader
 * and their parent node and leader declares himself as leader of the network.
 */


import java.io.*;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Master {

    static Synchronizer sync;
    static CyclicBarrier cb;

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {

        Scanner in = null;

        try {
            in = new Scanner(new File("connectivity.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Read number of processes and create a array of the given ids in the input file.
        int n = in.nextInt();
        int[] idArray = new int[n];
        for (int i = 0; i < n; i++) {
            idArray[i] = in.nextInt();
        }
        // Synchronization
        /** 
         * We create an array to store our nodes, and use a custom object called sync to handle 
         * "finished" messages, and a cyclic barrier which will be passed to each node to
         *	handle round synchronization with master.
         **/
        Node[] nodes = new Node[n];
        sync = new Synchronizer();
        cb = new CyclicBarrier(n + 1); // cb waits 

        // Node process creation
        /* Create a new node for each process id in the id list. */
        for (int i = 0; i < n; i++) {
            nodes[i] = new Node(idArray[i], cb, sync);
        }


        // Graph connectivity
        /** 
         * Each node in the graph has a list of outbound and inbound connections. We use the connectivity
         *	information in the input file to systematically add connections to the corresponding in and outbound
         *	lists for each node.
         **/
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int connect = in.nextInt();
                if (connect == 1 && i != j) {
                    MessageQueue q = new MessageQueue("+", nodes[i].pid, nodes[j].pid);
                    nodes[i].outboundList.add(q); // add(otherid : q)
                    nodes[j].inboundList.add(q);
                }
            }
        }
        // Start all processes
        for (int i = 0; i < n; i++) {
            new Thread(nodes[i]).start();
        }
        // Main master loop
        /**
         *  The synchronizer tracks whether the leader election is finished or not. While the algorithm is running,
         *	the master node waits for all of the spawned processes to finish their round and wait on the cyclic barrier.
         *	Once that is done, master waits on the barrier and the barrier breaks, starting the next round.
         **/
        
        while (sync.running) {
            if (sync.get() >= n) {
                sync.running = false;
            }
            if (cb.getNumberWaiting() == n) {
                cb.await();
            }
        }
        System.out.println("Complete.");
        System.exit(0);
    }

}

