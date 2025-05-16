package configuration;

import configuration.dto.TaskConfiguration;

import java.util.Scanner;

public class CommandLineTaskConfigurationReader implements TaskConfigurationReader {
    public TaskConfiguration read() {
        Scanner scanner = new Scanner(System.in);

        int threadCount = readThreadCount(scanner);
        String inputPath = readPathWithPrompt(scanner, "Enter the input file path: ", "input.txt");
        String outputPath = readPathWithPrompt(scanner, "Enter the output file path: ", "output.txt");

        scanner.close();

        return new TaskConfiguration(threadCount, inputPath, outputPath);
    }

    private int readThreadCount(Scanner scanner) {
        System.out.print("Enter the number of calculation threads (N): ");
        int threadCount = 1;
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                try {
                    threadCount = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid thread count entered: " + line);
                }
            }
        }

        return threadCount;
    }

    private String readPathWithPrompt(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt);
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {

                return line;
            }
        }

        return defaultValue;
    }
}