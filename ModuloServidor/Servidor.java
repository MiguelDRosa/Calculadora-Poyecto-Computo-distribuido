package ModuloServidor;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Servidor {
    private static final String HOST_MIDDLEWARE = "localhost";
    private static final int PUERTO_MIDDLEWARE = 5000;
    private static final String ID_SERVIDOR = "SRV-" + UUID.randomUUID().toString().substring(0, 6);
    private static final String ARCHIVO_HISTORIAL = "historial_servidor_" + ID_SERVIDOR + ".txt";

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   SERVIDOR DE CALCULO - ID: " + ID_SERVIDOR);
        System.out.println("   Conectando al middleware en " + HOST_MIDDLEWARE + ":" + PUERTO_MIDDLEWARE);
        System.out.println("==========================================");

        while (true) {
            try (
                Socket socket = new Socket(HOST_MIDDLEWARE, PUERTO_MIDDLEWARE);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                
                out.println("SERVIDOR," + ID_SERVIDOR);
                System.out.println("Conectado y registrado en el middleware.");
                guardarEnHistorial("CONEXIÓN: Servidor " + ID_SERVIDOR + " registrado en el middleware.");

                String peticion;
                while ((peticion = in.readLine()) != null) {
                    procesarPeticion(peticion, out);
                }
            } catch (IOException e) {
                System.err.println("No se pudo conectar/mantener conexión con el middleware: " + e.getMessage());
                System.err.println("Reintentando en 3 segundos...");
                dormir(3000);
            }
        }
    }

    private static void procesarPeticion(String peticion, PrintWriter out) {
      
        String[] partes = peticion.split(",");
        if (partes.length != 5) return;

        String idPeticion = partes[0];
        String idCliente = partes[1];
        String operacion = partes[2];

        double resultado = 0;
        boolean error = false;

        try {
            double n1 = Double.parseDouble(partes[3]);
            double n2 = Double.parseDouble(partes[4]);

            switch (operacion) {
                case "SUMA": resultado = n1 + n2; break;
                case "RESTA": resultado = n1 - n2; break;
                case "MULTIPLICACION": resultado = n1 * n2; break;
                case "DIVISION":
                    if (n2 != 0) {
                        resultado = n1 / n2;
                    } else {
                        error = true;
                    }
                    break;
                default:
                    error = true;
            }
        } catch (NumberFormatException e) {
            error = true;
        }

        String resultadoTexto = error ? "ERROR"
                : ((resultado % 1 == 0) ? String.valueOf((long) resultado) : String.valueOf(resultado));

        System.out.println("\n[PETICIÓN PROCESADA]");
        System.out.println(" > ID Petición : " + idPeticion);
        System.out.println(" > Cliente     : " + idCliente);
        System.out.println(" > Operación   : " + operacion);
        System.out.println(" > Resultado   : " + resultadoTexto);

        guardarEnHistorial("PETICIÓN (" + idPeticion + ") de [" + idCliente + "]: " + operacion + " -> " + resultadoTexto);

      
        String respuesta = idPeticion + "," + idCliente + "," + ID_SERVIDOR + "," + operacion + "," + resultadoTexto;
        out.println(respuesta);
    }

    private static synchronized void guardarEnHistorial(String evento) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (FileWriter fw = new FileWriter(ARCHIVO_HISTORIAL, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + fechaHora + "] " + evento);
        } catch (IOException e) {
            System.err.println("Error al escribir el historial: " + e.getMessage());
        }
    }

    private static void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}
