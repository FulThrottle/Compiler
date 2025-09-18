import minigroovy.Interpreter
import minigroovy.Lexer
import minigroovy.Parser

class Main {
    static void main(String[] args) {
        def code = '''
            let sum: int = 0;
            for (let i: int = 0; i < 5; i = i + 1) {
                if (i == 2) { continue; } // пост-частина все одно виконається
                sum = sum + i;
                if (i == 3) { break; }    // пост НЕ виконується після break
            }
            print(sum); // очікувано: 1 + 3 = 4
        '''
        def program = new Parser(new Lexer(code)).parseProgram()
        new Interpreter().run(program)
    }
}
