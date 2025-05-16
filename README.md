### Factorial App
Calculates a factorial for each value from the file.

It could be refactored more or simplified to a single class file.

Prerequisites
- Windows with WSL / Linux/Unix / macOS
- JDK 21

Compile
```aiignore
javac $(find . -name "*.java") -d out
```
Run
```aiignore
cd out
java FactorialFileCalculator
```
Enter or skip values to use defaults
```aiignore
Enter the number of calculation threads (N): 48 // default is 1
Enter the input file path: // skipping to use default "input.txt"
Enter the output file path: output.txt
```
TODO after the demo
1. **Error Handling**: Implement more robust error handling with proper logging
    - Add a dedicated logger rather than using System.out.println
    - Implement retry logic for transient errors

2. **Progress Reporting**: Add a progress bar or percentage completion indicator
    - Calculate and display the percentage of completed tasks

3. **Configuration Enhancements**:
    - Add the ability to read configuration from a properties file
    - Implement command-line arguments for easier scripting

4. **Performance Optimizations**:
    - Implement batched writing for better I/O performance
    - Add buffer size configuration for file I/O operations
    - Implement work stealing for better thread utilization

5. **Memory Management**:
    - Add a maximum in-memory queue size to prevent out-of-memory errors
    - Implement disk-based spooling for huge result sets

6. **Testing & Monitoring**:
    - Add a metrics collection for performance monitoring
    - Implement unit and integration tests
