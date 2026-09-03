import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

 
public class Middleware {
    private static final int PUERTO_ESCUCHA = 5000;
    private static final String ARCHIVO_HISTORIAL = "historial_middleware.txt";

    
    private static final List<PrintWriter> clientesConectados = new CopyOnWriteArrayList<>();
    private static final List<PrintWriter> servidoresConectados = new CopyOnWriteArrayList<>();

    private static final AtomicInteger contadorPeticiones = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   MIDDLEWARE   ");
        System.out.println("   Escuchando clientes y servidores en puerto " + PUERTO_ESCUCHA);
        System.out.println("==========================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO_ESCUCHA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ManejadorConexion(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor Middleware: " + e.getMessage());
        }
    }

    private static class ManejadorConexion implements Runnable {
        private final Socket socket;
        private PrintWriter salida;
        private boolean esServidor = false;
        private String idPropio = "?";

        public ManejadorConexion(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                salida = new PrintWriter(socket.getOutputStream(), true);

                String primeraLinea = entrada.readLine();
                if (primeraLinea == null) return;

                String[] primerasPartes = primeraLinea.split(",");

                if (primerasPartes.length == 2 && primerasPartes[0].equals("SERVIDOR")) {
                    
                    esServidor = true;
                    idPropio = primerasPartes[1];
                    servidoresConectados.add(salida);
                    System.out.println("\n[CONEXIÓN] Servidor registrado -> ID: " + idPropio
                            + " | Total servidores: " + servidoresConectados.size());
                    guardarEnHistorial("CONEXIÓN [Servidor " + idPropio + "]: registrado en el middleware.");

                    manejarLineasServidor(entrada);

                } else {
                   
                    esServidor = false;
                    clientesConectados.add(salida);
                    System.out.println("\n[CONEXIÓN] Cliente conectado. Total clientes: " + clientesConectados.size());

                    
                    boolean continuar = procesarLineaCliente(primeraLinea);
                    if (continuar) {
                        manejarLineasCliente(entrada);
                    }
                }

            } catch (IOException e) {
                System.out.println("Conexión finalizada: " + e.getMessage());
            } finally {
                if (esServidor) {
                    servidoresConectados.remove(salida);
                    System.out.println("\n[DESCONEXIÓN] Servidor " + idPropio + " se ha desconectado.");
                    guardarEnHistorial("DESCONEXIÓN [Servidor " + idPropio + "]");
                } else {
                    clientesConectados.remove(salida);
                }
            }
        }

        private void manejarLineasCliente(BufferedReader entrada) throws IOException {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                boolean continuar = procesarLineaCliente(linea);
                if (!continuar) break;
            }
        }

       
        private boolean procesarLineaCliente(String mensajeCliente) {
            String[] partes = mensajeCliente.split(",");
            if (partes.length != 4) return true;

            String idCliente = partes[0];
            String operacion = partes[1];
            String num1 = partes[2];
            String num2 = partes[3];

            if (operacion.equals("DESCONECTAR")) {
                System.out.println("\n[DESCONEXIÓN] Cliente ID: " + idCliente + " ha cerrado sesión de forma segura.");
                guardarEnHistorial("DESCONEXIÓN [" + idCliente + "]: El cliente cerró la aplicación.");
                return false;
            }

            String idPeticion = "P" + contadorPeticiones.incrementAndGet();

            System.out.println("\n[PETICIÓN RECIBIDA]");
            System.out.println(" > ID Petición : " + idPeticion);
            System.out.println(" > Cliente ID  : " + idCliente);
            System.out.println(" > Operación   : " + num1 + " " + operacion + " " + num2);

            guardarEnHistorial("RECIBIDO [" + idCliente + "] (" + idPeticion + "): " + operacion
                    + " (" + num1 + ", " + num2 + ")");

            
            String mensajeServidores = idPeticion + "," + idCliente + "," + operacion + "," + num1 + "," + num2;

            if (servidoresConectados.isEmpty()) {
                System.out.println(" ! No hay servidores conectados.");
                broadcastAClientes("RESULTADO,ERROR_SIN_SERVIDORES," + idPeticion + "," + idCliente + ",,");
                guardarEnHistorial("ERROR (" + idPeticion + "): No hay servidores conectados.");
            } else {
                for (PrintWriter salidaServidor : servidoresConectados) {
                    salidaServidor.println(mensajeServidores);
                }
                System.out.println(" > Petición reenviada a " + servidoresConectados.size() + " servidor(es).");
            }

            return true;
        }

        private void manejarLineasServidor(BufferedReader entrada) throws IOException {
            String linea;
            while ((linea = entrada.readLine()) != null) {
               
                String[] partes = linea.split(",");
                if (partes.length != 5) continue;

                String idPeticion = partes[0];
                String idCliente = partes[1];
                String idServidor = partes[2];
                String operacion = partes[3];
                String resultado = partes[4];

                System.out.println("\n[RESULTADO RECIBIDO de Servidor " + idServidor + "]");
                System.out.println(" > ID Petición : " + idPeticion);
                System.out.println(" > Resultado   : " + resultado);

                guardarEnHistorial("RESPUESTA [Servidor " + idServidor + "] (" + idPeticion + "): " + resultado);

                
                String mensajeClientes = "RESULTADO," + resultado + "," + idPeticion + "," + idCliente
                        + "," + idServidor + "," + operacion;
                broadcastAClientes(mensajeClientes);
            }
        }

        private void broadcastAClientes(String mensaje) {
            for (PrintWriter salidaCliente : clientesConectados) {
                salidaCliente.println(mensaje);
            }
        }
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
}
