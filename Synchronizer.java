
/**
 * @author Bharat M Bhasvar, Braden Herndon, Aditya Borde
 * @version 1.0
 * @since 04/05/2016
 * 
 * The synchronizer simply stores the number of processes that are finished with their tasks,
 * and also contains a boolean running variable which is used by master and nodes to terminate
 * once the algorithm elects a leader.
 *
 */

import java.util.concurrent.atomic.AtomicInteger;

public class Synchronizer {
    
    private AtomicInteger counter = null; // We use an atomic integer to guarantee atomicity between process accessing the same structure.
    public boolean running;

    public Synchronizer() {
        this.running = true;
        this.counter = new AtomicInteger();
    }

    // It's important to use synchronized functions to avoid thread-unsafe accesses to the synchronizer.
    public synchronized void increment() {
        this.counter.incrementAndGet();
    }

    public synchronized int get() {
        return counter.intValue();
    }
}
