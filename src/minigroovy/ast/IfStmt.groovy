package minigroovy.ast

class IfStmt implements Stmt {
    final Expr condition
    final Stmt thenBranch
    final Stmt elseBranch  // може бути null

    IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch = null) {
        this.condition = condition
        this.thenBranch = thenBranch
        this.elseBranch = elseBranch
    }

    String toString() {
        elseBranch ? "if (${condition}) ${thenBranch} else ${elseBranch}"
                : "if (${condition}) ${thenBranch}"
    }
}
