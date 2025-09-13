package minigroovy

import minigroovy.ast.*

class Parser {
    private final Lexer lx
    private Token cur

    Parser(Lexer lx) { this.lx = lx; this.cur = lx.next() }

    java.util.List<Stmt> parseProgram() {
        def res = new ArrayList<Stmt>()
        while (cur.type != TokenType.EOF) res.add(parseStmt())
        return res
    }

    Expr parseExpression() { parseOr() }

    private Stmt parseStmt() {
        switch (cur.type) {
            case TokenType.LBrace:  return parseBlock()
            case TokenType.Let:     return parseVarDecl()
            case TokenType.Const:   return parseConstDecl()
            case TokenType.Func:    return parseFuncDecl()
            case TokenType.Return:  return parseReturn()
            case TokenType.Switch:  return parseSwitch()
            case TokenType.If:      return parseIf()
            case TokenType.While:   return parseWhile()
            case TokenType.For:     return parseFor()          // <-- нове
            case TokenType.Break:   return parseBreak()
            case TokenType.Continue:return parseContinue()
            case TokenType.Print:   return parsePrint()
            case TokenType.ReadInt:
            case TokenType.ReadDouble:
            case TokenType.ReadBool:
            case TokenType.ReadString:
                return parseRead()
            default:                return parseExprStmt()
        }
    }

    private BlockStmt parseBlock() {
        eat(TokenType.LBrace)
        def list = new ArrayList<Stmt>()
        while (cur.type != TokenType.RBrace) list.add(parseStmt())
        eat(TokenType.RBrace)
        return new BlockStmt(list)
    }

    private VarDeclStmt parseVarDecl() {
        eat(TokenType.Let)
        String name = expectIdentifier("variable name")
        eat(TokenType.Colon)
        String typeName = expectIdentifier("type name")
        eat(TokenType.Assign)
        Expr init = parseExpression()
        eat(TokenType.Semicolon)
        return new VarDeclStmt(name, typeName, init)
    }

    // спеціальна версія для 'for (...)' — без фінальної ';'
    private VarDeclStmt parseVarDeclNoSemi() {
        eat(TokenType.Let)
        String name = expectIdentifier("variable name")
        eat(TokenType.Colon)
        String typeName = expectIdentifier("type name")
        eat(TokenType.Assign)
        Expr init = parseExpression()
        return new VarDeclStmt(name, typeName, init)
    }

    private ConstDeclStmt parseConstDecl() {
        eat(TokenType.Const)
        String name = expectIdentifier("const name")
        eat(TokenType.Colon)
        String typeName = expectIdentifier("type name")
        eat(TokenType.Assign)
        Expr init = parseExpression()
        eat(TokenType.Semicolon)
        return new ConstDeclStmt(name, typeName, init)
    }

    private FuncDeclStmt parseFuncDecl() {
        eat(TokenType.Func)
        String name = expectIdentifier("function name")
        eat(TokenType.LParen)
        def params = new ArrayList<Param>()
        if (cur.type != TokenType.RParen) {
            while (true) {
                String pName = expectIdentifier("parameter name")
                eat(TokenType.Colon)
                String pType = expectIdentifier("type name")
                params.add(new Param(pName, pType))
                if (cur.type == TokenType.Comma) { eat(TokenType.Comma); continue }
                break
            }
        }
        eat(TokenType.RParen)
        eat(TokenType.Colon)
        String retType = expectIdentifier("return type")
        Stmt body = parseStmt()
        return new FuncDeclStmt(name, params, retType, body)
    }

    private ReturnStmt parseReturn() {
        eat(TokenType.Return)
        Expr e = parseExpression()
        eat(TokenType.Semicolon)
        return new ReturnStmt(e)
    }

    // switch (Expr) { case Expr: Stmt ... default: Stmt }
    private SwitchStmt parseSwitch() {
        eat(TokenType.Switch)
        eat(TokenType.LParen)
        Expr subj = parseExpression()
        eat(TokenType.RParen)
        eat(TokenType.LBrace)

        def cases = new ArrayList<CaseClause>()
        Stmt defBody = null

        while (cur.type == TokenType.Case) {
            eat(TokenType.Case)
            Expr val = parseExpression()
            eat(TokenType.Colon)
            Stmt body = parseStmt()
            cases.add(new CaseClause(val, body))
        }
        if (cur.type == TokenType.Default) {
            eat(TokenType.Default)
            eat(TokenType.Colon)
            defBody = parseStmt()
        }
        eat(TokenType.RBrace)
        return new SwitchStmt(subj, cases, defBody)
    }

    private IfStmt parseIf() {
        eat(TokenType.If)
        eat(TokenType.LParen)
        Expr cond = parseExpression()
        eat(TokenType.RParen)
        Stmt thenB = parseStmt()
        Stmt elseB = null
        if (cur.type == TokenType.Else) { eat(TokenType.Else); elseB = parseStmt() }
        return new IfStmt(cond, thenB, elseB)
    }

