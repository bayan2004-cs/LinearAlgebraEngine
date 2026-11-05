package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            double fatigueFactor=0.5+Math.random();
            TiredThread Nt = new TiredThread(i,fatigueFactor);
            workers[i]=Nt;
            idleMinHeap.add(Nt);
            Nt.start();
            
        }
    }

    /*Thread Safety - Why increment BEFORE submission:
     * If we incremented after newTask(), a fast task could:
     * 1. Complete and decrement inFlight (now -1 if we hadn't incremented)
     * 2. Call notifyAll
     * 3. Then we increment (now 0, but too late - waiter already woken incorrectly)*/
    public synchronized void submit(Runnable task)  {
        try {
        while (idleMinHeap.isEmpty()) {
         wait();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    TiredThread leastT=idleMinHeap.poll();
    Runnable wrappedTask = () -> {
    try {
        task.run();
    } catch (Exception e) {
        System.err.println("Task failed: " + e.getMessage());
    } finally {
        this.taskFinished(leastT); 
    }
    };
    inFlight.incrementAndGet();
    leastT.newTask(wrappedTask);
    };

    public  void submitAll(Iterable<Runnable> tasks)  {
        for (Runnable runnable : tasks) {
            submit(runnable);
        }
        synchronized(this){
        while (inFlight.get()>0) {
           try {
            this.wait();
           } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
           } 
        }
        }
    }

    public void shutdown() throws InterruptedException {
        for (TiredThread tW : workers) {
            tW.shutdown();
        }
    }

    public synchronized String getWorkerReport() {
         String str ="";
        double sumFatigue = 0.0;
        for (TiredThread worker : workers) {
            str += "WorkerId: "+ worker.getWorkerId() +" Work: "+ worker.getTimeUsed() + 
            " Idle: "+ worker.getTimeIdle() + " Fatigue: "+ worker.getFatigue() +"\n";
        
            sumFatigue += worker.getFatigue();
        }
        double AverageFatigue = sumFatigue / workers.length;
        double fairnessFactor = 0.0;

        for (TiredThread worker : workers) {
            fairnessFactor += Math.pow(worker.getFatigue() - (AverageFatigue), 2);
        }

        str += "Fairness Factor: " + fairnessFactor + "\n";
        return str;
    }
    
    public synchronized void taskFinished(TiredThread worker) {
    idleMinHeap.add(worker); 
    inFlight.getAndDecrement(); 
    notifyAll(); 
}
     
}