package minigroovy.ast

class PrintStmt implements Stmt {
    final Expr expr
    PrintStmt(Expr expr) { this.expr = expr }
    String toString() { "print(${expr});" }
}
