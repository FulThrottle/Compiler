package minigroovy.ast

class ReadStmt implements Stmt {
    enum Kind { INT, DOUBLE, BOOL, STRING }

    final Kind kind
    final String varName

    ReadStmt(Kind kind, String varName) {
        this.kind = kind
        this.varName = varName
    }

    @Override
    String toString() {
        switch (kind) {
            case Kind.INT:    return "readInt(${varName});"
            case Kind.DOUBLE: return "readDouble(${varName});"
            case Kind.BOOL:   return "readBool(${varName});"
            case Kind.STRING: return "readString(${varName});"
        }
    }
}
