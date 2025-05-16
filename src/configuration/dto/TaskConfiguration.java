package configuration.dto;

public record TaskConfiguration(int threadPoolSize,
                                // path | URL | content | etc.
                                String input,
                                // path | URL | content | etc.
                                String output) {
}
