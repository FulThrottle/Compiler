package minigroovy.ast

class BinaryExpr implements Expr {
    final Expr left
    final String op
    final Expr right

    BinaryExpr(Expr l, String op, Expr r) { this.left = l; this.op = op; this.right = r }

    String toString() { "(${left} ${op} ${right})" }
}
