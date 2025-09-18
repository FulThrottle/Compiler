package minigroovy.ast

class CaseClause {
    final Expr value    // літерал
    final Stmt body     // одна інструкція або блок
    CaseClause(Expr value, Stmt body) { this.value = value; this.body = body }

    String toString() { "case ${value}: ${body}" }
}
