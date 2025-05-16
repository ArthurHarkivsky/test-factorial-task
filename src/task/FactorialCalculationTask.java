package task;

import dto.CalculationContext;
import dto.InputItem;
import dto.ResultItem;

import java.math.BigInteger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FactorialCalculationTask implements Runnable {
    private final CalculationContext context;

    public FactorialCalculationTask(CalculationContext context) {this.context = context;}

    @Override
    public void run() {
        try {
            Semaphore rateLimiter = new Semaphore(100); // Limit to 100 calculations per second

            while (!areTasksCompleted()) {
                InputItem task = context.taskQueue().poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    rateLimiter.acquire();

                    //noinspection resource
                    context.pool().submit(() -> {
                        try {
                            BigInteger result = calculateFactorial(task.number());

                            context.resultQueue().put(new ResultItem(task.index(), task.number(), result));
                            context.taskCount().decrementAndGet();

                            // Wait 1 second to maintain the rate limit
//                            Thread.sleep(1000);
                            rateLimiter.release();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }

            context.taskComplete().countDown();
            System.out.println("All calculations completed");

        } catch (InterruptedException e) {
            System.err.println("Calculation manager interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private boolean areTasksCompleted() {
        return context.taskQueue().isEmpty() && context.taskCount().get() == 0;
    }

    private BigInteger calculateFactorial(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }

        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= number; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
}