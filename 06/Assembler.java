import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

public class Assembler {

    private static final Map<String, Integer> symbolTable = new HashMap<>(Map.ofEntries(
            entry("SP", 0), entry("LCL", 1), entry("ARG", 2),
            entry("THIS", 3), entry("THAT", 4), entry("R0", 0),
            entry("R1", 1), entry("R2", 2), entry("R3", 3),
            entry("R4", 4), entry("R5", 5), entry("R6", 6),
            entry("R7", 7), entry("R8", 8), entry("R9", 9),
            entry("R10", 10), entry("R11", 11), entry("R12", 12),
            entry("R13", 13), entry("R14", 14), entry("R15", 15),
            entry("SCREEN", 16384), entry("KBD", 24576)));

    private static int slot = 16;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tAssembler <filename>");
            return;
        }

        // First time
        Parser parser = new Parser(args[0]);
        int address = -1;
        while (parser.hasMoreCommands()) {
            parser.advance();
            if (parser.commandType() == Parser.CommandType.L_COMMAND) {
                symbolTable.put(parser.symbol(), address + 1);
            } else {
                ++address;
            }
        }

        // Second time
        parser = new Parser(args[0]);
        BufferedWriter buf = new BufferedWriter(
                new FileWriter(args[0].substring(0, args[0].length() - 4) + ".hack"));
        while (parser.hasMoreCommands()) {
            parser.advance();
            if (parser.commandType() == Parser.CommandType.A_COMMAND) {
                int value;
                String symbol = parser.symbol();
                if (symbol.chars().allMatch(Character::isDigit)) {
                    value = Integer.parseInt(symbol);
                } else if (symbolTable.containsKey(symbol)) {
                    value = symbolTable.get(symbol);
                } else {
                    value = slot++;
                    symbolTable.put(symbol, value);
                }
                String str = Integer.toBinaryString(value);
                str = "0".repeat(16 - str.length()) + str + "\n";
                buf.write(str);
            } else if (parser.commandType() == Parser.CommandType.C_COMMAND) {
                buf.write("111" +
                        Code.comp(parser.comp()) +
                        Code.dest(parser.dest()) +
                        Code.jump(parser.jump()) + "\n");
            }
        }
        buf.close();
    }
}
