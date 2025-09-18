package minigroovy.ast

class WhileStmt implements Stmt {
    final Expr condition
    final Stmt body

    WhileStmt(Expr condition, Stmt body) { this.condition = condition; this.body = body }

    String toString() { "while (${condition}) ${body}" }
}
