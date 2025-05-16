package task;

import dto.CalculationContext;
import dto.InputItem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReaderTask implements Runnable {
    private final CalculationContext context;

    public ReaderTask(CalculationContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(context.configuration().input()))) {
            String line;
            int index = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    int number = Integer.parseInt(line);
                    context.taskQueue().put(new InputItem(index++, number));
                    context.taskCount().incrementAndGet();
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format: " + line);
                }
            }

            System.out.println("File reading completed. Total tasks: " + context.taskCount().get());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in reader thread: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}