    private WhileStmt parseWhile() {
        eat(TokenType.While)
        eat(TokenType.LParen)
        Expr cond = parseExpression()
        eat(TokenType.RParen)
        Stmt body = parseStmt()
        return new WhileStmt(cond, body)
    }

    // ----------- FOR -----------
    private Stmt parseFor() {
        eat(TokenType.For)
        eat(TokenType.LParen)

        Stmt init = null
        if (cur.type != TokenType.Semicolon) {
            if (cur.type == TokenType.Let) {
                init = parseVarDeclNoSemi()
            } else if (cur.type == TokenType.Identifier) {
                // або присвоювання, або вираз, що починається з ідентифікатора
                String id = cur.lexeme; eat(TokenType.Identifier)
                if (cur.type == TokenType.Assign) {
                    eat(TokenType.Assign)
                    Expr rhs = parseExpression()
                    init = new AssignStmt(id, rhs)
                } else {
                    Expr left = parseVarOrCall(id)
                    Expr tail = parseOrStartingWith(left)
                    init = new ExprStmt(tail)
                }
            } else {
                // довільний вираз
                Expr e = parseExpression()
                init = new ExprStmt(e)
            }
        }
        eat(TokenType.Semicolon)

        Expr cond = null
        if (cur.type != TokenType.Semicolon) {
            cond = parseExpression()
        }
        eat(TokenType.Semicolon)

        Stmt post = null
        if (cur.type != TokenType.RParen) {
            if (cur.type == TokenType.Identifier) {
                String id2 = cur.lexeme; eat(TokenType.Identifier)
                if (cur.type == TokenType.Assign) {
                    eat(TokenType.Assign)
                    Expr rhs2 = parseExpression()
                    post = new AssignStmt(id2, rhs2)
                } else {
                    Expr left2 = parseVarOrCall(id2)
                    Expr tail2 = parseOrStartingWith(left2)
                    post = new ExprStmt(tail2)
                }
            } else {
                Expr e2 = parseExpression()
                post = new ExprStmt(e2)
            }
        }
        eat(TokenType.RParen)

        Stmt body = parseStmt()
        return new ForStmt(init, cond, post, body)
    }
    // ---------------------------

    private Stmt parseBreak() {
        eat(TokenType.Break)
        eat(TokenType.Semicolon)
        return new BreakStmt()
    }

    private Stmt parseContinue() {
        eat(TokenType.Continue)
        eat(TokenType.Semicolon)
        return new ContinueStmt()
    }

    private PrintStmt parsePrint() {
        eat(TokenType.Print)
        eat(TokenType.LParen)
        Expr e = parseExpression()
        eat(TokenType.RParen)
        eat(TokenType.Semicolon)
        return new PrintStmt(e)
    }

    private ReadStmt parseRead() {
        ReadStmt.Kind k
        if      (cur.type == TokenType.ReadInt)     { k = ReadStmt.Kind.INT;    eat(TokenType.ReadInt) }
        else if (cur.type == TokenType.ReadDouble)  { k = ReadStmt.Kind.DOUBLE; eat(TokenType.ReadDouble) }
        else if (cur.type == TokenType.ReadBool)    { k = ReadStmt.Kind.BOOL;   eat(TokenType.ReadBool) }
        else                                        { k = ReadStmt.Kind.STRING; eat(TokenType.ReadString) }

        eat(TokenType.LParen)
        String name = expectIdentifier("variable name")
        eat(TokenType.RParen)
        eat(TokenType.Semicolon)
        return new ReadStmt(k, name)
    }

    private Stmt parseExprStmt() {
        if (cur.type == TokenType.Identifier) {
            String id = cur.lexeme; eat(TokenType.Identifier)
            if (cur.type == TokenType.Assign) {
                eat(TokenType.Assign)
                Expr rhs = parseExpression()
                eat(TokenType.Semicolon)
                return new AssignStmt(id, rhs)
            } else {
                Expr left = parseVarOrCall(id)
                Expr tail = parseOrStartingWith(left)
                eat(TokenType.Semicolon)
                return new ExprStmt(tail)
            }
        } else {
            Expr e = parseExpression()
            eat(TokenType.Semicolon)
            return new ExprStmt(e)
        }
    }

