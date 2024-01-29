package org.wallentines.mdproxy;

import org.wallentines.mdproxy.command.CommandExecutor;

import java.util.Scanner;

public class ConsoleHandler {

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

            exe.execute(server, parts);
        }

    }

    public void stop() {
        running = false;
        scanner.close();
    }
}
