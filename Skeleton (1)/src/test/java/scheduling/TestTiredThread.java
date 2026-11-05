package scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestTiredThread {

    private TiredThread worker;
    private final double TEST_FATIGUE_FACTOR = 1.2;

    @BeforeEach
    void setUp() {
        // [cite: 312, 326] Each worker is an instance of TiredThread with a fatigue factor
        worker = new TiredThread(1, TEST_FATIGUE_FACTOR);
    }

    @Test
    @DisplayName("Verify Task Execution and Busy State")
    void testTaskExecution() throws InterruptedException {
        worker.start();
        AtomicBoolean taskRun = new AtomicBoolean(false);
        
        // [cite: 313, 342] Tasks are standard Runnable objects
        Runnable task = () -> {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            taskRun.set(true);
        };

        worker.newTask(task);
        
        // Small sleep to ensure the thread has transitioned to busy
        Thread.sleep(50);
        assertTrue(worker.isBusy(), "Worker should be busy while executing a task.");
        
        // Wait for completion
        Thread.sleep(100);
        assertFalse(worker.isBusy(), "Worker should not be busy after task completion.");
        assertTrue(taskRun.get(), "The task logic should have been executed.");
        
        worker.shutdown();
        worker.join();
    }

    @Test
    @DisplayName("Verify Fatigue Calculation Logic")
    void testFatigueCalculation() throws InterruptedException {
        worker.start();
        
        // Simulating a task that takes time
        long workTimeMillis = 200;
        worker.newTask(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < workTimeMillis) {
                // Busy wait to consume time
            }
        });

        Thread.sleep(300); // Wait for task to finish
        
        // [cite: 330, 331] Fatigue = fatigueFactor * timeUsed
        long timeUsed = worker.getTimeUsed();
        double expectedFatigue = TEST_FATIGUE_FACTOR * timeUsed;
        
        assertTrue(timeUsed > 0, "timeUsed should be greater than zero after a task.");
        assertEquals(expectedFatigue, worker.getFatigue(), 0.001, "Fatigue calculation is incorrect.");
        
        worker.shutdown();
        worker.join();
    }

    @Test
    @DisplayName("Verify Idle Time Tracking")
    void testIdleTimeTracking() throws InterruptedException {
        worker.start();
        
        // [cite: 328] Separately tracks idle periods as timeIdle
        Thread.sleep(200); // Let the worker sit idle
        
        // Send a quick task to trigger the timeIdle update in the run loop
        worker.newTask(() -> {});
        Thread.sleep(50);
        
        assertTrue(worker.getTimeIdle() > 0, "timeIdle should accumulate during inactivity.");
        
        worker.shutdown();
        worker.join();
    }

    @Test
    @DisplayName("Verify Handoff Restriction (One-element queue)")
    void testHandoffRestriction() {
        // [cite: 322] Places the Runnable into the worker's one-element handoff queue
        worker.newTask(() -> { try { Thread.sleep(1000); } catch (InterruptedException e) {} });
        
        // If the handoff is full, it should throw IllegalStateException as per your code
        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {});
        }, "Should throw IllegalStateException when handoff is full.");
    }

    @Test
    @DisplayName("Verify Thread Shutdown via Poison Pill")
    void testShutdown() throws InterruptedException {
        worker.start();
        assertTrue(worker.isAlive());

        // [cite: 323] Shutdown by sending a designated poison pill
        worker.shutdown();
        
        worker.join(1000); // Wait for thread to finish
        assertFalse(worker.isAlive(), "Thread should exit its run loop after shutdown.");
    }

    @Test
    @DisplayName("Verify Comparable Logic (Fairness)")
    void testComparison() {
        TiredThread freshWorker = new TiredThread(2, 1.0);
        
        // [cite: 317, 378] Workers should be ordered by fatigue for scheduling
        // Manual check of compareTo logic
        assertTrue(worker.compareTo(freshWorker) == 0, "Initially both workers have 0 fatigue.");
    }
}