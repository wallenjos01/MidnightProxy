package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.CommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConsoleHandler implements CommandSender {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConsoleHandler");

    private final ProxyServer server;

    private boolean running = false;
    private Thread thread = null;

    public ConsoleHandler(ProxyServer server) {
        this.server = server;
    }

    public void start() {

        assert !running;
        assert thread == null;

        running = true;
        thread = new Thread("Console Handler Thread") {
            @Override
            public void run() {

                BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

                try {
                    String line;
                    while (running && (line = lineReader.readLine()) != null) {
                        handleInput(line);
                    }
                } catch (IOException ex) {
                    LOGGER.error("An exception occurred while handling console input!", ex);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private void handleInput(String cmd) {

        String[] parts = cmd.split(" ");
        CommandExecutor exe = server.getCommands().get(parts[0]);

        if(exe == null) {
            sendMessage("Unknown command");
            return;
        }

        try {
            exe.execute(this, parts);
        } catch (Exception ex) {
            LOGGER.error("An error occurred while executing a command!", ex);
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public Proxy getProxy() {
        return server;
    }

    @Override
    public void sendMessage(String message) {
        System.out.println(message);
    }
}
