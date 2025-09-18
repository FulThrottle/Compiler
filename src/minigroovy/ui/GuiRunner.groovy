package minigroovy.ui

import minigroovy.Interpreter
import minigroovy.Lexer
import minigroovy.Parser

import javax.swing.*
import java.awt.*
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

class GuiRunner {

    // ----- Демонстраційні шаблони -----
    static final LinkedHashMap<String, String> TEMPLATES = [
            "— Templates —": "",
            "Арифметика / пріоритети": '''
                let a: int = 2;
                let b: double = 3.5;
                print(1 + 2 * 3);
                print((b / 2) + a);
            '''.stripIndent(),
            "Цикл while": '''
                let i: int = 0;
                while (i < 5) {
                    print(i);
                    i = i + 1;
                }
            '''.stripIndent(),
            "Цикл for": '''
                let sum: int = 0;
                for (let i: int = 0; i < 10; i = i + 1) {
                    if (i % 2 == 0) { continue; }
                    sum = sum + i;
                    if (i >= 7) { break; }
                }
                print(sum); // очікувано 16
            '''.stripIndent(),
            "if / else": '''
                let x: int = 3;
                if (x < 0) { print("neg"); }
                else if (x == 0) { print("zero"); }
                else { print("pos"); }
            '''.stripIndent(),
            "switch": '''
                let s: int = -1;
                switch (s) {
                    case -1: print("neg"); break;
                    case 0:  print("zero"); break;
                    default: print("other");
                }
            '''.stripIndent(),
            "Функції": '''
                func pow2(n: int): int {
                    return n ** 2;
                }
                print(pow2(5)); // 25
            '''.stripIndent(),
            "Ввід/вивід": '''
                let name: string;
                print("Enter your name:");
                read(name);
                print("Hello, " + name);
            '''.stripIndent(),
            "Комплексний приклад": '''
                const PI: double = 3.14159;
                func area(r: double): double { return PI * r * r; }

                let sum: int = 0;
                for (let i: int = 1; i < 10; i = i + 2) {
                    sum = sum + i;
                }
                print(sum);                 // 25

                let r: double = 2.0;
                print(area(r));             // 12.56

                let k: int = -1;
                switch (k) {
                    case -1: print("neg"); break;
                    case 0:  print("zero"); break;
                    default: print("other");
                }
            '''.stripIndent()
    ]

    static void main(String[] args) {
        SwingUtilities.invokeLater {
            def frame = new JFrame("MiniLang IDE")
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            frame.setSize(1000, 640)

            // Ліва панель — редактор
            def codeArea = new JTextArea()
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14))
            codeArea.setText(defaultSample())

            // Права панель — вивід
            def outputArea = new JTextArea()
            outputArea.setEditable(false)
            outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14))

            // Кнопки
            def runBtn   = new JButton("Run")
            def clearBtn = new JButton("Clear")

            // Комбобокс шаблонів
            def templateBox = new JComboBox(TEMPLATES.keySet().toArray(new String[0]))
            templateBox.setMaximumSize(new Dimension(260, 28))

            // Топбар
            def topBar = new JPanel(new FlowLayout(FlowLayout.LEFT))
            topBar.add(runBtn)
            topBar.add(clearBtn)
            topBar.add(new JLabel("  Templates:"))
            topBar.add(templateBox)

            // Спліт — ліворуч код, праворуч вивід
            def split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(codeArea), new JScrollPane(outputArea))
            split.setDividerLocation(560)

            frame.getContentPane().add(topBar, BorderLayout.NORTH)
            frame.getContentPane().add(split, BorderLayout.CENTER)
            frame.setLocationRelativeTo(null)
            frame.setVisible(true)

            // Обробники
            clearBtn.addActionListener { outputArea.setText("") }

            templateBox.addActionListener {
                String key = (String) templateBox.getSelectedItem()
                if (key == null || key == "— Templates —") return
                String tmpl = TEMPLATES.get(key) ?: ""
                if (codeArea.getText().trim().length() > 0) {
                    int ans = JOptionPane.showConfirmDialog(frame,
                            "Замінити поточний текст шаблоном «${key}»?",
                            "Підставити шаблон",
                            JOptionPane.OK_CANCEL_OPTION)
                    if (ans != JOptionPane.OK_OPTION) return
                }
                codeArea.setText(tmpl)
                codeArea.setCaretPosition(codeArea.getDocument().getLength())
                outputArea.setText("")
            }

            runBtn.addActionListener {
                outputArea.setText("")
                String code = codeArea.getText()
                if (code.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Немає коду для запуску.", "Увага", JOptionPane.WARNING_MESSAGE)
                    return
                }

                // перенаправляємо out/err у праву панель
                PrintStream oldOut = System.out
                PrintStream oldErr = System.err
                PrintStream uiOut  = new PrintStream(new TextAreaOutputStream(outputArea), true, "UTF-8")
                System.setOut(uiOut)
                System.setErr(uiOut)

                runBtn.setEnabled(false) // блок на час виконання

                Thread worker = new Thread({
                    try {
                        def program = new Parser(new Lexer(code)).parseProgram()
                        new Interpreter().run(program) // як у твоєму робочому прикладі
                    } catch (Throwable ex) {
                        def sw = new StringWriter()
                        ex.printStackTrace(new PrintWriter(sw))
                        SwingUtilities.invokeLater {
                            outputArea.append(sw.toString() + "\n")
                        }
                    } finally {
                        System.setOut(oldOut)
                        System.setErr(oldErr)
                        uiOut.flush()
                        SwingUtilities.invokeLater { runBtn.setEnabled(true) }
                    }
                }, "MiniLang-Runner")

                worker.setDaemon(true)
                worker.start()
            }
        }
    }

    // Вивід у JTextArea (праворуч) із автопрокруткою
    static class TextAreaOutputStream extends OutputStream {
        private final JTextArea area
        private final StringBuilder buf = new StringBuilder()

        TextAreaOutputStream(JTextArea area) { this.area = area }

        @Override
        void write(int b) {
            char ch = (char) b
            if (ch == '\r') return
            if (ch == '\n') {
                final String line = buf.toString()
                buf.setLength(0)
                SwingUtilities.invokeLater {
                    area.append(line + "\n")
                    area.setCaretPosition(area.getDocument().getLength())
                }
            } else {
                buf.append(ch)
            }
        }

        @Override
        void flush() {
            if (buf.length() > 0) {
                final String line = buf.toString()
                buf.setLength(0)
                SwingUtilities.invokeLater {
                    area.append(line)
                    area.setCaretPosition(area.getDocument().getLength())
                }
            }
        }
    }

    private static String defaultSample() {
        return '''// Sample program for GUI
let sum: int = 0;
for (let i: int = 0; i < 10; i = i + 1) {
    if (i % 2 == 0) { continue; }
    sum = sum + i;
    if (i >= 7) { break; }
}
print(sum); // 1 + 3 + 5 + 7 = 16
'''
    }
}
