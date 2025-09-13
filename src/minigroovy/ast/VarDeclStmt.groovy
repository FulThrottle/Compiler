package minigroovy.ast

class VarDeclStmt implements Stmt {
    final String name       // ім'я змінної
    final String typeName   // "int" | "double" | "bool" (поки рядком)
    final Expr init         // ініціалізатор

    VarDeclStmt(String name, String typeName, Expr init) {
        this.name = name
        this.typeName = typeName
        this.init = init
    }

    String toString() { "let ${name}: ${typeName} = ${init};" }
}
