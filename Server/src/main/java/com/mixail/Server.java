package com.mixail;

import com.mixail.model.TypeOfUser;
import com.mixail.model.User;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

import static com.mixail.Сonstants.*;

public class Server {

    private static final ExecutorService serverThreadsExecutor = Executors.newFixedThreadPool(POOL_SIZE);
    private static final BlockingQueue<User> agentsQueue = new ArrayBlockingQueue<>(ARRAY_SIZE);
    private static Map<String, User> clientsMap = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(Server.class);

    public static void main(String[] args) {
        try (
                ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Chat server started");
            ConsoleHelper.writeMessage("server started");
            while (!serverSocket.isClosed()) {
                serverThreadsExecutor.execute(new ServerHandler(serverSocket.accept()));
            }
            serverThreadsExecutor.shutdown();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
        LOGGER.info("Chat server shut down");
    }

    private static class ServerHandler implements Runnable {

        private Connection userConnection;
        private Connection innerUserConnection;
        private Socket socket;
        private volatile boolean isRegistered = false;
        private User user;

        private ServerHandler(Socket socket) {
            this.socket = socket;
        }

        private static String[] spitMessage(String userMessage) {
            return userMessage.split(" ");
        }

        @Override
        public void run() {

            userConnection = null;
            String userMessage;
            try {
                userConnection = new Connection(socket);
                user = new User();
                while (!isRegistered) {
                    userMessage = userConnection.receive().getData();
                    String[] parts = spitMessage(userMessage);
                    String register = parts[PART_0];
                    String type = parts[PART_1];
                    String name = parts[PART_2];

                    if (register.equalsIgnoreCase(REGISTER) && parts.length == 3) {
                        {
                            if (type.equalsIgnoreCase(AGENT_TYPE)) {
                                registerAgent(name);
                            }
                            if (type.equalsIgnoreCase(CLIENT_TYPE)) {
                                if (clientsMap.containsKey(name)) {
                                    userConnection.send(new Message("this name has been already in use please register with another one"));
                                    continue;
                                }
                                registerClient(name);
                            }
                        }
                    } else {
                        userConnection.send(new Message("you entered incorrect data, please try again"));
                    }
                }

                User userForChat = startChat(user);

                if (innerUserConnection != null) {
                    LOGGER.info("Dialogue between agent " + userForChat.getName() + " and client " + user.getName() + " was over");
                    if (user.getUserType() == TypeOfUser.AGENT) {
                        if (!user.isUserLeave())
                            innerUserConnection.send(new Message("Agent left the chat and please exit the chat by entering /exit or try to send a message to another agent"));
                        userForChat.setUserLeave(true);
                    } else if (!user.isUserLeave()) {
                        innerUserConnection.send(new Message("Client exited from the chat please close the app by using /exit or wait when other client will connect"));
                        agentsQueue.add(userForChat);

                    }
                    userForChat.setUserLeave(true);
                    if (user.getUserType() == TypeOfUser.AGENT) {
                        // agentsQueue.remove(user);
                    }
                } else {
                    if (userForChat != null) {
                        agentsQueue.add(user);
                    }
                    clientsMap.remove(user);
                }
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.debug("Failed", e);
            } finally {
                if (userConnection != null)
                    userConnection.close();
            }

        }

        private User startChat(User user) throws IOException, ClassNotFoundException {

            User userForChat = null;

            while (userConnection != null) {
                String message = user.getConnection().receive().getData();
                if (message.equals(EXIT)) {
                    return userForChat;
                }
                while (userForChat == null) {
                    userForChat = searchSomeoneToChat(user, message);
                    if (message.equals(EXIT)) {

                        return userForChat;
                    }
                    if (userForChat == null && (user.getUserType() == TypeOfUser.AGENT)) {
                        message = user.getConnection().receive().getData();

                    } else break;
                }
                while (userConnection != null) {
                    message = user.getConnection().receive().getData();
                    if (message.equals(EXIT)) {
                        return userForChat;
                    }
                    if (user.isUserLeave()) {
                        while (true) {
                            userForChat = searchSomeoneToChat(user, message);
                            if (message.equals(EXIT)) {
                                return userForChat;
                            }
                            if (userForChat == null && (user.getUserType() == TypeOfUser.AGENT)) {
                                message = user.getConnection().receive().getData();
                                if (message.equals(EXIT)) {

                                    return userForChat;
                                }
                            } else break;
                        }
                        user.setUserLeave(false);
                        message = user.getConnection().receive().getData();
                        if (message.equals(EXIT)) {

                            return userForChat;
                        }
                    }

                    if (message.startsWith(LEAVE) && user.getUserType() == TypeOfUser.AGENT) {
                        user.getConnection().send(new Message("Agent can`t leave the app"));
                        message = user.getConnection().receive().getData();
                        if (message.equals(EXIT)) {
                            return userForChat;
                        }
                    }

                    if (message.startsWith(LEAVE) && user.getUserType() == TypeOfUser.CLIENT) {
                        user.getConnection().send(new Message("dialog is ended"));
                        innerUserConnection.send(new Message("dialog is ended, client " + user.getName() + " left chat"));
                        LOGGER.info("dialog is ended, client " + user.getName() + " left chat");
                        innerUserConnection = null;
                        userForChat.setUserLeave(true);
                        agentsQueue.add(userForChat);
                        userForChat = null;
                        break;
                    }
                    innerUserConnection.send(new Message(user.getName() + ": " + message));
                }
            }
            return userForChat;
        }


        private User searchSomeoneToChat(User user, String line) {
            User userForChat = null;
            if (user.getUserType() == TypeOfUser.CLIENT) {
                try {
                    if (agentsQueue.size() == 0) {
                        user.getConnection().send(new Message("All agent are busy now, all your messages will be redirected once any agent is available"));
                    }
                    synchronized (this) {
                        userForChat = agentsQueue.take();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                innerUserConnection = userForChat.getConnection();
                innerUserConnection.send(new Message("client is connected " + user.getName()));
                innerUserConnection.send(new Message(user.getName() + ": " + line));
                user.getConnection().send(new Message("Agent " + userForChat.getName() + ": connected to you"));
                return userForChat;
            }

            if (user.getUserType() == TypeOfUser.AGENT) {

                if (line.startsWith("client is connected")) {
                    String[] parts = spitMessage(line);
                    String clientName = parts[3];
                    userForChat = clientsMap.get(clientName);
                    innerUserConnection = userForChat.getConnection();
                    return userForChat;
                } else {
                    user.getConnection().send(new Message("Please wait for the connected client prior to start the dialog. There are no connected clients now."));
                    return null;
                }
            }
            return null;
        }


        private void registerClient(String clientName) {
            user.setUserType(TypeOfUser.CLIENT);
            user.setConnection(userConnection);
            user.setName(clientName);
            clientsMap.put(clientName, user);
            isRegistered = true;
            userConnection.send(new Message(MessageType.TEXT, "Client " + clientName + " is registered"));
            LOGGER.info("Client " + clientName + "is registered");

        }

        private void registerAgent(String agentName) {
            user.setUserType(TypeOfUser.AGENT);
            user.setConnection(userConnection);
            user.setName(agentName);
            isRegistered = true;
            agentsQueue.add(user);
            userConnection.send(new Message(MessageType.TEXT, "Agent " + agentName + " is registered"));
            LOGGER.info("Agent " + agentName + " is registered");
        }
    }
}

