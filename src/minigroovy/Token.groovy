package minigroovy

enum TokenType {
    // літерали/ідентифікатори
    Identifier, Number, StringLit, True, False,

    // ключові слова
    Let, Const, If, Else, While, For, Print,
    ReadInt, ReadDouble, ReadBool, ReadString,
    Func, Return,
    Switch, Case, Default,
    Break, Continue,

    // оператори
    Plus, Minus, Star, Slash, Pow, Mod,
    Assign,
    EqEq, NotEq, Lt, Lte, Gt, Gte,
    AndAnd, OrOr, Bang,

    // розділювачі
    LParen, RParen, LBrace, RBrace, Semicolon, Colon, Comma,

    EOF
}

class Token {
    final TokenType type
    final String lexeme
    final int pos
    Token(TokenType type, String lexeme, int pos) {
        this.type = type; this.lexeme = lexeme; this.pos = pos
    }
    @Override String toString() { "${type} => ${lexeme}" }
}
