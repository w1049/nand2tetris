package SyntexAnalyzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JackAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tJackAnalyzer <filename>\n\tJackAnalyzer <dictionary>");
            return;
        }
        Path path = Paths.get(args[0]);
        List<String> fileList;
        if (Files.isRegularFile(path)) {
            fileList = new ArrayList<>();
            fileList.add(args[0]);
        } else if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path, 1)) {
                fileList = walk.filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(s -> s.endsWith(".jack"))
                        .collect(Collectors.toList());
            }
        } else throw new FileNotFoundException();
        for (String fileName : fileList) {
            CompilationEngine engine = new CompilationEngine(fileName,
                    fileName.substring(0, fileName.length() - 5) + ".xml");
            engine.compileClass();
        }
    }
}
