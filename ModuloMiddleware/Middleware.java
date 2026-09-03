
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Middleware {
    private static final int PUERTO_CLIENTES = 5000;
    private static final int PUERTO_SERVIDOR_CALC = 6000; 
    private static final String ARCHIVO_HISTORIAL = "historial_middleware.txt";

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   MIDDLEWARE   ");
        System.out.println("   Escuchando clientes en puerto " + PUERTO_CLIENTES);
        System.out.println("==========================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO_CLIENTES)) {
            while (true) {
              
                Socket socketCliente = serverSocket.accept();
                new Thread(new ManejadorCliente(socketCliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor Middleware: " + e.getMessage());
        }
    }

    private static class ManejadorCliente implements Runnable {
        private Socket socketCliente;

        public ManejadorCliente(Socket socket) {
            this.socketCliente = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader entradaCliente = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
                PrintWriter salidaCliente = new PrintWriter(socketCliente.getOutputStream(), true)
            ) {
                String mensajeCliente;
                while ((mensajeCliente = entradaCliente.readLine()) != null) {
              
                    String[] partes = mensajeCliente.split(",");
                    
                    if (partes.length == 4) {
                        String idCliente = partes[0];
                        String operacion = partes[1];
                        String num1 = partes[2];
                        String num2 = partes[3];

           
                        if (operacion.equals("DESCONECTAR")) {
                            System.out.println("\n[DESCONEXIÓN] Cliente ID: " + idCliente + " ha cerrado sesión de forma segura.");
                            guardarEnHistorial("DESCONEXIÓN [" + idCliente + "]: El cliente cerro la aplicación.");
                            break; 
                        }

                        System.out.println("\n[PETICIÓN RECIBIDA]");
                  

                        System.out.println("\n[PETICION RECIBIDA]");
                        System.out.println(" > Cliente ID : " + idCliente);
                        System.out.println(" > Operacion  : " + num1 + " " + operacion + " " + num2);

                        guardarEnHistorial("RECIBIDO [" + idCliente + "]: " + operacion + " (" + num1 + ", " + num2 + ")");

                  
                        String resultado = procesarConServidorCalculo(operacion + "," + num1 + "," + num2);

                        if (resultado != null) {
                            System.out.println(" > Respuesta devuelta a " + idCliente + ": " + resultado);
                            salidaCliente.println("RESULTADO," + resultado);
                            guardarEnHistorial("RESPUESTA [" + idCliente + "]: " + resultado);
                        } else {
                            salidaCliente.println("RESULTADO,ERROR_SERVIDOR");
                            guardarEnHistorial("ERROR [" + idCliente + "]: Servidor no disponible");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Un cliente se ha desconectado.");
            }
        }


        private String procesarConServidorCalculo(String peticion) {
            try (
                Socket socketServidor = new Socket("localhost", PUERTO_SERVIDOR_CALC);
                PrintWriter salidaServidor = new PrintWriter(socketServidor.getOutputStream(), true);
                BufferedReader entradaServidor = new BufferedReader(new InputStreamReader(socketServidor.getInputStream()))
            ) {
                salidaServidor.println(peticion);
                return entradaServidor.readLine();
            } catch (IOException e) {
                System.err.println(" ERROR: No se pudo conectar con el servidor (Puerto " + PUERTO_SERVIDOR_CALC + ")");
                return null;
            }
        }
    }

    private static synchronized void guardarEnHistorial(String evento) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (FileWriter fw = new FileWriter(ARCHIVO_HISTORIAL, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + fechaHora + "] " + evento);
        } catch (IOException e) {
            System.err.println("error al escribir el historial: " + e.getMessage());
        }
    }
}