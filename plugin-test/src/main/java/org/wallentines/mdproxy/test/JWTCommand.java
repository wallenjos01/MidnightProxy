package org.wallentines.mdproxy.test;

import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.CommandSender;

import java.util.Set;

public class JWTCommand implements CommandExecutor {

    private final Set<String> subcommands = Set.of("genKey", "clearKey");

    @Override
    public void execute(CommandSender sender, String[] args) {

        if(args.length != 3 || !subcommands.contains(args[1])) {
            sender.sendMessage("Usage: " + args[0] + " [genKey/clearKey] <keyName>");
            return;
        }

        String key = args[2];
        KeyStore store = sender.getProxy().getPluginManager().get(JWTPlugin.class).getKeyStore();

        switch (args[1]) {
            case "genKey":

                store.generateKey(args[2]);
                sender.sendMessage("New key " + key + " generated");

                break;
            case "clearKey":

                store.clearKey(args[2]);
                sender.sendMessage("Key " + key + " cleared");
        }


    }
}
