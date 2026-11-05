package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {
    /*  Thread Safety:
 * - AtomicBoolean for alive and busy flags
 * - AtomicLong for timing statistics (timeUsed, timeIdle, idleStartTime)
 * - ArrayBlockingQueue for thread-safe task handoff
 * - Stats are updated BEFORE busy flag is cleared for consistent reads
 */

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public synchronized void newTask(Runnable task) {
       if (!handoff.isEmpty()) {
        throw new IllegalStateException(" the worker is not ready to accept a task ");
       }
       handoff.add(task);
       notifyAll();
    }
  /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown()  {
   this.alive.set(false);
    
    try {
        handoff.put(POISON_PILL); 
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    synchronized (this) {
        this.notifyAll(); 
    }
    }

    @Override
    public void run() {
        while (alive.get()) {
            Runnable currentTask;
            try {
                currentTask = handoff.take(); 
                long startTime = System.nanoTime();
                timeIdle.addAndGet(startTime - idleStartTime.get());
                if (currentTask == POISON_PILL) {
                     alive.set(false);
                    break;
                }
                busy.set(true); 
                try {
                    currentTask.run();
                } finally {
                    long endTime = System.nanoTime();
                    timeUsed.addAndGet(endTime - startTime);
                    idleStartTime.set(endTime);
                    busy.set(false);
                }
            } catch (InterruptedException e) {
                // Exit if interrupted during shutdown; otherwise keep interrupt status and loop
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public int compareTo(TiredThread o) {
        // Done
        if(o.getFatigue()< this.getFatigue()){
            return 1;
        }
        else if ( o.getFatigue() > this.getFatigue()){
            return -1;
        }
        else {
            return 0;
        }
    }
}