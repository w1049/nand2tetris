package SyntexAnalyzer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class JackTokenizer {
    private static final Map<String, Keyword> keywords = Map.ofEntries(
            entry("class", Keyword.CLASS),
            entry("method", Keyword.METHOD),
            entry("int", Keyword.INT),
            entry("function", Keyword.FUNCTION),
            entry("boolean", Keyword.BOOLEAN),
            entry("constructor", Keyword.CONSTRUCTOR),
            entry("char", Keyword.CHAR),
            entry("void", Keyword.VOID),
            entry("var", Keyword.VAR),
            entry("static", Keyword.STATIC),
            entry("field", Keyword.FIELD),
            entry("let", Keyword.LET),
            entry("do", Keyword.DO),
            entry("if", Keyword.IF),
            entry("else", Keyword.ELSE),
            entry("while", Keyword.WHILE),
            entry("return", Keyword.RETURN),
            entry("true", Keyword.TRUE),
            entry("false", Keyword.FALSE),
            entry("null", Keyword.NULL),
            entry("this", Keyword.THIS));
    private static final HashSet<Integer> symbols = "{}()[].,;+-*/&|<>=~"
            .chars().mapToObj(c -> (int) c).collect(Collectors.toCollection(HashSet::new));
    private final ListIterator<Token> tokenIterator;
    private Token currentToken;

    public JackTokenizer(String fileName) throws IOException {
        BufferedReader buf = new BufferedReader(new FileReader(fileName));
        List<Token> tokenList = new ArrayList<>();
        int c = buf.read();
        while (c != -1) {
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                c = buf.read();
                continue;
            }
            if (c == '/') {
                c = buf.read();
                if (c == '/') {
                    while (c != '\n' && c != -1)
                        c = buf.read();
                    continue;
                } else if (c == '*') {
                    int prev = ' ';
                    for (; c != -1; prev = c, c = buf.read())
                        if (prev == '*' && c == '/') break;
                    c = buf.read();
                    continue;
                } else {
                    tokenList.add(new Token(TokenType.SYMBOL, '/'));
                    continue;
                }
            }
            if (symbols.contains(c)) { // '/' has been handled
                tokenList.add(new Token(TokenType.SYMBOL, (char) c));
                c = buf.read();
                continue;
            }
            if (c == '"') {
                StringBuilder b = new StringBuilder();
                while ((c = buf.read()) != '"' && c != -1) {
                    b.append((char) c);
                }
                tokenList.add(new Token(TokenType.STRING_CONST, b.toString()));
                c = buf.read();
                continue;
            }
            if (Character.isDigit(c)) {
                int v = 0;
                do {
                    v = 10 * v + Character.digit(c, 10);
                    c = buf.read();
                } while (Character.isDigit(c));
                tokenList.add(new Token(TokenType.INT_CONST, v));
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '_') {
                StringBuilder b = new StringBuilder();
                do {
                    b.append((char) c);
                    c = buf.read();
                } while (Character.isLetterOrDigit(c) || c == '_');
                String s = b.toString();
                Keyword word = keywords.get(s);
                if (word != null) {
                    tokenList.add(new Token(TokenType.KEYWORD, word));
                    continue;
                }
                tokenList.add(new Token(TokenType.IDENTIFIER, s));
                continue;
            }
        }
        buf.close();
        tokenIterator = tokenList.listIterator();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: \n\tJackTokenizer <filename>");
            return;
        }
        JackTokenizer tokenizer = new JackTokenizer(args[0]);
        BufferedWriter buf = new BufferedWriter(
                new FileWriter(args[0].substring(0, args[0].length() - 5) + "TTest.xml"));
        buf.write("<tokens>\n");
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            buf.write(tokenizer.toXML());
        }
        buf.write("</tokens>\n");
        buf.close();
    }

    public String toXML() {
        String str = null;
        switch (tokenType()) {
            case KEYWORD -> str = "<keyword> " + keyword().toString().toLowerCase() + " </keyword>\n";
            case IDENTIFIER -> str = "<identifier> " + identifier() + " </identifier>\n";
            case INT_CONST -> str = "<integerConstant> " + intVal() + " </integerConstant>\n";
            case STRING_CONST -> str = "<stringConstant> " + stringVal() + " </stringConstant>\n";
            case SYMBOL -> {
                char c = symbol();
                String s;
                if (c == '<') s = "&lt;";
                else if (c == '>') s = "&gt;";
                else if (c == '&') s = "&amp;";
                else s = String.valueOf(c);
                str = "<symbol> " + s + " </symbol>\n";
            }
        }
        return str;
    }

    public boolean hasMoreTokens() {
        return tokenIterator.hasNext();
    }

    public void advance() {
        currentToken = tokenIterator.next();
    }

    public void back() {
        currentToken = tokenIterator.previous();
    }

    public TokenType tokenType() {
        return currentToken.type();
    }

    public Keyword keyword() {
        return (Keyword) currentToken.data();
    }

    public char symbol() {
        return (char) currentToken.data();
    }

    public boolean isOp() {
        switch (symbol()) {
            case '+', '-', '*', '/', '&', '|', '<', '>', '=' -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public String identifier() {
        return (String) currentToken.data();
    }

    public int intVal() {
        return (int) currentToken.data();
    }

    public String stringVal() {
        return (String) currentToken.data();
    }

    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER,
        INT_CONST, STRING_CONST
    }

    public enum Keyword {
        CLASS, METHOD, INT, FUNCTION, BOOLEAN,
        CONSTRUCTOR, CHAR, VOID, VAR, STATIC, FIELD,
        LET, DO, IF, ELSE, WHILE, RETURN,
        TRUE, FALSE, NULL, THIS
    }

    private record Token(TokenType type, Object data) {
    }

}