package minigroovy.ast

class ExprStmt implements Stmt {
    final Expr expr
    ExprStmt(Expr expr) { this.expr = expr }
    String toString() { "${expr};" }
}
