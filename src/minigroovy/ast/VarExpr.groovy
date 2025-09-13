package minigroovy.ast

class VarExpr implements Expr {
    final String name
    VarExpr(String n) { this.name = n }
    String toString() { name }
}
