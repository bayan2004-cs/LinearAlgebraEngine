package scheduling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestTiredExecutor {

    private TiredExecutor executor;
    private final int NUM_THREADS = 2;

    @BeforeEach
    void setUp() {
        executor = new TiredExecutor(NUM_THREADS);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
    }

    @Test
    @DisplayName("Test Task Submission: Verify tasks are distributed")
    void testSubmitTasks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        
        // Submit tasks equal to the number of threads
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                counter.incrementAndGet();
            });
        }
        // Allow some time for workers to process
        Thread.sleep(300);
        assertEquals(NUM_THREADS, counter.get(), "All submitted tasks should be executed by the pool.");
    }

    @Test
    @DisplayName("Test submitAll: Verify it blocks until batch completion")
    void testSubmitAllBlocking() {
        List<Runnable> tasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                counter.incrementAndGet();
            });
        }

        // This should only return once all 5 tasks are done
        executor.submitAll(tasks);
        
        assertEquals(5, counter.get(), "submitAll should block until the entire batch is completed.");
    }

    @Test
    @DisplayName("Test Executor Blocking: Verify it waits when no threads are idle")
    void testExecutorBlocksWhenFull() throws InterruptedException {
        // Fill all threads with long-running tasks
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            });
        }
        long startTime = System.currentTimeMillis();
        // This submission should block until one of the previous tasks finishes
        executor.submit(() -> {});
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 400, "The executor should have blocked while all threads were busy.");
    }

    @Test
    @DisplayName("Test Worker Report: Verify statistics are collected")
    void testWorkerReport() throws InterruptedException {
        executor.submit(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        });
        
        Thread.sleep(200);
        String report = executor.getWorkerReport();
        
        assertNotNull(report);
        assertTrue(report.contains("WorkerId:"), "Report should contain worker IDs.");
        assertTrue(report.contains("Fatigue:"), "Report should contain fatigue levels.");
    }
}