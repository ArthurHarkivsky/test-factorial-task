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

End of log example
```aiignore
Result queue size: 3
Result queue size: 2
Result queue size: 1
Result queue size: 0
All results written to output.txt
Factorial calculation completed
```