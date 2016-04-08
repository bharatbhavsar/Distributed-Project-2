/**
 * @author Bharat M Bhasvar, Braden Herndon, Aditya Borde
 * @version 1.0
 * @since 04/05/2016
 *
 * This is actual asynchronous floodMax algorithm implementation
 * which will run at each node. Every round is consists of Receive, Process
 * and reply of message except first round where every node first sends own ID
 * to each neighbor and then starts processing messages as and when they are
 * arrived at node. In the end, only leader will declare itself and master will
 * stop rounds.
 * 
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class Node implements Runnable{

    public ArrayList<MessageQueue> outboundList;
    public ArrayList<MessageQueue> inboundList;
    private ArrayList<Message> inbox;
    private ArrayList<Integer> children;
    int pid;
    int maxIdKnown;
    int parent;
    boolean active;
    boolean isIncremented = false;
    boolean isSent = false;

    CyclicBarrier cb;
    Synchronizer sync;
    boolean firstRun = true;
    int ackNackCounter = 0;
    Set<Integer> setAck; // This set is used to track that a process isn't sending duplicate acks for the same maxId.

    public Node(int pid, CyclicBarrier cb, Synchronizer sync) {
        this.pid = pid;
        this.maxIdKnown = pid;
        this.parent = -1;
        this.outboundList = new ArrayList<>();
        this.inboundList = new ArrayList<>();
        this.cb = cb;
        this.sync = sync;
        this.inbox = new ArrayList<>();
        this.children = new ArrayList<>();
        this.setAck = new HashSet<>();
    }

    /* The run function of the process is made up of 3 phases.
    
    The first time the process runs, the initial phase in which the first messages are broadcasted is run. 
    This initial phase does not go again.
    
    The receive phase is the phase in which the process checks all of its incoming message queues from neighboring processes.
    It stores these messages in its inbox, waiting to be processed in the processing phase.
    
    The process phase is the bulk of the actual algorithm implementation. This is where the process decides what to do with 
    a message depending on its type and the information it holds. Further details below in the actual implementation.
    */
    public void run() {
        while (sync.running) {
            if (firstRun) {
                initialPhase();
                firstRun = false;
            }

            receivePhase();
            processPhase();
            try {
                cb.await(); // At the end of the node's phases, it calls await on the cb which stops the node until the next round.
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
        System.out.println();
    }

    /* The initial phase, which is only run once, is used to send initial id messages to
    all neighbors. */
    private void initialPhase() {
        Message initMessage = new Message(pid, pid, "init");
        for(int i=0; i<outboundList.size(); i++){
            outboundList.get(i).send(initMessage);
        }
    }

    
    /**
     * A general purpose function for broadcasting a message to all neighbors. The noSend
     * argument chooses the node that the message should not be sent to. In this case, if a node
     * updates its id and sends a message out, it doesn't need to send a message to the process from
     * which it updated its id, so that parent process would be the noSend value.
     * 
     * @param m
     * @param noSend
     */
    private void sendAll(Message m, int noSend) {
        for(int i = 0; i < outboundList.size(); i++) {
            MessageQueue queue = outboundList.get(i);
            if (queue.destination != noSend) {
                queue.send(m);
            }
        }
    }

    /* A general purpose function for sending a specific message to a specific target. Used for
    sending our ack and nack messages between processes. */
    private void sendAckNack(Message m, int target) {
        for (MessageQueue queue : outboundList) {
            if (queue.destination == target) {
                queue.send(m);
            }
        }
    }

    /* In the receive phase, each node will decrement the time of all messages in the message queue 
    as part of the asynchronous simulation, and will then "download" messages from its inbound queues
    into the "inbox" for this round. */
    private void receivePhase() {
        for(int i=0; i<inboundList.size(); i++){
            inboundList.get(i).decrementTime();
        }
        for(int i=0; i<inboundList.size(); i++) {
            Message m = inboundList.get(i).receive();
            if (m != null) {
                inbox.add(m);
            }
        }
    }

    /* In the process phase, each node loops through the inbox it populated during its receive phase
    and performs the correct action. If the message is an init type, then the message checks if its id is larger
    than its own. If it is, the process sends an ack, declares the origin of the message its parent,
    and broadcasts its new status to its neighbors. If the message is from a process with a smaller id, the process 
    simply sends a nack message and does nothing else.
    
    If the message is an ack, the process adds the origin of that ack to its child list and increases its acknack 
    counter, which we use to track the number of messages that have been responded to by the processes neighbors.
    
    If the message is an nack, the process simply increments its acknack counter. Note that for both ack and nack,
    we check that the maxIdKnown for the message is the same as the maxIdKnown for the checking process to insure
    we are dealing with a message with the same information. */
    
    private void processPhase() {
        if (!inbox.isEmpty()) {
            for (int i = 0; i < inbox.size(); i++) {
                Message m = inbox.get(i);
                String type = m.type;

                if (type.equals("init")) {
                    if (this.maxIdKnown < m.maxIdKnown) {

                        this.maxIdKnown = m.maxIdKnown;
                        this.parent = m.ownId;
                        this.children.clear();
                        sendAll(new Message(this.pid, this.maxIdKnown, "init"), this.parent);
                        ackNackCounter = 0;
                    } else if (this.maxIdKnown >= m.maxIdKnown) {
                        Message nack = new Message(this.pid, m.maxIdKnown, "nack");
                        sendAckNack(nack, m.ownId);
                    }

                } else if (type.equals("ack")) {
                    if (this.maxIdKnown == m.maxIdKnown) {
                        ackNackCounter++;
                        children.add(m.ownId);
                    }
                }else if (type.equals("nack")) {
                    if (this.maxIdKnown == m.maxIdKnown) {
                        ackNackCounter++;
                    }

                }
                inbox.remove(i);
            }
            ackNack();
        }
    }

    /* The acknack function is called at the end of every process phase. This is where the process checks if 
    it is done or not. If it has a parent and it's received messages from all its neighbors, and it hasn't 
    already sent an ack for the maxId it knows, it will send an ack to its parent. It also changes a boolean
    flag to insure it doesn't increment the synchronizer's process tracker again,
    
    If it doesn't have a parent and it's received messages from all its neighbors, then it elects itself leader.
    */
    private void ackNack(){
        if (parent != -1 && (ackNackCounter == (outboundList.size() - 1))) {

            if(setAck.add(this.maxIdKnown)){
                System.out.println(this.pid +"-->"+this.parent +" MAX="+ this.maxIdKnown);
                sendAckNack(new Message(this.pid, this.maxIdKnown, "ack"), this.parent);
            }
            if(!isIncremented){
                sync.increment();
                isIncremented = true;
            }
        } else if (parent == -1 && (ackNackCounter == outboundList.size())) {
            System.out.println(this.pid + " is the leader.");
            sync.increment();
        }
    }
}
