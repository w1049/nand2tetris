import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AssemblerL {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tAssemblerL <filename>");
            return;
        }
        Parser parser = new Parser(args[0]);
        BufferedWriter buf = new BufferedWriter(
                new FileWriter(args[0].substring(0, args[0].length() - 4) + ".hack"));
        while (parser.hasMoreCommands()) {
            parser.advance();
            if (parser.commandType() == Parser.CommandType.A_COMMAND) {
                int value = Integer.parseInt(parser.symbol());
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
