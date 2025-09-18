package minigroovy.ast

class BlockStmt implements Stmt {
    final java.util.List<Stmt> statements

    BlockStmt(java.util.List<Stmt> statements) { this.statements = statements }

    String toString() { "{ " + statements.collect { it.toString() }.join(" ") + " }" }
}
