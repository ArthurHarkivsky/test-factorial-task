package task;

import dto.CalculationContext;
import dto.InputItem;
import dto.ResultItem;
import math.CachedMath;

import java.math.BigInteger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class FactorialCalculationTask implements Runnable {
    private static final int POLL_TIMEOUT_MS = 50;
    private static final int MAX_CONCURRENT_CALCULATIONS = 100;
    private final CalculationContext context;

    public FactorialCalculationTask(CalculationContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            Semaphore rateLimiter = new Semaphore(MAX_CONCURRENT_CALCULATIONS);
            AtomicInteger activeCalculations = new AtomicInteger(0);

            while (!areTasksCompleted()) {
                InputItem task = context.taskQueue().poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (task != null) {
                    rateLimiter.acquire();
                    activeCalculations.incrementAndGet();

                    //noinspection resource
                    context.pool().submit(() -> {
                        try {
                            BigInteger result = CachedMath.calculateFactorial(task.number());
                            context.resultQueue().put(new ResultItem(task.index(), task.number(), result));
                            context.taskCount().decrementAndGet();
                            activeCalculations.decrementAndGet();
                            // To maintain the rate limit
                            Thread.sleep(1000);
                            rateLimiter.release();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                logProgressPeriodically(activeCalculations);
            }

            waitForStragglersToComplete(activeCalculations);

            context.taskComplete().countDown();
            System.out.println("All calculations completed");

        } catch (InterruptedException e) {
            System.err.println("Calculation manager interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void waitForStragglersToComplete(AtomicInteger activeCalculations) throws InterruptedException {
        while (activeCalculations.get() > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(POLL_TIMEOUT_MS));
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private boolean areTasksCompleted() {
        return context.taskQueue().isEmpty() && context.taskCount().get() == 0;
    }

    private void logProgressPeriodically(AtomicInteger activeCalculations) {
        if (context.taskCount().get() % 1000 == 0 && context.taskCount().get() > 0) {
            System.out.printf("""
                    Calculation progress
                    Remaining tasks: %d
                    Active calculations: %d""", context.taskCount().get(), activeCalculations.get());
        }
    }
}