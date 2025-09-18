package minigroovy.ast

class ReturnStmt implements Stmt {
    final Expr expr

    ReturnStmt(Expr expr) { this.expr = expr }

    String toString() { "return ${expr};" }
}
