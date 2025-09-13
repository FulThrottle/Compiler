package minigroovy.ui

import minigroovy.*
import javax.swing.*
import java.awt.*
import java.io.OutputStream
import java.io.PrintStream

class GuiRunner {

    static void main(String[] args) {
        SwingUtilities.invokeLater {
            def frame = new JFrame("MiniLang IDE")
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            frame.setSize(900, 600)

            def codeArea = new JTextArea()
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14))
            codeArea.setText(defaultSample())

            def outputArea = new JTextArea()
            outputArea.setEditable(false)
            outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14))

            def runBtn = new JButton("Run")


            runBtn.addActionListener {
                outputArea.setText("")                 // очистити лог
                String code = codeArea.getText()

                // перенаправляємо out/err у праву панель
                PrintStream oldOut = System.out
                PrintStream oldErr = System.err
                PrintStream uiOut  = new PrintStream(new TextAreaOutputStream(outputArea), true, "UTF-8")
                System.setOut(uiOut)
                System.setErr(uiOut)

                // виконуємо програму у воркер-потоці, а out/err відновимо після завершення
                // ... усередині runBtn.addActionListener { ... }
                Thread worker = new Thread({
                    try {
                        def program = new Parser(new Lexer(code)).parseProgram()
                        new Interpreter().run(program)
                    } catch (Throwable ex) {
                        // покажемо повний стек у правій панелі
                        def sw = new StringWriter()
                        ex.printStackTrace(new PrintWriter(sw))
                        SwingUtilities.invokeLater {
                            outputArea.append(sw.toString() + "\n")
                        }
                    } finally {
                        System.setOut(oldOut)
                        System.setErr(oldErr)
                        uiOut.flush()
                    }
                }, "MiniLang-Runner")

                worker.setDaemon(true)
                worker.start()
            }

            def clearBtn = new JButton("Clear")
            clearBtn.addActionListener { outputArea.setText("") }

            def topBar = new JPanel(new FlowLayout(FlowLayout.LEFT))
            topBar.add(runBtn)
            topBar.add(clearBtn)
            

            def split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(codeArea), new JScrollPane(outputArea))
            split.setDividerLocation(520)

            frame.getContentPane().add(topBar, BorderLayout.NORTH)
            frame.getContentPane().add(split, BorderLayout.CENTER)
            frame.setLocationRelativeTo(null)
            frame.setVisible(true)
        }
    }

    // простий OutputStream, що пише рядки у JTextArea
    static class TextAreaOutputStream extends OutputStream {
        private final JTextArea area
        private final StringBuilder buf = new StringBuilder()
        TextAreaOutputStream(JTextArea area) { this.area = area }

        // GuiRunner.groovy → клас TextAreaOutputStream
        @Override
        void write(int b) {
            char ch = (char) b
            if (ch == '\r') return
            if (ch == '\n') {
                final String line = buf.toString()
                buf.setLength(0)
                SwingUtilities.invokeLater {
                    area.append(line + "\n")
                    area.setCaretPosition(area.getDocument().getLength()) // автопрокрутка
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
