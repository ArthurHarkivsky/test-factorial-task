import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FactorialFileProcessor {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of calculation threads (N): ");
        int threadCount = scanner.nextInt();
        scanner.close();

        BlockingQueue<InputItem> taskQueue = new LinkedBlockingQueue<>();
        BlockingQueue<ResultItem> resultQueue = new LinkedBlockingQueue<>();
        ConcurrentMap<Integer, ResultItem> orderedResults = new ConcurrentHashMap<>();
        AtomicInteger nextIndexToWrite = new AtomicInteger(0);
        AtomicInteger taskCount = new AtomicInteger(0);
        CountDownLatch calculationComplete = new CountDownLatch(1);

        Thread readerThread = new Thread(new ReaderTask(taskQueue, taskCount), "ReaderThread");
        readerThread.start();

        ExecutorService calculatorPool = Executors.newFixedThreadPool(threadCount);
        Thread calculationManager = new Thread(new CalculationManager(
                calculatorPool, taskQueue, resultQueue, taskCount, calculationComplete), "CalculationManager");
        calculationManager.start();

        Thread writerThread = new Thread(new WriterTask(
                resultQueue, orderedResults, nextIndexToWrite, calculationComplete), "WriterThread");
        writerThread.start();

        try {
            readerThread.join();
            calculationManager.join();
            writerThread.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            calculatorPool.shutdownNow();
        }

        System.out.println("Factorial calculation completed!");
    }

    private static class InputItem {
        final int index;
        final int number;

        public InputItem(int index, int number) {
            this.index = index;
            this.number = number;
        }
    }

    private static class ResultItem {
        final int index;
        final int number;
        final BigInteger result;

        public ResultItem(int index, int number, BigInteger result) {
            this.index = index;
            this.number = number;
            this.result = result;
        }
    }

    private static class ReaderTask implements Runnable {
        private final BlockingQueue<InputItem> taskQueue;
        private final AtomicInteger taskCount;

        public ReaderTask(BlockingQueue<InputItem> taskQueue, AtomicInteger taskCount) {
            this.taskQueue = taskQueue;
            this.taskCount = taskCount;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {
                String line;
                int index = 0;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    try {
                        int number = Integer.parseInt(line);
                        taskQueue.put(new InputItem(index++, number));
                        taskCount.incrementAndGet();
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid number format: " + line);
                    }
                }

                System.out.println("File reading completed. Total tasks: " + taskCount.get());
            } catch (IOException | InterruptedException e) {
                System.err.println("Error in reader thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    // Class that manages the thread pool for calculations
    private static class CalculationManager implements Runnable {
        private final ExecutorService calculatorPool;
        private final BlockingQueue<InputItem> taskQueue;
        private final BlockingQueue<ResultItem> resultQueue;
        private final AtomicInteger taskCount;
        private final CountDownLatch calculationComplete;

        public CalculationManager(
                ExecutorService calculatorPool,
                BlockingQueue<InputItem> taskQueue,
                BlockingQueue<ResultItem> resultQueue,
                AtomicInteger taskCount,
                CountDownLatch calculationComplete) {
            this.calculatorPool = calculatorPool;
            this.taskQueue = taskQueue;
            this.resultQueue = resultQueue;
            this.taskCount = taskCount;
            this.calculationComplete = calculationComplete;
        }

        @Override
        public void run() {
            try {
                Semaphore rateLimiter = new Semaphore(100); // Limit to 100 calculations per second

                while (!areTasksCompleted()) {
                    InputItem task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        rateLimiter.acquire();

                        calculatorPool.submit(() -> {
                            try {
                                BigInteger result = calculateFactorial(task.number);

                                resultQueue.put(new ResultItem(task.index, task.number, result));
                                taskCount.decrementAndGet();

                                // Wait 1 second to maintain the rate limit
                                Thread.sleep(1000);
                                rateLimiter.release();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                }

                calculationComplete.countDown();
                System.out.println("All calculations completed");

            } catch (InterruptedException e) {
                System.err.println("Calculation manager interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        private boolean areTasksCompleted() {
            return taskQueue.isEmpty() && taskCount.get() == 0;
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

    private static class WriterTask implements Runnable {
        private final BlockingQueue<ResultItem> resultQueue;
        private final ConcurrentMap<Integer, ResultItem> orderedResults;
        private final AtomicInteger nextIndexToWrite;
        private final CountDownLatch calculationComplete;

        public WriterTask(
                BlockingQueue<ResultItem> resultQueue,
                ConcurrentMap<Integer, ResultItem> orderedResults,
                AtomicInteger nextIndexToWrite,
                CountDownLatch calculationComplete) {
            this.resultQueue = resultQueue;
            this.orderedResults = orderedResults;
            this.nextIndexToWrite = nextIndexToWrite;
            this.calculationComplete = calculationComplete;
        }

        @Override
        public void run() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
                boolean isCompleted = false;

                while (!isCompleted || !resultQueue.isEmpty() || !orderedResults.isEmpty()) {

                    ResultItem result = resultQueue.poll(100, TimeUnit.MILLISECONDS);

                    if (result != null) {
                        orderedResults.put(result.index, result);
                    }

                    writeOrderedResults(writer);

                    if (!isCompleted) {
                        isCompleted = calculationComplete.await(10, TimeUnit.MILLISECONDS);
                    }
                    System.out.println("Result queue size: " + resultQueue.size());
                }
                System.out.println("All results written to output.txt");
            } catch (IOException | InterruptedException e) {
                System.err.println("Error in writer thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        private void writeOrderedResults(BufferedWriter writer) throws IOException {
            ResultItem nextResult;
            while ((nextResult = orderedResults.remove(nextIndexToWrite.get())) != null) {
                // format: "number = factorial"
                writer.write(nextResult.number + " = " + nextResult.result);
                writer.newLine();
                writer.flush();

                nextIndexToWrite.incrementAndGet();
            }
        }
    }
}