# EVRPTW ALNS Solver

Electric Vehicle Routing Problem with Time Windows solver using Adaptive Large Neighborhood Search.

## Build

```bash
mvn package -q -DskipTests
```

## Usage

```bash
java -jar target/EVRPTW_ALNS-1.0-SNAPSHOT-jar-with-dependencies.jar <instance_file> [options]

java -jar target/EVRPTW_ALNS-1.0-SNAPSHOT-jar-with-dependencies.jar .\src\main\resources\data\r201_21.txt -i 5000 -t 60 --plot
```

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--iterations <n>` | `-i` | Number of ALNS iterations (default: 5000) |
| `--time <seconds>` | `-t` | Time limit in seconds (default: 0 = no limit) |
| `--output-dir <dir>` | `-o` | Output directory for solutions (default: solutions) |
| `--plot` | `-p` | Generate solution plot (PNG image) |
| `--no-verify` | | Skip solution verification |
| `--verifier <path>` | | Path to verifier JAR file |
| `--log-level <level>` | | Logging level: DEBUG, INFO, WARNING, ERROR (default: INFO) |
| `--help` | `-h` | Show help message |