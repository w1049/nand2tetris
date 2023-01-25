package Compiler;

import Compiler.JackTokenizer.Keyword;
import Compiler.JackTokenizer.TokenType;
import Compiler.SymbolTable.Kind;
import Compiler.VMWriter.Command;
import Compiler.VMWriter.Segment;

import java.io.IOException;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final VMWriter writer;
    private final SymbolTable symbolTable;

    private String className;
    private int ifIndex, whileIndex;

    public CompilationEngine(String inputName, String outputName) throws IOException {
        tokenizer = new JackTokenizer(inputName);
        writer = new VMWriter(outputName);
        symbolTable = new SymbolTable();
        tokenizer.advance(); // do not use this at the beginning of each compileXxx()
        // may use more back() to normalize beginning and ending?
    }

    public void compileClass() throws IOException {
        // buf.write(tokenizer.toXML()); // class
        tokenizer.advance(); // class name
        className = tokenizer.identifier();
        tokenizer.advance(); // {

        tokenizer.advance();
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}')) {
            if (tokenizer.tokenType() == TokenType.KEYWORD) { // certainly
                if (tokenizer.keyword() == Keyword.STATIC ||
                        tokenizer.keyword() == Keyword.FIELD) {
                    compileClassVarDec();
                } else if (tokenizer.keyword() == Keyword.CONSTRUCTOR ||
                        tokenizer.keyword() == Keyword.FUNCTION ||
                        tokenizer.keyword() == Keyword.METHOD) {
                    compileSubroutine();
                }
                tokenizer.advance();
            }
        }
        // buf.write(tokenizer.toXML()); // }
        writer.close();
    }

    public void compileClassVarDec() {
        // buf.write(tokenizer.toXML()); // static or field
        Kind kind = tokenizer.keyword() == Keyword.STATIC ? Kind.STATIC : Kind.FIELD;
        tokenizer.advance(); // type
        String type = tokenizer.tokenType() == TokenType.KEYWORD ?
                tokenizer.keyword().toString().toLowerCase() : tokenizer.identifier();
        do {
            tokenizer.advance(); // var name
            symbolTable.define(tokenizer.identifier(), type, kind);
            tokenizer.advance(); // , or ;
        } while (tokenizer.symbol() != ';');
    }

    public void compileSubroutine() throws IOException {
        // buf.write(tokenizer.toXML()); // constructor, function or method
        Keyword subroutineType = tokenizer.keyword();
        symbolTable.startSubroutine();
        ifIndex = whileIndex = 0;
        if (subroutineType == Keyword.METHOD)
            symbolTable.define("this", className, Kind.ARG);
        tokenizer.advance(); // type or void
        tokenizer.advance(); // subroutine name
        String subroutineName = tokenizer.identifier();
        tokenizer.advance(); // (
        tokenizer.advance();
        compileParameterList();
        // buf.write(tokenizer.toXML()); // )

        // subroutine body begin
        tokenizer.advance(); // {
        tokenizer.advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            tokenizer.advance();
        }
        writer.writeFunction(className + "." + subroutineName, symbolTable.varCount(Kind.VAR));
        if (subroutineType == Keyword.CONSTRUCTOR) {
            writer.writePush(Segment.CONST, symbolTable.varCount(Kind.FIELD));
            writer.writeCall("Memory.alloc", 1);
            writer.writePop(Segment.POINTER, 0); // alloc returns the base address of new object
        } else if (subroutineType == Keyword.METHOD) {
            writer.writePush(Segment.ARG, 0);
            writer.writePop(Segment.POINTER, 0); // pointer[0] = arg[0], set "this" to this object
        }
        compileStatements();
        // buf.write(tokenizer.toXML()); // }
        // subroutine body end
    }

    public void compileParameterList() {
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
            // buf.write(tokenizer.toXML()); // type
            String type = tokenizer.tokenType() == TokenType.KEYWORD ?
                    tokenizer.keyword().toString().toLowerCase() : tokenizer.identifier();
            tokenizer.advance(); // var name
            symbolTable.define(tokenizer.identifier(), type, Kind.ARG);
            tokenizer.advance();
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                // buf.write(tokenizer.toXML()); // ,
                tokenizer.advance();
            }
        }
    }

    public void compileVarDec() {
        // buf.write(tokenizer.toXML()); // var
        tokenizer.advance();
        // buf.write(tokenizer.toXML()); // type
        String type = tokenizer.tokenType() == TokenType.KEYWORD ?
                tokenizer.keyword().toString().toLowerCase() : tokenizer.identifier();
        do {
            tokenizer.advance();
            // buf.write(tokenizer.toXML()); // var name
            symbolTable.define(tokenizer.identifier(), type, Kind.VAR);
            tokenizer.advance();
            // buf.write(tokenizer.toXML()); // , or ;
        } while (tokenizer.symbol() != ';');
    }

    public void compileStatements() throws IOException {
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}')) {
            switch (tokenizer.keyword()) {
                case LET -> compileLet();
                case IF -> compileIf();
                case WHILE -> compileWhile();
                case DO -> compileDo();
                case RETURN -> compileReturn();
            }
            tokenizer.advance();
        }
    }

    private Segment getSegment(Kind kind) {
        Segment segment;
        switch (kind) {
            case STATIC -> segment = Segment.STATIC;
            case FIELD -> segment = Segment.THIS;
            case ARG -> segment = Segment.ARG;
            case VAR -> segment = Segment.LOCAL;
            default -> segment = Segment.CONST; // impossible
        }
        return segment;
    }

    public void compileDo() throws IOException {
        // buf.write(tokenizer.toXML()); // do
        // subroutine call begin
        tokenizer.advance(); // subroutine name or class/var name
        String name = tokenizer.identifier();
        int nArgs = 0;
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            // buf.write(tokenizer.toXML()); // .
            tokenizer.advance(); // subroutine name
            if (symbolTable.kindOf(name) == Kind.NONE) { // class name
                name += "." + tokenizer.identifier();
            } else { // var name
                writer.writePush(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name)); // as arg[0]
                ++nArgs;
                name = symbolTable.typeOf(name) + "." + tokenizer.identifier();
            }
            tokenizer.advance();
        } else {
            writer.writePush(Segment.POINTER, 0);
            ++nArgs;
            name = className + "." + name;
        }
        // buf.write(tokenizer.toXML()); // (
        tokenizer.advance();
        nArgs += compileExpressionList();
        // buf.write(tokenizer.toXML()); // )
        writer.writeCall(name, nArgs);
        tokenizer.advance();
        // subroutine call end
        writer.writePop(Segment.TEMP, 0);
        // buf.write(tokenizer.toXML()); // ;
    }

    public void compileLet() throws IOException {
        // buf.write(tokenizer.toXML()); // let
        tokenizer.advance(); // var name
        String name = tokenizer.identifier();
        tokenizer.advance();
        boolean isArray = false;
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            // buf.write(tokenizer.toXML()); // [
            isArray = true;
            writer.writePush(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
            tokenizer.advance();
            compileExpression();
            writer.writeArithmetic(Command.ADD);
            // buf.write(tokenizer.toXML()); // ]
            tokenizer.advance();
        }
        // buf.write(tokenizer.toXML()); // =
        tokenizer.advance();
        compileExpression();
        if (isArray) {
            writer.writePop(Segment.TEMP, 0);
            writer.writePop(Segment.POINTER, 1);
            writer.writePush(Segment.TEMP, 0);
            writer.writePop(Segment.THAT, 0);
        } else {
            writer.writePop(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
        }
        // buf.write(tokenizer.toXML()); // ;
    }

    public void compileWhile() throws IOException {
        // buf.write(tokenizer.toXML()); // while
        String endLabel = "WHILE_END" + whileIndex;
        String expLabel = "WHILE_EXP" + whileIndex++;
        writer.writeLabel(expLabel);
        tokenizer.advance(); // (
        tokenizer.advance();
        compileExpression();
        // buf.write(tokenizer.toXML()); // )
        writer.writeArithmetic(Command.NOT);
        writer.writeIf(endLabel);
        tokenizer.advance(); // {
        tokenizer.advance();
        compileStatements();
        writer.writeGoto(expLabel);
        writer.writeLabel(endLabel);
        // buf.write(tokenizer.toXML()); // }
    }

    public void compileReturn() throws IOException {
        // buf.write(tokenizer.toXML()); // return
        tokenizer.advance();
        if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
            compileExpression();
        } else {
            writer.writePush(Segment.CONST, 0);
        }
        writer.writeReturn();
        // buf.write(tokenizer.toXML()); // ;
    }

    public void compileIf() throws IOException {
        // buf.write(tokenizer.toXML()); // if
        String endLabel = "IF_END" + ifIndex;
        String falseLabel = "IF_FALSE" + ifIndex;
        String trueLabel = "IF_TRUE" + ifIndex++;
        tokenizer.advance(); // (
        tokenizer.advance();
        compileExpression();
        // buf.write(tokenizer.toXML()); // )
        writer.writeIf(trueLabel);
        writer.writeGoto(falseLabel);
        writer.writeLabel(trueLabel);
        tokenizer.advance(); // {
        tokenizer.advance();
        compileStatements();
        // buf.write(tokenizer.toXML()); // }
        // else begin
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.ELSE) {
            writer.writeGoto(endLabel);
            writer.writeLabel(falseLabel);
            // buf.write(tokenizer.toXML()); // else
            tokenizer.advance();
            // buf.write(tokenizer.toXML()); // {
            tokenizer.advance();
            compileStatements();
            // buf.write(tokenizer.toXML()); // }
            writer.writeLabel(endLabel);
        } else {
            writer.writeLabel(falseLabel);
            tokenizer.back();
        }
        // else end
    }

    public void compileExpression() throws IOException {
        compileTerm();
        tokenizer.advance();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.isOp()) {
            // buf.write(tokenizer.toXML()); // op
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            switch (op) {
                case '+' -> writer.writeArithmetic(Command.ADD);
                case '-' -> writer.writeArithmetic(Command.SUB);
                case '*' -> writer.writeCall("Math.multiply", 2);
                case '/' -> writer.writeCall("Math.divide", 2);
                case '&' -> writer.writeArithmetic(Command.AND);
                case '|' -> writer.writeArithmetic(Command.OR);
                case '<' -> writer.writeArithmetic(Command.LT);
                case '>' -> writer.writeArithmetic(Command.GT);
                case '=' -> writer.writeArithmetic(Command.EQ);
            }
            tokenizer.advance();
        }
    }

    public void compileTerm() throws IOException {
        // buf.write(tokenizer.toXML()); // cases below
        switch (tokenizer.tokenType()) {
            case INT_CONST -> writer.writePush(Segment.CONST, tokenizer.intVal());
            case STRING_CONST -> {
                String str = tokenizer.stringVal();
                writer.writePush(Segment.CONST, str.length());
                writer.writeCall("String.new", 1);
                for (int i = 0; i < str.length(); i++) {
                    writer.writePush(Segment.CONST, str.charAt(i));
                    writer.writeCall("String.appendChar", 2);
                }
            }
            case KEYWORD -> {
                switch (tokenizer.keyword()) {
                    case TRUE -> {
                        writer.writePush(Segment.CONST, 1);
                        writer.writeArithmetic(Command.NEG);
                    }
                    case FALSE, NULL -> writer.writePush(Segment.CONST, 0);
                    case THIS -> writer.writePush(Segment.POINTER, 0);
                }
            }
            case IDENTIFIER -> {
                String name = tokenizer.identifier();
                tokenizer.advance();
                char peek = tokenizer.symbol();
                switch (peek) {
                    case '[' -> {
                        // buf.write(tokenizer.toXML()); // [
                        writer.writePush(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
                        tokenizer.advance();
                        compileExpression();
                        writer.writeArithmetic(Command.ADD);
                        writer.writePop(Segment.POINTER, 1);
                        writer.writePush(Segment.THAT, 0);
                        // buf.write(tokenizer.toXML()); // ]
                    }
                    case '(' -> {
                        // buf.write(tokenizer.toXML()); // (
                        tokenizer.advance();
                        writer.writePush(Segment.POINTER, 0);
                        int nArgs = compileExpressionList() + 1;
                        name = className + "." + name;
                        // buf.write(tokenizer.toXML()); // )
                        writer.writeCall(name, nArgs);
                    }
                    case '.' -> {
                        // buf.write(tokenizer.toXML()); // .
                        tokenizer.advance(); // subroutine name
                        int nArgs = 0;
                        if (symbolTable.kindOf(name) == Kind.NONE) { // class name
                            name += "." + tokenizer.identifier();
                        } else { // var name
                            writer.writePush(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name)); // as arg[0]
                            ++nArgs;
                            name = symbolTable.typeOf(name) + "." + tokenizer.identifier();
                        }
                        tokenizer.advance();
                        // buf.write(tokenizer.toXML()); // (
                        tokenizer.advance();
                        nArgs += compileExpressionList();
                        // buf.write(tokenizer.toXML()); // )
                        writer.writeCall(name, nArgs);
                    }
                    default -> {
                        writer.writePush(getSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
                        tokenizer.back();
                    }
                }
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    tokenizer.advance();
                    compileExpression();
                    // buf.write(tokenizer.toXML()); // )
                } else { // unary op
                    char op = tokenizer.symbol();
                    tokenizer.advance();
                    compileTerm();
                    if (op == '~') writer.writeArithmetic(Command.NOT);
                    else if (op == '-') writer.writeArithmetic(Command.NEG);
                }
            }
        }
    }

    public int compileExpressionList() throws IOException {
        int nExpressions = 0;
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
            compileExpression();
            ++nExpressions;
            if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
                // buf.write(tokenizer.toXML()); // ,
                tokenizer.advance();
            }
        }
        return nExpressions;
    }
}
