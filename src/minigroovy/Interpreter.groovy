package minigroovy

import minigroovy.ast.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.HashSet

class Interpreter {

    static class Env {
        final Map<String, Object> vars  = [:]
        final Map<String, String> types = [:]
        final Set<String> consts = new HashSet<String>()
    }

    static class FuncDef {
        final String name
        final java.util.List<Param> params
        final String returnType
        final Stmt body
        FuncDef(String name, java.util.List<Param> params, String returnType, Stmt body) {
            this.name = name; this.params = params; this.returnType = returnType; this.body = body
        }
    }

    private final java.util.List<Env> envStack = [ new Env() ]
    private final Map<String, FuncDef> functions = [:]
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
    private int callDepth = 0
    private Env current() { envStack.get(envStack.size() - 1) }

    private static class ReturnSignal extends RuntimeException { final Object value; ReturnSignal(Object v){ super(null,null,false,false); this.value=v } }
    private static class BreakSignal  extends RuntimeException { BreakSignal(){ super(null,null,false,false) } }
    private static class ContinueSignal extends RuntimeException { ContinueSignal(){ super(null,null,false,false) } }

    void run(java.util.List<Stmt> program) { program.each { exec(it) } }

    private void exec(Stmt s) {
        switch (s) {
            case VarDeclStmt:   varDecl((VarDeclStmt) s);     return
            case ConstDeclStmt: constDecl((ConstDeclStmt) s); return
            case FuncDeclStmt:  funcDecl((FuncDeclStmt) s);   return
            case AssignStmt:    assign((AssignStmt) s);       return
            case PrintStmt:
                def v = eval(((PrintStmt) s).expr); println(valueToString(v)); return
            case IfStmt:        doIf((IfStmt) s);             return
            case WhileStmt:     doWhile((WhileStmt) s);       return
            case ForStmt:       doFor((ForStmt) s);           return   // <-- нове
            case SwitchStmt:    doSwitch((SwitchStmt) s);     return
            case BlockStmt:
                envStack.add(new Env())
                try { ((BlockStmt) s).statements.each { exec(it) } }
                finally { envStack.remove(envStack.size() - 1) }
                return
            case ExprStmt:      eval(((ExprStmt) s).expr);    return
            case ReadStmt:      doRead((ReadStmt) s);         return
            case ReturnStmt:
                if (callDepth <= 0) throw rt("return outside of function")
                def val = eval(((ReturnStmt) s).expr)
                throw new ReturnSignal(val)
            case BreakStmt:     throw new BreakSignal()
            case ContinueStmt:  throw new ContinueSignal()
            default: throw rt("Unknown statement: ${s?.class?.simpleName}")
        }
    }

    private void varDecl(VarDeclStmt s) {
        Env e = current()
        if (e.vars.containsKey(s.name)) throw rt("Variable '${s.name}' already declared in this scope")
        def v = eval(s.init)
        checkAssignableToType(v, s.typeName, "initializer of '${s.name}'")
        e.types[s.name] = s.typeName
        e.vars[s.name]  = coerceToDeclaredType(v, s.typeName)
    }

    private void constDecl(ConstDeclStmt s) {
        Env e = current()
        if (e.vars.containsKey(s.name)) throw rt("Name '${s.name}' already declared in this scope")
        def v = eval(s.init)
        checkAssignableToType(v, s.typeName, "initializer of const '${s.name}'")
        e.types[s.name] = s.typeName
        e.vars[s.name]  = coerceToDeclaredType(v, s.typeName)
        e.consts.add(s.name)
    }

    private void funcDecl(FuncDeclStmt s) {
        if (functions.containsKey(s.name)) throw rt("Function '${s.name}' already declared")
        functions[s.name] = new FuncDef(s.name, s.params, s.returnType, s.body)
    }

    private void assign(AssignStmt s) {
        int idx = findVarFrameIndex(s.name)
        if (idx < 0) throw rt("Undeclared variable '${s.name}'")
        Env e = envStack.get(idx)
        if (e.consts.contains(s.name)) throw rt("Cannot assign to const '${s.name}'")
        def v = eval(s.expr)
        def t = e.types[s.name]
        checkAssignableToType(v, t, "assignment to '${s.name}'")
        e.vars[s.name] = coerceToDeclaredType(v, t)
    }

