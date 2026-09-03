package ModuloCliente;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class Cliente {
    private static PrintWriter out;
    private static JTextField display;
    
    private static final String ID_CLIENTE = UUID.randomUUID().toString().substring(0, 8);
    private static final String archivoHistorial = "historial_cliente_" + ID_CLIENTE + ".txt";

    private static ArrayList<JButton> listaBotones = new ArrayList<>();
    
    private static String num1 = "";
    private static String num2 = "";
    private static String operador = "";
    private static boolean escribiendoNum2 = false;
    private static boolean conectado = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            crearInterfazGrafica();
            conectarAlMiddleware();
        });
    }

    private static void crearInterfazGrafica() {
        JFrame frame = new JFrame("Cliente: " + ID_CLIENTE);
        
       
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectarYSalir();
            }
        });

        frame.setSize(320, 420);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBackground(new Color(44, 62, 80));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));

        display = new JTextField();
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Consolas", Font.BOLD, 28));
        display.setBackground(new Color(236, 240, 241));
        display.setForeground(new Color(44, 62, 80));
        display.setPreferredSize(new Dimension(300, 60));
        panelPrincipal.add(display, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 4, 8, 8));
        grid.setBackground(new Color(44, 62, 80));

        String[] botones = {
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "C", "0", "=", "+"
        };

        for (String texto : botones) {
            JButton btn = new JButton(texto);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder());
            
            listaBotones.add(btn); 

            if (texto.matches("[0-9]")) {
                btn.setBackground(new Color(52, 73, 94));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> agregarNumero(texto));
            } else if (texto.equals("C")) {
                btn.setBackground(new Color(231, 76, 60));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> limpiarCalculadora());
            } else if (texto.equals("=")) {
                btn.setBackground(new Color(46, 204, 113));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> enviarOperacion());
            } else {
                btn.setBackground(new Color(243, 156, 18));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> asignarOperador(texto));
            }
            grid.add(btn);
        }

        panelPrincipal.add(grid, BorderLayout.CENTER);
        frame.add(panelPrincipal);
        frame.setVisible(true);
    }

    private static void agregarNumero(String numero) {
        if (escribiendoNum2) {
            num2 += numero;
            display.setText(num1 + " " + obtenerSimbolo(operador) + " " + num2);
        } else {
            num1 += numero;
            display.setText(num1);
        }
    }

    private static void asignarOperador(String simbolo) {
        if (!num1.isEmpty() && num2.isEmpty()) {
            switch (simbolo) {
                case "+": operador = "SUMA"; break;
                case "-": operador = "RESTA"; break;
                case "*": operador = "MULTIPLICACION"; break;
                case "/": operador = "DIVISION"; break;
            }
            escribiendoNum2 = true;
            display.setText(num1 + " " + simbolo + " ");
        }
    }

    private static void limpiarCalculadora() {
        num1 = ""; num2 = ""; operador = "";
        escribiendoNum2 = false;
        display.setText("");
    }

    private static String obtenerSimbolo(String op) {
        switch (op) {
            case "SUMA": return "+";
            case "RESTA": return "-";
            case "MULTIPLICACION": return "*";
            case "DIVISION": return "/";
            default: return "";
        }
    }

    private static void conectarAlMiddleware() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                conectado = true;

                String respuesta;
                while ((respuesta = in.readLine()) != null) {
                    String[] partes = respuesta.split(",");
                    if (partes.length >= 2 && partes[0].equals("RESULTADO")) {
                        String valorFinal = partes[1];
                        SwingUtilities.invokeLater(() -> {
                            display.setText(valorFinal);
                            num1 = valorFinal; 
                            num2 = ""; operador = ""; escribiendoNum2 = false;
                            cambiarEstadoBotones(true); 
                        });
                        guardarHistorial("Operación respondida: " + respuesta);
                    }
                }
            } catch (ConnectException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, 
                        "No se pudo conectar al Middlewar, Asegúrate de que el servidor esté encendido.", 
                        "Error de Red", JOptionPane.ERROR_MESSAGE);
                });
            } catch (IOException e) {
                
            }
        }).start();
    }

    private static void enviarOperacion() {
        if (num1.isEmpty() || num2.isEmpty() || operador.isEmpty()) return;
        
        if (!conectado) {
            JOptionPane.showMessageDialog(null, "No estas conectado al servidor.");
            return;
        }

        if (operador.equals("DIVISION") && num2.equals("0")) {
            JOptionPane.showMessageDialog(null, "Error: No se puede dividir entre cero.", "Error", JOptionPane.WARNING_MESSAGE);
            limpiarCalculadora();
            return;
        }

        String mensaje = ID_CLIENTE + "," + operador + "," + num1 + "," + num2;
        out.println(mensaje);
        guardarHistorial("Operacion solicitada: " + mensaje);
        
        display.setText("Calculando...");
        cambiarEstadoBotones(false); 
    }

    // NUEVA FUNCIÓN DE DESCONEXIÓN
    private static void desconectarYSalir() {
        if (conectado && out != null) {
            String mensajeDespedida = ID_CLIENTE + ",DESCONECTAR,0,0";
            out.println(mensajeDespedida);
            guardarHistorial("Desconectando cliente del servidor...");
        }
        System.exit(0);
    }

    private static void cambiarEstadoBotones(boolean estado) {
        for (JButton btn : listaBotones) {
            btn.setEnabled(estado);
        }
    }

    private static void guardarHistorial(String registro) {
        try (FileWriter fw = new FileWriter(archivoHistorial, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(registro);
        } catch (IOException e) {}
    }
}
