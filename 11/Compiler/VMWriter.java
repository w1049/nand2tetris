package Compiler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
    private final BufferedWriter buf;

    public VMWriter(String fileName) throws IOException {
        buf = new BufferedWriter(new FileWriter(fileName));
    }

    public void writePush(Segment segment, int index) throws IOException {
        buf.write("push " + segment.getName() + " " + index + "\n");
    }

    public void writePop(Segment segment, int index) throws IOException {
        buf.write("pop " + segment.getName() + " " + index + "\n");
    }

    public void writeArithmetic(Command command) throws IOException {
        buf.write(command.toString().toLowerCase() + "\n");
    }

    public void writeLabel(String label) throws IOException {
        buf.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        buf.write("goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        buf.write("if-goto " + label + "\n");
    }

    public void writeCall(String functionName, int numArgs) throws IOException {
        buf.write("call " + functionName + " " + numArgs + "\n");
    }

    public void writeFunction(String functionName, int numLocals) throws IOException {
        buf.write("function " + functionName + " " + numLocals + "\n");
    }

    public void writeReturn() throws IOException {
        buf.write("return\n");
    }

    public void close() throws IOException {
        buf.close();
    }

    public enum Segment {
        CONST("constant"), ARG("argument"),
        LOCAL("local"), STATIC("static"),
        THIS("this"), THAT("that"),
        POINTER("pointer"), TEMP("temp");

        final String segmentName;

        Segment(String segmentName) {
            this.segmentName = segmentName;
        }

        public String getName() {
            return segmentName;
        }
    }

    public enum Command {
        ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
    }
}