    private void doIf(IfStmt s) {
        def c = eval(s.condition)
        if (!(c instanceof Boolean)) throw rt("if-condition must be bool")
        if (c) exec(s.thenBranch) else if (s.elseBranch != null) exec(s.elseBranch)
    }

    private void doWhile(WhileStmt s) {
        while (true) {
            def c = eval(s.condition)
            if (!(c instanceof Boolean)) throw rt("while-condition must be bool")
            if (!c) break
            try {
                exec(s.body)
            } catch (ContinueSignal ign) {
                continue
            } catch (BreakSignal br) {
                break
            }
        }
    }

    // головна логіка for: свій scope + правильна обробка continue/break відносно post
    private void doFor(ForStmt s) {
        envStack.add(new Env())
        try {
            if (s.init != null) exec(s.init)
            while (true) {
                def ok = (s.cond == null) ? true : eval(s.cond)
                if (!(ok instanceof Boolean)) throw rt("for-condition must be bool")
                if (!ok) break
                boolean didBreak = false
                try {
                    exec(s.body)
                } catch (ContinueSignal ign) {
                    // просто перейти до пост-частини
                } catch (BreakSignal br) {
                    didBreak = true
                }
                if (didBreak) break
                if (s.post != null) exec(s.post)
            }
        } finally {
            envStack.remove(envStack.size() - 1)
        }
    }

    private void doSwitch(SwitchStmt s) {
        def subj = eval(s.subject)
        boolean matched = false
        for (CaseClause cc : s.cases) {
            def v = eval(cc.value)
            if (eq(subj, v)) {
                try { exec(cc.body) }
                catch (BreakSignal br) { /* вихід лише зі switch */ }
                catch (ContinueSignal ct) { throw rt("continue used outside of loop") }
                matched = true
                break
            }
        }
        if (!matched && s.defaultBody != null) {
            try { exec(s.defaultBody) }
            catch (BreakSignal br) { /* ок */ }
            catch (ContinueSignal ct) { throw rt("continue used outside of loop") }
        }
    }

    private void doRead(ReadStmt s) {
        int idx = findVarFrameIndex(s.varName)
        if (idx < 0) throw rt("Undeclared variable '${s.varName}'")
        Env e = envStack.get(idx)
        if (e.consts.contains(s.varName)) throw rt("Cannot read into const '${s.varName}'")
        def declared = e.types[s.varName]

        String prompt
        switch (s.kind) {
            case ReadStmt.Kind.INT:    prompt = "Enter int ${s.varName}: "; break
            case ReadStmt.Kind.DOUBLE: prompt = "Enter double ${s.varName}: "; break
            case ReadStmt.Kind.BOOL:   prompt = "Enter bool ${s.varName} (true/false): "; break
            case ReadStmt.Kind.STRING: prompt = "Enter string ${s.varName}: "; break
            default: prompt = "Enter value ${s.varName}: "
        }
        System.out.print(prompt)
        String line = reader.readLine()

        Object value
        switch (s.kind) {
            case ReadStmt.Kind.INT:
                try { value = Integer.parseInt(line.trim()) } catch (e2) { throw rt("Invalid int input for ${s.varName}") }
                break
            case ReadStmt.Kind.DOUBLE:
                try { value = Double.parseDouble(line.trim()) } catch (e2) { throw rt("Invalid double input for ${s.varName}") }
                break
            case ReadStmt.Kind.BOOL:
                String t = line.trim().toLowerCase()
                if (t == "true" || t == "1") value = true
                else if (t == "false" || t == "0") value = false
                else throw rt("Invalid bool input for ${s.varName}")
                break
            case ReadStmt.Kind.STRING:
                value = line
                break
        }

        checkAssignableToType(value, declared, "read into '${s.varName}'")
        e.vars[s.varName] = coerceToDeclaredType(value, declared)
    }

    private Object eval(Expr e) {
        switch (e) {
            case LiteralExpr: return ((LiteralExpr) e).value
            case VarExpr:     return getVarValue(((VarExpr) e).name)
            case UnaryExpr:   return evalUnary((UnaryExpr) e)
            case BinaryExpr:  return evalBinary((BinaryExpr) e)
            case CallExpr:    return evalCall((CallExpr) e)
            default: throw rt("Unknown expression: ${e?.class?.simpleName}")
        }
    }

