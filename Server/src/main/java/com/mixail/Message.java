package com.mixail;

import java.io.Serializable;

public class Message implements Serializable {

    private final MessageType type;
    private final String data;

    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }

    //по умалчанию тип  текстовый
    public Message(String data) {
        this.data = data;
        this.type = MessageType.TEXT;
    }

    public String getData() {
        return data;
    }
}

