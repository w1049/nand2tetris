import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Parser {
    private final Iterator<String> commandIterator;
    private String currentCommand;

    public Parser(String fileName) throws IOException {
        List<String> commandList = new ArrayList<>();
        BufferedReader buf = new BufferedReader(new FileReader(fileName));
        String str;
        while ((str = buf.readLine()) != null) {
            String command = removeWhitespace(str);
            if (!command.equals(""))
                commandList.add(command);
        }

        buf.close();
        commandIterator = commandList.iterator();
    }

    private String removeWhitespace(String str) {
        String command = str.replaceAll("\\s", "");
        if (command.contains("//")) {
            command = command.substring(0, command.indexOf("//"));
        }
        return command;
    }

    public boolean hasMoreCommands() {
        return commandIterator.hasNext();
    }

    public void advance() {
        currentCommand = commandIterator.next();
    }

    public CommandType commandType() {
        if (currentCommand.startsWith("@"))
            return CommandType.A_COMMAND;
        else if (currentCommand.startsWith("("))
            return CommandType.L_COMMAND;
        return CommandType.C_COMMAND;
    }

    public String symbol() {
        if (commandType() == CommandType.A_COMMAND)
            return currentCommand.substring(1);
        else if (commandType() == CommandType.L_COMMAND)
            return currentCommand.substring(1, currentCommand.length() - 1);
        return null;
    }

    public String dest() {
        if (commandType() == CommandType.C_COMMAND) {
            if (currentCommand.contains("="))
                return currentCommand.substring(0, currentCommand.indexOf("="));
            else
                return "";
        }
        return null;
    }

    public String comp() {
        if (commandType() == CommandType.C_COMMAND) {
            int beginIndex = currentCommand.contains("=") ? currentCommand.indexOf("=") + 1 : 0;
            int endIndex = currentCommand.contains(";") ? currentCommand.indexOf(";") : currentCommand.length();
            return currentCommand.substring(beginIndex, endIndex);
        }
        return null;
    }

    public String jump() {
        if (commandType() == CommandType.C_COMMAND) {
            if (currentCommand.contains(";"))
                return currentCommand.substring(currentCommand.indexOf(";") + 1);
            else
                return "";
        }
        return null;
    }

    public enum CommandType {
        A_COMMAND, C_COMMAND, L_COMMAND
    }
}
