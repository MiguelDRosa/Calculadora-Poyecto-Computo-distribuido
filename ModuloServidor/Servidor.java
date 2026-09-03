package ModuloServidor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    private static final int PUERTO_ESCUCHA = 6000;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   SERVIDOR        ");
        System.out.println("   Escuchando al Middleware en puerto " + PUERTO_ESCUCHA);
        System.out.println("==========================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO_ESCUCHA)) {
            while (true) {
                Socket socketMiddleware = serverSocket.accept();
                new Thread(() -> procesarPeticion(socketMiddleware)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el Servidor de calculo: " + e.getMessage());
        }
    }

    private static void procesarPeticion(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String peticion = in.readLine(); 
            if (peticion != null) {
                String[] partes = peticion.split(",");
                if (partes.length == 3) {
                    String operacion = partes[0];
                    double n1 = Double.parseDouble(partes[1]);
                    double n2 = Double.parseDouble(partes[2]);
                    
                    double resultado = 0;
                    boolean error = false;

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

                    if (!error) {
                        
                        String resFormateado = (resultado % 1 == 0) ? String.valueOf((long) resultado) : String.valueOf(resultado);
                        out.println(resFormateado);
                    } else {
                        out.println("ERROR");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando calculo: " + e.getMessage());
        }
    }
}