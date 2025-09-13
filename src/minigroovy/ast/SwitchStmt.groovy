package minigroovy.ast

class SwitchStmt implements Stmt {
    final Expr subject
    final java.util.List<CaseClause> cases
    final Stmt defaultBody  // може бути null

    SwitchStmt(Expr subject, java.util.List<CaseClause> cases, Stmt defaultBody) {
        this.subject = subject; this.cases = cases; this.defaultBody = defaultBody
    }

    String toString() {
        def cs = cases.collect { it.toString() }.join(' ')
        return "switch (${subject}) { ${cs} ${defaultBody ? 'default: ' + defaultBody : ''} }"
    }
}
