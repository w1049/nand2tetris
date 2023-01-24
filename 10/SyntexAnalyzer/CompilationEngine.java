package SyntexAnalyzer;

import SyntexAnalyzer.JackTokenizer.Keyword;
import SyntexAnalyzer.JackTokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final BufferedWriter buf;

    public CompilationEngine(String inputName, String outputName) throws IOException {
        tokenizer = new JackTokenizer(inputName);
        buf = new BufferedWriter(new FileWriter(outputName));
        tokenizer.advance(); // do not use this at the beginning of each compileXxx()
        // may use more back() to normalize beginning and ending?
    }

    public void compileClass() throws IOException {
        buf.write("<class>\n");
        buf.write(tokenizer.toXML()); // class
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // class name
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // {

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
        buf.write(tokenizer.toXML()); // }
        buf.write("</class>\n");
        buf.close();
    }

    public void compileClassVarDec() throws IOException {
        buf.write("<classVarDec>\n");
        buf.write(tokenizer.toXML()); // static or field
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // type
        do {
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // var name
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // , or ;
        } while (tokenizer.symbol() != ';');
        buf.write("</classVarDec>\n");
    }

    public void compileSubroutine() throws IOException {
        buf.write("<subroutineDec>\n");
        buf.write(tokenizer.toXML()); // constructor, function or method
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // type or void
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // subroutine name
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // (
        tokenizer.advance();
        compileParameterList();
        buf.write(tokenizer.toXML()); // )
        buf.write("<subroutineBody>\n");
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // {
        tokenizer.advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            tokenizer.advance();
        }
        compileStatements();
        buf.write(tokenizer.toXML()); // }
        buf.write("</subroutineBody>\n");
        buf.write("</subroutineDec>\n");
    }

    public void compileParameterList() throws IOException {
        buf.write("<parameterList>\n");
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
            buf.write(tokenizer.toXML()); // type
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // var name
            tokenizer.advance();
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                buf.write(tokenizer.toXML()); // ,
                tokenizer.advance();
            }
        }
        buf.write("</parameterList>\n");
    }

    public void compileVarDec() throws IOException {
        buf.write("<varDec>\n");
        buf.write(tokenizer.toXML()); // var
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // type
        do {
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // var name
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // , or ;
        } while (tokenizer.symbol() != ';');
        buf.write("</varDec>\n");
    }

    public void compileStatements() throws IOException { // end at }
        buf.write("<statements>\n");
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
        buf.write("</statements>\n");
    }

    public void compileDo() throws IOException { // end at ;
        buf.write("<doStatement>\n");
        buf.write(tokenizer.toXML()); // do
        tokenizer.advance();
        // subroutine call begin
        buf.write(tokenizer.toXML()); // subroutine name or class/var name
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            buf.write(tokenizer.toXML()); // .
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // subroutine name
            tokenizer.advance();
        }
        buf.write(tokenizer.toXML()); // (
        tokenizer.advance();
        compileExpressionList();
        buf.write(tokenizer.toXML()); // )
        tokenizer.advance();
        // subroutine call end
        buf.write(tokenizer.toXML()); // ;
        buf.write("</doStatement>\n");
    }

    public void compileLet() throws IOException { // end at ;
        buf.write("<letStatement>\n");
        buf.write(tokenizer.toXML()); // let
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // var name
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            buf.write(tokenizer.toXML()); // [
            tokenizer.advance();
            compileExpression();
            buf.write(tokenizer.toXML()); // ]
            tokenizer.advance();
        }
        buf.write(tokenizer.toXML()); // =
        tokenizer.advance();
        compileExpression();
        buf.write(tokenizer.toXML()); // ;
        buf.write("</letStatement>\n");
    }

    public void compileWhile() throws IOException { // end at }
        buf.write("<whileStatement>\n");
        buf.write(tokenizer.toXML()); // while
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // (
        tokenizer.advance();
        compileExpression();
        buf.write(tokenizer.toXML()); // )
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // {
        tokenizer.advance();
        compileStatements();
        buf.write(tokenizer.toXML()); // }
        buf.write("</whileStatement>\n");
    }

    public void compileReturn() throws IOException { // end at ;
        buf.write("<returnStatement>\n");
        buf.write(tokenizer.toXML()); // return
        tokenizer.advance();
        if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
            compileExpression();
        }
        buf.write(tokenizer.toXML()); // ;
        buf.write("</returnStatement>\n");
    }

    public void compileIf() throws IOException {// end at }
        buf.write("<ifStatement>\n");
        buf.write(tokenizer.toXML()); // if
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // (
        tokenizer.advance();
        compileExpression();
        buf.write(tokenizer.toXML()); // )
        tokenizer.advance();
        buf.write(tokenizer.toXML()); // {
        tokenizer.advance();
        compileStatements();
        buf.write(tokenizer.toXML()); // }
        // else begin
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.ELSE) {
            buf.write(tokenizer.toXML()); // else
            tokenizer.advance();
            buf.write(tokenizer.toXML()); // {
            tokenizer.advance();
            compileStatements();
            buf.write(tokenizer.toXML()); // }
        } else {
            tokenizer.back();
        }
        // else end
        buf.write("</ifStatement>\n");
    }

    public void compileExpression() throws IOException { // end at ]),;
        buf.write("<expression>\n");
        compileTerm();
        tokenizer.advance();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.isOp()) {
            buf.write(tokenizer.toXML()); // op
            tokenizer.advance();
            compileTerm();
            tokenizer.advance();
        }
        buf.write("</expression>\n");
    }

    public void compileTerm() throws IOException {
        buf.write("<term>\n");
        buf.write(tokenizer.toXML()); // cases below
        switch (tokenizer.tokenType()) {
            case INT_CONST, STRING_CONST, KEYWORD -> {
            } // KEYWORD must be constant
            case IDENTIFIER -> {
                tokenizer.advance();
                char peek = tokenizer.symbol();
                switch (peek) {
                    case ')' -> tokenizer.back();
                    case '[' -> {
                        buf.write(tokenizer.toXML()); // [
                        tokenizer.advance();
                        compileExpression();
                        buf.write(tokenizer.toXML()); // ]
                    }
                    case '(' -> {
                        buf.write(tokenizer.toXML()); // (
                        tokenizer.advance();
                        compileExpressionList();
                        buf.write(tokenizer.toXML()); // )
                    }
                    case '.' -> {
                        buf.write(tokenizer.toXML()); // .
                        tokenizer.advance();
                        buf.write(tokenizer.toXML()); // subroutine name
                        tokenizer.advance();
                        buf.write(tokenizer.toXML()); // (
                        tokenizer.advance();
                        compileExpressionList();
                        buf.write(tokenizer.toXML()); // )
                    }
                    default -> tokenizer.back();
                }
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    tokenizer.advance();
                    compileExpression();
                    buf.write(tokenizer.toXML()); // )
                } else { // unary op
                    tokenizer.advance();
                    compileTerm();
                }
            }
        }
        buf.write("</term>\n");
    }

    public void compileExpressionList() throws IOException { // end at )
        buf.write("<expressionList>\n");
        while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
            compileExpression();
            if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
                buf.write(tokenizer.toXML()); // ,
                tokenizer.advance();
            }
        }
        buf.write("</expressionList>\n");
    }
}
