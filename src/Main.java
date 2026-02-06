import java.io.*;
import java.util.Locale;

/**
 * Main entry point for Matrixnet platform.
 */
public class Main {

    public static void main(String[] args) {

        CommandExecuter commandExecuter = new CommandExecuter();

        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer, commandExecuter);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer, CommandExecuter commandExecuter)
            throws IOException {

        String[] parts = command.split("\\s+");
        String operation = parts[0];

        try {
            String result = "";

            switch (operation) {
                case "spawn_host":
                    result = commandExecuter.spawnHost(parts[1], Integer.parseInt(parts[2]));
                    break;
                case "link_backdoor":
                    result = commandExecuter.linkBackdoor(parts[1], parts[2], Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
                    break;

                case "seal_backdoor":
                    result = commandExecuter.sealBackdoor(parts[1], parts[2]);
                    break;

                case "trace_route":
                    result = commandExecuter.traceRoute(parts[1], parts[2], Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]));
                    break;

                case "scan_connectivity":
                    result = commandExecuter.scanConnectivity();
                    break;

                case "simulate_breach":
                    if(parts.length == 2){
                        result = commandExecuter.simulateBreach(parts[1]);
                    }
                    else if(parts.length == 3){
                        result = commandExecuter.simulateBreach(parts[1], parts[2]);
                    }
                    break;

                case "oracle_report":
                    result = commandExecuter.oracleReport();
                    break;

                default:
                    result = "Unknown command: " + operation;
            }

            writer.write(result);
            writer.newLine();

        } catch (Exception e) {
            writer.write("Error processing command: " + command);
            writer.newLine();
        }
    }
}