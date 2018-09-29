package com.mixail;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {

    private static final Logger LOGGER = Logger.getLogger(ConsoleHelper.class);

    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {

        while (true) {
            try {
                return reader.readLine();
            } catch (IOException e) {
                writeMessage("Error while reading message from console.");
                LOGGER.debug("Error while reading message from console.");
            }
        }
    }
}
