package Duarte;

/*****************************************************************************************
 * Engenharia de Serviços de Rede - 22/23
 * TP2 - Serviço Over the Top para entrega de multimédia
 * Módulo: Cliente
 * Módulo: Servidor
 * Grupo: PL83
 * Programadores: 
 *  a83630 - Duarte Serrão
 *  pg50794 - Vasco Oliveira
 *  pg50340 - Diogo Matos
 *****************************************************************************************/

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/*****************************************************************************************
 * NAME: Server
 * PURPOSE:
 *****************************************************************************************/

public class Server {

    ServerSocket server;

    public static void main(String[] args) {
        try {
            new Server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Server() throws IOException {
        server = new ServerSocket(8888);

        // Loop that is constantly waiting for connection
        while (true) {
            System.out.println("Waiting for connection on port 8888...");

            // Client shows up
            Socket client = server.accept();
            System.out.println("Received connection from " + client.getInetAddress() + " on port " + client.getPort());

            ClientHandler handler = new ClientHandler(this);
            handler.start();
        }
    }
}

class ClientHandler extends Thread {

    Server server;

    // Client socket here
    Socket client;

    public ClientHandler(Server server) {
        this.server = server;
    }

    public void run() {
        try {
            // First we create a reading buffer that will try constantly to read from the
            // client's socket
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // Second, we create a writer, for when the server needs to comunicate back to
            // the
            // client
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

            // Main program here (maybe a loop)

            // Closing sockets and reading buffers here

        } catch (Exception e) {
        }
    }
}