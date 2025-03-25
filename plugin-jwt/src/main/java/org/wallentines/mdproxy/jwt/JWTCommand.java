package org.wallentines.mdproxy.jwt;

import org.wallentines.jwt.KeyStore;
import org.wallentines.jwt.KeyType;
import org.wallentines.mdproxy.command.ArgumentParser;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.CommandSender;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Set;

public class JWTCommand implements CommandExecutor {

    private final Set<String> subcommands = Set.of("genKey", "clearKey");
    private final SecureRandom rand = new SecureRandom();

    private final ArgumentParser parser = new ArgumentParser()
            .addOption("type", 't', "hmac")
            .addOption("length", 'l', "32");

    @Override
    public void execute(CommandSender sender, String[] args) {

        ArgumentParser.ParseResult res = parser.parse(args);
        if(res.isError()) {
            sender.sendMessage(res.getError());
            return;
        }

        ArgumentParser.Parsed parsed = res.getOutput();

        String subcommand;
        if(parsed.getPositionalArgumentCount() < 3 || !subcommands.contains((subcommand = parsed.getPositionalArgument(1)))) {
            sender.sendMessage("Usage: " + args[0] + " genKey/clearKey <keyName> [-t hmac/aes/rsa] [-l <keyLength>]");
            return;
        }

        String name = parsed.getPositionalArgument(2);
        KeyStore store = sender.getProxy().getPluginManager().get(JWTPlugin.class).getKeyStore();

        switch (subcommand) {
            case "genKey":

                String lenStr = parsed.getValue("length");
                int keyLength = lenStr == null || lenStr.isEmpty() ? 32 : Integer.parseInt(lenStr);
                String type = parsed.getValue("type");
                switch (type) {
                    case "hmac" -> {
                        byte[] keyData = new byte[keyLength];
                        rand.nextBytes(keyData);

                        store.setKey(name, KeyType.HMAC, KeyType.HMAC.create(keyData).getOrThrow());
                    }
                    case "aes" -> {
                        byte[] keyData = new byte[keyLength];
                        rand.nextBytes(keyData);

                        store.setKey(name, KeyType.AES, KeyType.AES.create(keyData).getOrThrow());
                    }
                    case "rsa" -> {
                        try {
                            KeyPairGenerator factory = KeyPairGenerator.getInstance("RSA");
                            KeyPair kp = factory.generateKeyPair();

                            store.setKey(name, KeyType.RSA_PUBLIC, kp.getPublic());
                            store.setKey(name, KeyType.RSA_PRIVATE, kp.getPrivate());
                        } catch (GeneralSecurityException ex) {
                            sender.sendMessage("Unable to generate RSA key!");
                            throw new IllegalStateException(ex);
                        }
                    }
                    default -> {
                        sender.sendMessage("Key type " + type + " unknown!");
                        return;
                    }
                }

                sender.sendMessage("New key " + name + " generated");
                break;

            case "clearKey":

                switch (parsed.getValue("type")) {
                    case "hmac" -> store.clearKey(name, KeyType.HMAC);
                    case "aes" -> store.clearKey(name, KeyType.AES);
                    case "rsa" -> {
                        store.clearKey(name, KeyType.RSA_PUBLIC);
                        store.clearKey(name, KeyType.RSA_PRIVATE);
                    }
                }

                sender.sendMessage("Key " + name + " cleared");
        }
    }
}
