package minigroovy.ast

class ForStmt implements Stmt {
    final Stmt init        // може бути null
    final Expr cond        // може бути null (=> true)
    final Stmt post        // може бути null
    final Stmt body
    ForStmt(Stmt init, Expr cond, Stmt post, Stmt body) {
        this.init = init; this.cond = cond; this.post = post; this.body = body
    }
    String toString() {
        "for(${init ?: ';'} ${cond ?: 'true'}; ${post ?: ''}) ${body}"
    }
}
