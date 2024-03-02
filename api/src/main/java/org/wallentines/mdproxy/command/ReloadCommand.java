package org.wallentines.mdproxy.command;

public class ReloadCommand implements CommandExecutor {
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.getProxy().reload();
        sender.sendMessage("Reloaded proxy configuration");
    }
}
