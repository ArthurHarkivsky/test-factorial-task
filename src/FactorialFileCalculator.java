import configuration.CommandLineTaskConfigurationReader;
import configuration.dto.TaskConfiguration;
import dto.CalculationContext;
import task.FactorialCalculationTask;
import task.ReaderTask;
import task.WriterTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FactorialFileCalculator {

    public static void main(String[] args) {
        TaskConfiguration configuration = new CommandLineTaskConfigurationReader().read();
        try (ExecutorService pool = Executors.newFixedThreadPool(configuration.threadPoolSize())) {
            CalculationContext context = new CalculationContext(
                    configuration,
                    new AtomicInteger(0),
                    new AtomicInteger(0),
                    new CountDownLatch(1),
                    new LinkedBlockingQueue<>(),
                    new LinkedBlockingQueue<>(),
                    new ConcurrentHashMap<>(),
                    pool);

            pool.submit(new ReaderTask(context)).get();
            pool.submit(new FactorialCalculationTask(context)).get();
            pool.submit(new WriterTask(context)).get();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Factorial calculation completed");
    }
}