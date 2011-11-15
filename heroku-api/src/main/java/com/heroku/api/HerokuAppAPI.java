package com.heroku.api;


import com.heroku.api.command.app.AppCommand;
import com.heroku.api.command.app.AppCreateCommand;
import com.heroku.api.command.app.AppDestroyCommand;
import com.heroku.api.command.log.LogsCommand;
import com.heroku.api.command.sharing.SharingAddCommand;
import com.heroku.api.command.sharing.SharingRemoveCommand;
import com.heroku.api.command.sharing.SharingTransferCommand;
import com.heroku.api.connection.Connection;

import java.util.Map;

public class HerokuAppAPI {

    final Connection<?> connection;
    final String appName;

    public HerokuAppAPI(Connection<?> connection, String name) {
        this.connection = connection;
        this.appName = name;
    }

    public Map<String, String> create(HerokuStack stack) {
        return connection.executeCommand(new AppCreateCommand(stack.value).withName(appName)).getData();
    }

    public HerokuAppAPI createAnd(HerokuStack stack) {
        create(stack);
        return this;
    }

    public void destroy() {
        connection.executeCommand(new AppDestroyCommand(appName));
    }

    public Map<String, String> info() {
        return connection.executeCommand(new AppCommand(appName)).getData();
    }

    public HerokuAppAPI addCollaborator(String collaborator) {
        connection.executeCommand(new SharingAddCommand(appName, collaborator));
        return this;
    }

    public HerokuAppAPI removeCollaborator(String collaborator) {
        connection.executeCommand(new SharingRemoveCommand(appName, collaborator));
        return this;
    }

    public void transferApp(String to) {
        connection.executeCommand(new SharingTransferCommand(appName, to));
    }

    public String getLogChunk() {
        return connection.executeCommand(connection.executeCommand(new LogsCommand(appName)).getData()).getData();
    }

    public HerokuAPI api() {
        return new HerokuAPI(connection);
    }

    public String getAppName() {
        return appName;
    }

    public Connection getConnection() {
        return connection;
    }
}
