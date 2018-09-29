package com.mixail;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private static final Logger LOGGER = Logger.getLogger(Connection.class);
    public Socket getSocket() {
        return socket;
    }

    public Connection(Socket socket) {
        this.socket = socket;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.debug("Error while create connection");
        }

    }

    public void send(Message message) {
        synchronized (out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                ConsoleHelper.writeMessage("Error while sending message");
                LOGGER.debug("Error while sending message");
            }

        }
    }

    public Message receive() throws IOException, ClassNotFoundException {
        synchronized (in) {
            return (Message) in.readObject();
        }
    }

    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.debug("Failed userSocket.close", e);
        }
    }
}
