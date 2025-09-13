package minigroovy.ast

class FuncDeclStmt implements Stmt {
    final String name
    final java.util.List<Param> params
    final String returnType
    final Stmt body   // зазвичай BlockStmt

    FuncDeclStmt(String name, java.util.List<Param> params, String returnType, Stmt body) {
        this.name = name; this.params = params; this.returnType = returnType; this.body = body
    }
    String toString() { "func ${name}(${params?.join(', ')}) : ${returnType} ${body}" }
}
