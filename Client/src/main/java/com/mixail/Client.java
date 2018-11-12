package com.mixail;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;


import static com.mixail.model.Constans.*;


public class Client {

    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(Connection.class);
    public static void main(String[] args) {

        new Client().runClient();
    }

    private void runClient() {

        try {
            Socket socket = new Socket(IP, PORT);
            connection = new Connection(socket);
            ConsoleHelper.writeMessage("Please register as an agent or client by /register agent name or by /register client name ");
            Thread clientThread = new Thread(new ClientThread());
            clientThread.setDaemon(true);
            clientThread.start();
            String message = "";
            while (!message.equals(EXIT)) {
                message = ConsoleHelper.readString();
                sendTextMessage(message);
            }
        } catch (IOException e) {
           LOGGER.debug("Failed ", e);
        }
    }



    private void sendTextMessage(String text) {
        Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
    }

    private class ClientThread implements Runnable {

        @Override
        public void run() {
            while (!connection.getSocket().isClosed()) {
                String in ;
                try {
                    in = connection.receive().getData();
                    if (in.startsWith("client is connected"))
                    {
                        connection.send(new Message(in));
                    }
                    processIncomingMessage(in);
                } catch (IOException  | ClassNotFoundException e) {
                    LOGGER.debug("Failed ", e);
                }
            }
        }
    }

    private void processIncomingMessage(String message) {
        ConsoleHelper.writeMessage(message);
    }
}
