package minigroovy

class Lexer {
    private final String text
    private int pos = 0

    private static final Map<String, TokenType> KEYWORDS = [
            let       : TokenType.Let,
            const     : TokenType.Const,
            if        : TokenType.If,
            else      : TokenType.Else,
            while     : TokenType.While,
            for       : TokenType.For,
            print     : TokenType.Print,
            readInt   : TokenType.ReadInt,
            readDouble: TokenType.ReadDouble,
            readBool  : TokenType.ReadBool,
            readString: TokenType.ReadString,
            func      : TokenType.Func,
            return    : TokenType.Return,
            switch    : TokenType.Switch,
            case      : TokenType.Case,
            default   : TokenType.Default,
            break     : TokenType.Break,
            continue  : TokenType.Continue,
            true      : TokenType.True,
            false     : TokenType.False,
    ]

    Lexer(String text) { this.text = text }

    Token next() {
        skipWSAndComments()
        if (pos >= text.length()) return token(TokenType.EOF, "")

        char c = text.charAt(pos)

        if (c == '"' as char) return readString()

        if (Character.isLetter(c) || c == '_' as char) return readIdentOrKeyword()
        if (Character.isDigit(c) || (c == '.' as char && pos + 1 < text.length() && Character.isDigit(text.charAt(pos + 1))))
            return readNumber()

        pos++
        switch (c) {
            case '+': return token(TokenType.Plus, "+")
            case '-': return token(TokenType.Minus, "-")
            case '*': if (match('*' as char)) return token(TokenType.Pow, "**"); return token(TokenType.Star, "*")
            case '/': return token(TokenType.Slash, "/")
            case '=': return match('=' as char) ? token(TokenType.EqEq, "==") : token(TokenType.Assign, "=")
            case '!': return match('=' as char) ? token(TokenType.NotEq, "!=") : token(TokenType.Bang, "!")
            case '<': return match('=' as char) ? token(TokenType.Lte, "<=") : token(TokenType.Lt, "<")
            case '>': return match('=' as char) ? token(TokenType.Gte, ">=") : token(TokenType.Gt, ">")
            case '&': if (match('&' as char)) return token(TokenType.AndAnd, "&&"); throw error("expected '&' after '&'")
            case '|': if (match('|' as char)) return token(TokenType.OrOr, "||"); throw error("expected '|' after '|'")
            case '(' : return token(TokenType.LParen, "(")
            case ')' : return token(TokenType.RParen, ")")
            case '{' : return token(TokenType.LBrace, "{")
            case '}' : return token(TokenType.RBrace, "}")
            case ';' : return token(TokenType.Semicolon, ";")
            case ':' : return token(TokenType.Colon, ":")
            case ',' : return token(TokenType.Comma, ",")
            case '%': return token(TokenType.Mod, "%")
            default  : throw error("unexpected char '${c}'")
        }
    }

    private void skipWSAndComments() {
        while (pos < text.length()) {
            char c = text.charAt(pos)
            if (Character.isWhitespace(c)) { pos++; continue }
            if (c == '/' as char && pos + 1 < text.length() && text.charAt(pos + 1) == '/' as char) {
                pos += 2; while (pos < text.length() && text.charAt(pos) != '\n' as char) pos++; continue
            }
            break
        }
    }

    private Token readIdentOrKeyword() {
        int start = pos++
        while (pos < text.length()) {
            char ch = text.charAt(pos)
            if (Character.isLetterOrDigit(ch) || ch == '_' as char) pos++ else break
        }
        String lex = text.substring(start, pos)
        TokenType type = KEYWORDS.get(lex)
        return token(type ?: TokenType.Identifier, lex)
    }

    private Token readNumber() {
        int start = pos
        boolean hasDot = false
        if (text.charAt(pos) == '.' as char) {
            hasDot = true; pos++
            if (pos >= text.length() || !Character.isDigit(text.charAt(pos))) throw error("invalid number")
        }
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++
        if (pos < text.length() && text.charAt(pos) == '.' as char) {
            if (hasDot) throw error("multiple dots")
            hasDot = true; pos++
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' as char || text.charAt(pos) == 'E' as char)) {
            pos++
            if (pos < text.length() && (text.charAt(pos) == '+' as char || text.charAt(pos) == '-' as char)) pos++
            if (pos >= text.length() || !Character.isDigit(text.charAt(pos))) throw error("invalid exponent")
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++
        }
        return token(TokenType.Number, text.substring(start, pos))
    }

    private Token readString() {
        pos++ // "
        StringBuilder sb = new StringBuilder()
        while (pos < text.length()) {
            char ch = text.charAt(pos++)
            if (ch == '"' as char) return token(TokenType.StringLit, sb.toString())
            if (ch == '\\' as char) {
                if (pos >= text.length()) throw error("unterminated escape")
                char esc = text.charAt(pos++)
                switch (esc) {
                    case 'n': sb.append('\n'); break
                    case 't': sb.append('\t'); break
                    case 'r': sb.append('\r'); break
                    case '"': sb.append('"' as char); break
                    case '\\': sb.append('\\' as char); break
                    default: sb.append(esc); break
                }
            } else sb.append(ch)
        }
        throw error("unterminated string literal")
    }

    private boolean match(char expected) {
        if (pos >= text.length()) return false
        if (text.charAt(pos) != expected) return false
        pos++; return true
    }

    private Token token(TokenType t, String lex) { new Token(t, lex, pos) }
    private RuntimeException error(String msg) { new RuntimeException("Lexer error at pos $pos: $msg") }
}
