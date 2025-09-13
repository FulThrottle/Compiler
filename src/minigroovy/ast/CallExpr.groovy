package minigroovy.ast

class CallExpr implements Expr {
    final String callee
    final java.util.List<Expr> args
    CallExpr(String callee, java.util.List<Expr> args) { this.callee = callee; this.args = args }
    String toString() { "${callee}(${args?.join(', ')})" }
}
