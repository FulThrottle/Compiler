package minigroovy.ast

class AssignStmt implements Stmt {
    final String name
    final Expr expr
    AssignStmt(String name, Expr expr) { this.name = name; this.expr = expr }
    String toString() { "${name} = ${expr};" }
}