    private Object evalCall(CallExpr c) {
        FuncDef f = functions[c.callee]
        if (f == null) throw rt("Undefined function '${c.callee}'")
        if (c.args.size() != f.params.size())
            throw rt("Function '${f.name}' expects ${f.params.size()} args, got ${c.args.size()}")

        def values = new ArrayList<Object>()
        for (int i=0;i<c.args.size();i++) values.add(eval(c.args.get(i)))

        Env frame = new Env()
        for (int i=0;i<f.params.size();i++) {
            def p = f.params.get(i)
            def v = values.get(i)
            checkAssignableToType(v, p.typeName, "argument ${i+1} of '${f.name}'")
            frame.types[p.name] = p.typeName
            frame.vars[p.name]  = coerceToDeclaredType(v, p.typeName)
        }

        envStack.add(frame)
        callDepth++
        try {
            exec(f.body)
            throw rt("Missing return in function '${f.name}'")
        } catch (ReturnSignal rs) {
            def ret = rs.value
            checkAssignableToType(ret, f.returnType, "return from '${f.name}'")
            return coerceToDeclaredType(ret, f.returnType)
        } finally {
            callDepth--
            envStack.remove(envStack.size()-1)
        }
    }

    private Object evalUnary(UnaryExpr u) {
        def v = eval(u.expr)
        switch (u.op) {
            case '+': return num(v)
            case '-':
                def n = num(v)
                return (n instanceof Double) ? -((Double)n) : -((Integer)n)
            case '!':
                if (!(v instanceof Boolean)) throw rt("Operand of '!' must be bool")
                return !((Boolean)v)
            default: throw rt("Unsupported unary op '${u.op}'")
        }
    }

    private Object evalBinary(BinaryExpr b) {
        def L = eval(b.left)
        def R = eval(b.right)
        switch (b.op) {
            case '&&':
                if (!(L instanceof Boolean)) throw rt("Left operand of '&&' must be bool")
                return ((Boolean)L) && asBool(R)
            case '||':
                if (!(L instanceof Boolean)) throw rt("Left operand of '||' must be bool")
                return ((Boolean)L) || asBool(R)
            case '==': return eq(L, R)
            case '!=': return !eq(L, R)
            case '<':  return cmp(L, R)  < 0
            case '<=': return cmp(L, R) <= 0
            case '>':  return cmp(L, R)  > 0
            case '>=': return cmp(L, R) >= 0
            case '+':  return add(L, R)
            case '-':  return sub(L, R)
            case '*':  return mul(L, R)
            case '/':  return div(L, R)
            case '**': return pow(L, R)
            case '%':  return mod(L, R)
            default: throw rt("Unsupported binary op '${b.op}'")
        }
    }

    private int findVarFrameIndex(String name) {
        for (int i = envStack.size()-1; i >= 0; i--) if (envStack[i].vars.containsKey(name)) return i
        return -1
    }
    private Object getVarValue(String name) {
        int idx = findVarFrameIndex(name)
        if (idx < 0) throw rt("Undeclared variable '$name'")
        return envStack[idx].vars[name]
    }

    private Number num(Object v) {
        if (v instanceof Integer || v instanceof Double) return (Number)v
        throw rt("Numeric value expected, got ${typeName(v)}")
    }

    private java.util.List<Number> promote2(Object a, Object b) {
        if (a instanceof String || b instanceof String) return null
        Number A = num(a), B = num(b)
        if (A instanceof Double || B instanceof Double) return [A.doubleValue(), B.doubleValue()]
        return [A.intValue(), B.intValue()]
    }

    private Object add(Object a, Object b) {
        if (a instanceof String || b instanceof String) {
            return String.valueOf(a instanceof String ? a : valueToString(a)) +
                    String.valueOf(b instanceof String ? b : valueToString(b))
        }
        def (x,y) = promote2(a,b)
        return (x instanceof Double || y instanceof Double) ? ((double)x + (double)y) : ((int)x + (int)y)
    }
    private Object mod(Object a, Object b) {
        def pr = promote2(a, b)
        if (pr == null) throw rt("Numeric value expected for '%'")
        def (x, y) = pr
        if (x instanceof Double || y instanceof Double) {
            double yy = ((Number) y).doubleValue()
            if (yy == 0.0d) throw rt("division by zero in '%'")
            return ((Number) x).doubleValue() % yy
        } else {
            int yi = ((Number) y).intValue()
            if (yi == 0) throw rt("division by zero in '%'")
            return ((Number) x).intValue() % yi
        }
    }

