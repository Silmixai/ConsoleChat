package com.mixail.model;

import com.mixail.Connection;

public class User {

    private String name;
    private Connection connection;
    TypeOfUser userType;
    boolean isUserLeave = false;
    
    public boolean isUserLeave() {
        return isUserLeave;
    }

    public void setUserLeave(boolean userLeave) {
        isUserLeave = userLeave;
    }


    public TypeOfUser getUserType() {
        return userType;
    }

    public void setUserType(TypeOfUser userType) {
        this.userType = userType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}