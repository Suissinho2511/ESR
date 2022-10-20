package Duarte;

/*****************************************************************************************
 * Sistemas Distribuídos - 22/23
 * NOME DO TRABALHO AQUI
 * Módulo: Cliente
 * Grupo:
 * Programadores: 
 * a83630 - Duarte Serrão
 *  pg50794 - Vasco Oliveira
 * pg50340 - Diogo Matos
 *****************************************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

/*****************************************************************************************
 * NAME: Client
 * PURPOSE: Comunication User-Computer and Client-Server
 *****************************************************************************************/
public class Client {
    Socket client = null;

    public static void main(String[] args) {
        try {
            new Client();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Client() throws IOException {
        // Print instructions here
        System.out.println("Instruções para cliente aqui");

        // Each client will have a different adress, but all will link to the same
        // port, wich is the one that links to the server
        client = new Socket(InetAddress.getLocalHost(), 8888);

        // We need 2 threads. One that is ready to send new commands, and one that is
        // always listening
        Thread client2Server = new Thread(new ThreadClientToServer(client));
        Thread server2Client = new Thread(new ThreadServerToClient(client));

        client2Server.start();
        server2Client.start();

    }
}

/*****************************************************************************************
 * NAME: ThreadClientToServer
 * PURPOSE: Recieves instructions from the user and then sends it to the server
 *****************************************************************************************/
class ThreadClientToServer implements Runnable {

    Socket sender = null;
    PrintWriter writer = null;

    public ThreadClientToServer(Socket client) {
        this.sender = client;

    }

    public void run() {
        boolean running = true;
        String userInstruction = "";
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        // Trying to create the writer
        try {
            writer = new PrintWriter(sender.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (running) {
            // Waits here until the user writes something on the keyboard
            try {
                userInstruction = input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // As soon as the user writes the instruction, it sends automatically
            // to the server
            writer.println(userInstruction);
            writer.flush();

            // Stop mechanism for client
            if (userInstruction.equals("exit")) {
                running = false;
            }

        }
        try {
            sender.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

/*****************************************************************************************
 * NAME: ThreadServerToClient
 * PURPOSE: Recieves information from the server and displays it on screen for
 * the user
 * to see.
 *****************************************************************************************/
class ThreadServerToClient implements Runnable {

    Socket listener;
    BufferedReader buffer;

    public ThreadServerToClient(Socket client) {
        this.listener = client;
    }

    public void run() {
        String aux = "";
        //
        try {
            buffer = new BufferedReader(new InputStreamReader(listener.getInputStream()));
            // If the listener found something, then we print it
            while ((aux = buffer.readLine()) != null) {
                System.out.println(aux);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
