package main.java.main;



import java.io.*;
import java.net.Socket;

public class ChatClient {
    private Socket socket;
    private BufferedReader entrada;
    private BufferedWriter salida;

    public ChatClient(String serverAddress, int serverPort) {
        try {
            
            socket = new Socket(serverAddress, serverPort);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarMensaje(String mensaje) {
        try {
            salida.write(mensaje + "\n");
            salida.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String recibirMensaje() {
        try {
            return entrada.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean nuevoMensaje() {
        try {
            return entrada.ready();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String loginUsuario(String username, String password) {
        
        try {
            // Enviar operación de autenticación al servidor
            String mensaje = "LOGIN" + ":" + username + ":" + password;
            
            salida.write(mensaje);
            salida.newLine();
            salida.flush();

            // Recibir la respuesta del servidor
            String respuesta = entrada.readLine();
            System.out.println("Respuesta del servidor (Login): " + respuesta);
            
            return respuesta;

        } catch (IOException e) {
            e.printStackTrace();
            // Manejar errores de comunicación
            return "Error de comunicación";
        }
        
    }

    public String registerUsuario(String username, String password) {
        try {
            // Enviar operación de registro al servidor
            String mensaje = "REGISTER" + ":" + username + ":" + password;
            
            salida.write(mensaje);
            salida.newLine();
            salida.flush();
            
            // Recibir la respuesta del servidor
            String respuesta = entrada.readLine();
            System.out.println("Respuesta del servidor (Register): " + respuesta);
            return respuesta;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Error de comunicación";
        }
    }
    
    public void cerrarConexion() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
