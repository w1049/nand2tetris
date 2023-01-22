import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tVMTranslator <filename>");
            return;
        }

        Parser parser = new Parser(args[0]);
        CodeWriter writer = new CodeWriter(args[0].substring(0, args[0].length() - 3) + ".asm");
        while (parser.hasMoreCommands()) {
            parser.advance();
            switch (parser.commandType()) {
                case C_ARITHMETIC -> writer.writeArithmetic(parser.getCurrentCommand());
                case C_PUSH, C_POP -> writer.writePushPop(parser.commandType(), parser.arg1(), parser.arg2());
            }
        }
        writer.close();
    }
}
