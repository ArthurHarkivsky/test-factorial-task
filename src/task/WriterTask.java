package task;

import dto.CalculationContext;
import dto.ResultItem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WriterTask implements Runnable {
    private final CalculationContext context;

    public WriterTask(CalculationContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(context.configuration().output()))) {
            boolean isCompleted = false;

            while (!isCompleted || resultQueuesAreNotEmpty()) {

                ResultItem result = context.resultQueue().poll(100, TimeUnit.MILLISECONDS);

                if (result != null) {
                    context.orderedResults().put(result.index(), result);
                }

                writeOrderedResults(writer);

                if (!isCompleted) {
                    isCompleted = context.taskComplete().await(10, TimeUnit.MILLISECONDS);
                }
                System.out.println("Result queue size: " + context.resultQueue().size());
            }
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
        ResultItem nextResult;
        while ((nextResult = getNextAndRemoveFromQueue()) != null) {
            // format: "number = factorial"
            writer.write(nextResult.number() + " = " + nextResult.result());
            writer.newLine();
            writer.flush();

            context.nextIndexToWrite().incrementAndGet();
        }
    }

    private ResultItem getNextAndRemoveFromQueue() {
        return context.orderedResults().remove(context.nextIndexToWrite().get());
    }
}