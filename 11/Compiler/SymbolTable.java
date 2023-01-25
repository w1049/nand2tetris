package Compiler;

import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, Identifier> classTable;
    private final HashMap<Kind, Integer> counter;
    private HashMap<String, Identifier> subroutineTable;

    public SymbolTable() {
        classTable = new HashMap<>();
        subroutineTable = new HashMap<>();
        counter = new HashMap<>();
        counter.put(Kind.STATIC, 0);
        counter.put(Kind.FIELD, 0);
        counter.put(Kind.ARG, 0);
        counter.put(Kind.VAR, 0);
    }

    public void startSubroutine() {
        subroutineTable = new HashMap<>();
        counter.put(Kind.ARG, 0);
        counter.put(Kind.VAR, 0);
    }

    public void define(String name, String type, Kind kind) {
        if (kind == Kind.STATIC || kind == Kind.FIELD)
            classTable.put(name, new Identifier(name, type, kind, counter.get(kind)));
        else
            subroutineTable.put(name, new Identifier(name, type, kind, counter.get(kind)));
        counter.put(kind, counter.get(kind) + 1);
    }

    public int varCount(Kind kind) {
        return counter.get(kind);
    }

    private Identifier get(String name) {
        Identifier identifier = subroutineTable.get(name);
        if (identifier != null) return identifier;
        return classTable.get(name);
    }

    public Kind kindOf(String name) {
        Identifier identifier = get(name);
        if (identifier != null) return identifier.kind();
        return Kind.NONE;
    }

    public String typeOf(String name) {
        Identifier identifier = get(name);
        if (identifier != null) return identifier.type();
        return null;
    }

    public int indexOf(String name) {
        Identifier identifier = get(name);
        if (identifier != null) return identifier.index();
        return -1;
    }

    public enum Kind {
        STATIC, FIELD, ARG, VAR, NONE
    }

    private record Identifier(String name, String type, Kind kind, int index) {
    }
}
