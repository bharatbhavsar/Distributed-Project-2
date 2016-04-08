/**
 * @author Bharat M Bhasvar, Braden Herndon, Aditya Borde
 * @version 1.0
 * @since 04/05/2016
 *  
 *  A message queue is composed of the actual queue datastructure, the queue's id used for testing
 *  purposes, and the origin and destination of the queue. In this way, each node is connected by two queues,
 *  each traveling in either direction.
 */

import java.util.LinkedList;
import java.util.Queue;


public class MessageQueue {

    private Queue<Message> queue;
    String queueId;
    int origin;
    int destination;

    /**
     * Constructor to initiate the message queue between source and destination
     * @param queueId
     * @param origin
     * @param destination
     */
    public MessageQueue(String queueId, int origin, int destination) {
        this.queue = new LinkedList<>();
        this.queueId = queueId;
        this.origin = origin;
        this.destination = destination;
    }

    /* Simply add a message to the queue in an atomic, synchronized way. */
    public synchronized void send(Message t) {
        queue.add(t);
    }

    /* Return a message in the queue provided that that queue's first message has time 0 and is 
    ready to be delivered. */
    public synchronized Message receive() {
        if (!queue.isEmpty()) {
            Message m = queue.peek();
            if (m.randomTime == 0) {
                m = queue.poll();
                return m;
            }
        }
        return null;
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /* Globally decrement the time values of all of our messages. This is the core
    component of the asynchronous simulation. */
    public synchronized void decrementTime() {
        for (Message m : queue) {
            if (m.randomTime > 0) {
                m.randomTime--;
            }
        }
    }
}