    // ----- вирази -----
    private Expr parseOr() {
        Expr node = parseAnd()
        while (cur.type == TokenType.OrOr) { String op = cur.lexeme; eat(TokenType.OrOr); Expr r = parseAnd(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseAnd() {
        Expr node = parseEqual()
        while (cur.type == TokenType.AndAnd) { String op = cur.lexeme; eat(TokenType.AndAnd); Expr r = parseEqual(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseEqual() {
        Expr node = parseRel()
        while (cur.type in [TokenType.EqEq, TokenType.NotEq]) { String op = cur.lexeme; eat(cur.type); Expr r = parseRel(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseRel() {
        Expr node = parseAdd()
        while (cur.type in [TokenType.Lt, TokenType.Lte, TokenType.Gt, TokenType.Gte]) { String op = cur.lexeme; eat(cur.type); Expr r = parseAdd(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseAdd() {
        Expr node = parseMul()
        while (cur.type in [TokenType.Plus, TokenType.Minus]) { String op = cur.lexeme; eat(cur.type); Expr r = parseMul(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseMul() {
        Expr node = parsePow()
        while (cur.type in [TokenType.Star, TokenType.Slash, TokenType.Mod]) { // <-- додали Mod
            String op = cur.lexeme
            eat(cur.type)
            Expr r = parsePow()
            node = new BinaryExpr(node, op, r)
        }
        return node
    }

    private Expr parsePow() {
        Expr left = parseUnary()
        if (cur.type == TokenType.Pow) { eat(TokenType.Pow); Expr right = parsePow(); return new BinaryExpr(left, "**", right) }
        return left
    }
    private Expr parseUnary() {
        if (cur.type in [TokenType.Plus, TokenType.Minus, TokenType.Bang]) { String op = cur.lexeme; eat(cur.type); return new UnaryExpr(op, parseUnary()) }
        return parsePrimary()
    }

    private Expr parsePrimary() {
        switch (cur.type) {
            case TokenType.Number:
                String lex = cur.lexeme; eat(TokenType.Number)
                def val = (lex.contains(".") || lex.contains("e") || lex.contains("E")) ? Double.valueOf(lex) : Integer.valueOf(lex)
                return new LiteralExpr(val)
            case TokenType.StringLit:
                String s = cur.lexeme; eat(TokenType.StringLit)
                return new LiteralExpr(s)
            case TokenType.True:  eat(TokenType.True);  return new LiteralExpr(true)
            case TokenType.False: eat(TokenType.False); return new LiteralExpr(false)
            case TokenType.Identifier:
                String name = cur.lexeme; eat(TokenType.Identifier)
                return parseVarOrCall(name)
            case TokenType.LParen:
                eat(TokenType.LParen)
                Expr inner = parseExpression()
                eat(TokenType.RParen)
                return inner
            default:
                throw error("Unexpected token in expression: ${cur.type}")
        }
    }

    private Expr parseVarOrCall(String name) {
        if (cur.type == TokenType.LParen) {
            eat(TokenType.LParen)
            def args = new ArrayList<Expr>()
            if (cur.type != TokenType.RParen) {
                while (true) {
                    args.add(parseExpression())
                    if (cur.type == TokenType.Comma) { eat(TokenType.Comma); continue }
                    break
                }
            }
            eat(TokenType.RParen)
            return new CallExpr(name, args)
        }
        return new VarExpr(name)
    }

    // start-with ланцюжки (для ExprStmt та спец-випадків у for)
    private Expr parseOrStartingWith(Expr first) {
        Expr node = parseAndStartingWith(first)
        while (cur.type == TokenType.OrOr) { String op = cur.lexeme; eat(TokenType.OrOr); Expr r = parseAnd(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseAndStartingWith(Expr first) {
        Expr node = parseEqualStartingWith(first)
        while (cur.type == TokenType.AndAnd) { String op = cur.lexeme; eat(TokenType.AndAnd); Expr r = parseEqual(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseEqualStartingWith(Expr first) {
        Expr node = parseRelStartingWith(first)
        while (cur.type in [TokenType.EqEq, TokenType.NotEq]) { String op = cur.lexeme; eat(cur.type); Expr r = parseRel(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseRelStartingWith(Expr first) {
        Expr node = parseAddStartingWith(first)
        while (cur.type in [TokenType.Lt, TokenType.Lte, TokenType.Gt, TokenType.Gte]) { String op = cur.lexeme; eat(cur.type); Expr r = parseAdd(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseAddStartingWith(Expr first) {
        Expr node = parseMulStartingWith(first)
        while (cur.type in [TokenType.Plus, TokenType.Minus]) { String op = cur.lexeme; eat(cur.type); Expr r = parseMul(); node = new BinaryExpr(node, op, r) }
        return node
    }
    private Expr parseMulStartingWith(Expr first) {
        Expr node = parsePowStartingWith(first)
        while (cur.type in [TokenType.Star, TokenType.Slash, TokenType.Mod]) { // <-- додали Mod
            String op = cur.lexeme
            eat(cur.type)
            Expr r = parsePow()
            node = new BinaryExpr(node, op, r)
        }
        return node
    }

    private Expr parsePowStartingWith(Expr first) {
        Expr left = first
        if (cur.type == TokenType.Pow) { eat(TokenType.Pow); Expr right = parsePow(); return new BinaryExpr(left, "**", right) }
        return left
    }

    private void eat(TokenType t) { if (cur.type != t) throw error("Expected $t, got ${cur.type}"); cur = lx.next() }
    private String expectIdentifier(String what) { if (cur.type != TokenType.Identifier) throw error("Expected $what, got ${cur.type}"); String s = cur.lexeme; eat(TokenType.Identifier); return s }
    private RuntimeException error(String msg) { new RuntimeException("Parser error: $msg at pos ${cur.pos}") }
}
