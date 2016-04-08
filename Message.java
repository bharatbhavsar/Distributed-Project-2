/**
 * @author Bharat M Bhasvar, Braden Herndon, Aditya Borde
 * @version 1.0
 * @since 04/05/2016
 * 
 * We store several pieces of information in the message object. The origin of the message,
 * the largest id that the origin of the message has seen, the type of message ('ack', 'nack', etc),
 * and a random value between 1 and 20 which will be used as the asynchronous delay simulation once
 * the message is in the message queue.
 */ 

import java.util.Random;


public class Message {

    public int ownId;
    public int maxIdKnown;
    public String type;
    public int randomTime;

    
    /**
     * Constructor to create message
     * @param ownId
     * @param maxIdKnown
     * @param type
     */
    public Message(int ownId, int maxIdKnown, String type) {
        this.ownId = ownId;
        this.maxIdKnown = maxIdKnown;
        this.type = type;
        Random rand = new Random();
        int randomInt = rand.nextInt(20);
        randomInt++;
        this.randomTime = randomInt;
    }


}

