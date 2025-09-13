package minigroovy.ast

class LiteralExpr implements Expr {
    final Object value   // Integer, Double, Boolean, String (пізніше)
    LiteralExpr(Object v) { this.value = v }
    String toString() { value instanceof String ? "\"$value\"" : String.valueOf(value) }
}
