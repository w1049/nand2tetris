import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Parser {
    private final Iterator<String> commandIterator;
    private String currentCommand;
    private String[] args;

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
        if (str.contains("//")) {
            str = str.substring(0, str.indexOf("//"));
        }
        return str.strip().replaceAll("\\s+", " ");
    }

    public boolean hasMoreCommands() {
        return commandIterator.hasNext();
    }

    public void advance() {
        currentCommand = commandIterator.next();
        args = currentCommand.split(" ");
    }

    public String getCurrentCommand() {
        return currentCommand;
    }

    public CommandType commandType() {
        CommandType commandType = null;
        switch (args[0]) {
            case "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not" -> commandType = CommandType.C_ARITHMETIC;
            case "push" -> commandType = CommandType.C_PUSH;
            case "pop" -> commandType = CommandType.C_POP;
            case "label" -> commandType = CommandType.C_LABEL;
            case "goto" -> commandType = CommandType.C_GOTO;
            case "if-goto" -> commandType = CommandType.C_IF;
            case "function" -> commandType = CommandType.C_FUNCTION;
            case "call" -> commandType = CommandType.C_CALL;
            case "return" -> commandType = CommandType.C_RETURN;
        }
        return commandType;
    }

    public String arg1() {
        if (commandType() == CommandType.C_RETURN)
            return null;
        else if (commandType() == CommandType.C_ARITHMETIC)
            return args[0];
        else
            return args[1];
    }

    public int arg2() {
        if (commandType() == CommandType.C_PUSH || commandType() == CommandType.C_POP ||
                commandType() == CommandType.C_FUNCTION || commandType() == CommandType.C_CALL)
            return Integer.parseInt(args[2]);
        else
            return 0;
    }

    public enum CommandType {
        C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF,
        C_FUNCTION, C_RETURN, C_CALL
    }
}
