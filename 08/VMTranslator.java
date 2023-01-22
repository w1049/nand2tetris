import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VMTranslator {

    private static List<String> fileList;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tVMTranslator <filename>\n\tVMTranslator <dictionary>");
            return;
        }
        Path path = Paths.get(args[0]);
        String outputFileName = null;
        if (Files.isRegularFile(path)) {
            outputFileName = args[0].substring(0, args[0].length() - 3);
            fileList = new ArrayList<>();
            fileList.add(args[0]);
        }
        if (Files.isDirectory(path)) {
            outputFileName = path + "/" + path.getFileName().toString();
            try (Stream<Path> walk = Files.walk(path, 1)) {
                fileList = walk.filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(s -> s.endsWith(".vm"))
                        .collect(Collectors.toList());
            }
        }

        CodeWriter writer = new CodeWriter(outputFileName + ".asm");
        for (String fileName : fileList) {
            Parser parser = new Parser(fileName);
            writer.setFileName(fileName);
            while (parser.hasMoreCommands()) {
                parser.advance();
                switch (parser.commandType()) {
                    case C_ARITHMETIC -> writer.writeArithmetic(parser.getCurrentCommand());
                    case C_PUSH, C_POP -> writer.writePushPop(parser.commandType(), parser.arg1(), parser.arg2());
                    case C_LABEL -> writer.writeLabel(parser.arg1());
                    case C_GOTO -> writer.writeGoto(parser.arg1());
                    case C_IF -> writer.writeIf(parser.arg1());
                    case C_CALL -> writer.writeCall(parser.arg1(), parser.arg2());
                    case C_FUNCTION -> writer.writeFunction(parser.arg1(), parser.arg2());
                    case C_RETURN -> writer.writeReturn();
                }
            }
        }
        writer.close();
    }
}
