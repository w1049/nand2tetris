import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {
    private final BufferedWriter buf;
    private String fileName;
    private int jumpID = 0;

    public CodeWriter(String fileName) throws IOException {
        buf = new BufferedWriter(new FileWriter(fileName));
        setFileName(fileName);
    }

    public void setFileName(String fileName) {
        int endIndex = fileName.lastIndexOf(".");
        if (fileName.contains("\\"))
            this.fileName = fileName.substring(fileName.lastIndexOf("\\") + 1, endIndex);
        else if (fileName.contains("/"))
            this.fileName = fileName.substring(fileName.lastIndexOf("/") + 1, endIndex);
        else
            this.fileName = fileName.substring(0, endIndex);
    }

    public void writeArithmetic(String command) throws IOException {
        switch (command) {
            case "add" -> {
                pop();
                buf.write("A=A-1\nM=M+D\n");
            }
            case "sub" -> {
                pop();
                buf.write("A=A-1\nM=M-D\n");
            }
            case "neg" -> buf.write("@SP\nA=M-1\nM=-M\n");
            case "eq" -> arithmeticJump("JEQ");
            case "gt" -> arithmeticJump("JGT");
            case "lt" -> arithmeticJump("JLT");
            case "and" -> {
                pop();
                buf.write("A=A-1\nM=M&D\n");
            }
            case "or" -> {
                pop();
                buf.write("A=A-1\nM=M|D\n");
            }
            case "not" -> buf.write("@SP\nA=M-1\nM=!M\n");
        }
    }

    public void writePushPop(Parser.CommandType commandType, String segment, int index) throws IOException {
        switch (commandType) {
            case C_PUSH -> {
                switch (segment) {
                    case "constant" -> {
                        buf.write(String.format("@%d\nD=A\n", index));
                        push();
                    }
                    case "local" -> offsetPush("LCL", index, false);
                    case "argument" -> offsetPush("ARG", index, false);
                    case "this" -> offsetPush("THIS", index, false);
                    case "that" -> offsetPush("THAT", index, false);
                    case "pointer" -> offsetPush("THIS", index, true);
                    case "temp" -> offsetPush("R5", index, true);
                    case "static" -> {
                        buf.write(String.format("@%s.%s\nD=M\n", fileName, index));
                        push();
                    }
                }
            }
            case C_POP -> {
                switch (segment) {
                    case "local" -> offsetPop("LCL", index, false);
                    case "argument" -> offsetPop("ARG", index, false);
                    case "this" -> offsetPop("THIS", index, false);
                    case "that" -> offsetPop("THAT", index, false);
                    case "pointer" -> offsetPop("THIS", index, true);
                    case "temp" -> offsetPop("R5", index, true);
                    case "static" -> {
                        pop();
                        buf.write(String.format("@%s.%s\nM=D\n", fileName, index));
                    }
                }
            }
        }
    }

    public void close() throws IOException {
        buf.close();
    }

    // save popped value in D
    private void pop() throws IOException {
        buf.write("@SP\nAM=M-1\nD=M\n");
    }

    // push D value into stack
    private void push() throws IOException {
        buf.write("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
    }

    private void arithmeticJump(String jump) throws IOException {
        pop();
        buf.write(String.format("""
                A=A-1
                D=M-D
                @TRUE%1$d
                D;%2$s
                @SP
                A=M-1
                M=0
                @END%1$d
                0;JMP
                (TRUE%1$d)
                @SP
                A=M-1
                M=-1
                (END%1$d)
                """, ++jumpID, jump));
    }

    private void offsetPush(String base, int index, boolean direct) throws IOException {
        buf.write(String.format("@%1$s\nD=%3$s\n@%2$d\nA=D+A\nD=M\n", base, index, direct ? "A" : "M"));
        push();
    }

    private void offsetPop(String base, int index, boolean direct) throws IOException {
        buf.write(String.format("@%1$s\nD=%3$s\n@%2$d\nD=D+A\n@R13\nM=D\n", base, index, direct ? "A" : "M"));
        pop();
        buf.write("@R13\nA=M\nM=D\n");
    }
}
