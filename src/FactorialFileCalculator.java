import configuration.CommandLineTaskConfigurationReader;
import configuration.dto.TaskConfiguration;
import dto.CalculationContext;
import task.FactorialCalculationTask;
import task.ReaderTask;
import task.WriterTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.CompletableFuture.runAsync;

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

            CompletableFuture.allOf(new CompletableFuture[]{
                    runAsync(new ReaderTask(context), pool),
                    runAsync(new FactorialCalculationTask(context), pool),
                    runAsync(new WriterTask(context), pool)}).join();

        }
        System.out.println("Factorial calculation completed");
    }
}