    private Object sub(Object a, Object b) { def (x,y)=promote2(a,b); (x instanceof Double || y instanceof Double)?((double)x-(double)y):((int)x-(int)y) }
    private Object mul(Object a, Object b) { def (x,y)=promote2(a,b); (x instanceof Double || y instanceof Double)?((double)x*(double)y):((int)x*(int)y) }
    private Object div(Object a, Object b) { def (x,y)=promote2(a,b); ((double)x) / ((double)y) }
    private Object pow(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            int base = (Integer) a
            int exp  = (Integer) b
            if (exp >= 0) {
                long res = 1, cur = base
                int e = exp
                while (e > 0) {
                    if ((e & 1) == 1) { res *= cur; if (res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) return Math.pow((double)base,(double)exp) }
                    cur *= cur; if (cur > Integer.MAX_VALUE || cur < Integer.MIN_VALUE) return Math.pow((double)base,(double)exp)
                    e >>= 1
                }
                return (int)res
            }
            return Math.pow((double)base,(double)exp)
        }
        def (x,y)=promote2(a,b)
        return Math.pow(((Number)x).doubleValue(), ((Number)y).doubleValue())
    }

    private int cmp(Object a, Object b) {
        if (a instanceof String || b instanceof String) throw rt("Cannot compare non-numeric values with <,>,<=,>=")
        def (x, y) = promote2(a, b)
        double d = ((Number)x).doubleValue() - ((Number)y).doubleValue()
        return d < 0 ? -1 : (d > 0 ? 1 : 0)
    }

    private boolean eq(Object a, Object b) {
        if ((a instanceof Integer || a instanceof Double) &&
                (b instanceof Integer || b instanceof Double)) {
            def (x, y) = promote2(a, b)
            return Double.compare(((Number)x).doubleValue(), ((Number)y).doubleValue()) == 0
        }
        if ((a instanceof Boolean) && (b instanceof Boolean)) return a == b
        if ((a instanceof String)  && (b instanceof String))  return a == b
        return false
    }

    private boolean asBool(Object v) {
        if (!(v instanceof Boolean)) throw rt("Boolean value expected, got ${typeName(v)}")
        return (Boolean)v
    }

    private void checkAssignableToType(Object value, String declared, String where) {
        switch (declared) {
            case "int":
                if (!(value instanceof Integer)) throw rt("$where: expected int, got ${typeName(value)}"); return
            case "double":
                if (!(value instanceof Integer || value instanceof Double)) throw rt("$where: expected double, got ${typeName(value)}"); return
            case "bool":
                if (!(value instanceof Boolean)) throw rt("$where: expected bool, got ${typeName(value)}"); return
            case "string":
                if (!(value instanceof String)) throw rt("$where: expected string, got ${typeName(value)}"); return
            default:
                throw rt("Unknown declared type '$declared'")
        }
    }

    private Object coerceToDeclaredType(Object value, String declared) {
        switch (declared) {
            case "int":    return (value instanceof Integer) ? value : ((Number)value).intValue()
            case "double": return (value instanceof Double) ? value
                    : (value instanceof Integer ? ((Integer)value).doubleValue() : ((Number)value).doubleValue())
            case "bool":   return (Boolean)value
            case "string": return (String)value
            default:       return value
        }
    }

    private static String valueToString(Object v) {
        if (v instanceof Double) {
            double d = (Double)v
            if (d == (long)d) return String.valueOf((long)d)
        }
        return String.valueOf(v)
    }

    private static RuntimeException rt(String msg) { new RuntimeException("Runtime error: $msg") }
    private static String typeName(Object v) {
        if (v == null) return "null"
        if (v instanceof Integer) return "int"
        if (v instanceof Double)  return "double"
        if (v instanceof Boolean) return "bool"
        if (v instanceof String)  return "string"
        return v.getClass().simpleName
    }
}
