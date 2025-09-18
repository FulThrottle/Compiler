package minigroovy.ast

class UnaryExpr implements Expr {
    final String op
    final Expr expr

    UnaryExpr(String op, Expr e) { this.op = op; this.expr = e }

    String toString() { "(${op}${expr})" }
}
