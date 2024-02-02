package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.CommandSender;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleHandler implements CommandSender {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConsoleHandler");
    private final Scanner scanner;
    private final ProxyServer server;
    private boolean running = false;


    public ConsoleHandler(ProxyServer server) {

        this.server = server;
        this.scanner = new Scanner(System.in);
    }

    public void start() {

        running = true;
        while(running) {

            String cmd = scanner.nextLine();
            String[] parts = cmd.split(" ");

            CommandExecutor exe = server.getCommands().get(parts[0]);

            if(exe == null) {
                System.out.println("Unknown command");
                continue;
            }

            try {
                exe.execute(this, parts);
            } catch (Exception ex) {
                LOGGER.error("An error occurred while executing a command!", ex);
            }
        }

    }

    public void stop() {
        running = false;
        scanner.close();
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
