package minigroovy.ast

class ConstDeclStmt implements Stmt {
    final String name
    final String typeName
    final Expr init
    ConstDeclStmt(String name, String typeName, Expr init) {
        this.name = name; this.typeName = typeName; this.init = init
    }
    String toString() { "const ${name}: ${typeName} = ${init};" }
}
