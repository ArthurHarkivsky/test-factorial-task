package task;

import dto.CalculationContext;
import dto.ResultItem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WriterTask implements Runnable {
    private static final int POLLING_INTERVAL_MS = 50;
    private static final int LOG_INTERVAL = 100; // Log every 100 polls
    private final CalculationContext context;

    public WriterTask(CalculationContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(context.configuration().output()))) {
            boolean isCompleted = false;
            int pollCount = 0;

            while (!isCompleted || resultQueuesAreNotEmpty()) {
                ResultItem result = context.resultQueue().poll(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);

                if (result != null) {
                    context.orderedResults().put(result.index(), result);
                }

                writeOrderedResults(writer);

                if (!isCompleted) {
                    isCompleted = context.taskComplete().await(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }

                pollCount = logPeriodically(pollCount);
            }

            // Final writing pass to ensure nothing was missed
            writeOrderedResults(writer);

            System.out.println("All results written to " + context.configuration().output());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in writer thread: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private boolean resultQueuesAreNotEmpty() {
        return !context.resultQueue().isEmpty() || !context.orderedResults().isEmpty();
    }

    private void writeOrderedResults(BufferedWriter writer) throws IOException {
        int currentIndex = context.nextIndexToWrite().get();
        ResultItem nextResult;

        while ((nextResult = context.orderedResults().get(currentIndex)) != null) {
            writer.write(nextResult.number() + " = " + nextResult.result());
            writer.newLine();
            context.orderedResults().remove(currentIndex);
            currentIndex = context.nextIndexToWrite().incrementAndGet();
        }

        // Single flush after writing multiple results for better performance
        writer.flush();
    }

    private int logPeriodically(int pollCount) {
        if (++pollCount % LOG_INTERVAL == 0) {
            System.out.printf("""
                            Progress:
                            Queue size=%d,
                            Ordered results size=%d,
                            Next index=%d%n""",
                    context.resultQueue().size(), context.orderedResults().size(), context.nextIndexToWrite().get());
        }
        return pollCount;
    }
}