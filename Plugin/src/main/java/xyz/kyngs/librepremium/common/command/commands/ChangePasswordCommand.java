package xyz.kyngs.librepremium.common.command.commands;

import co.aikar.commands.annotation.*;
import net.kyori.adventure.audience.Audience;
import xyz.kyngs.librepremium.api.database.User;
import xyz.kyngs.librepremium.api.event.events.PasswordChangeEvent;
import xyz.kyngs.librepremium.common.AuthenticLibrePremium;
import xyz.kyngs.librepremium.common.command.Command;
import xyz.kyngs.librepremium.common.command.InvalidCommandArgument;
import xyz.kyngs.librepremium.common.event.events.AuthenticPasswordChangeEvent;

import java.util.UUID;

@CommandAlias("changepassword|changepass|passwd|passch")
public class ChangePasswordCommand extends Command {
    public ChangePasswordCommand(AuthenticLibrePremium plugin) {
        super(plugin);
    }

    @Default
    @Syntax("<oldPassword> <newPassword>")
    @CommandCompletion("oldPassword newPassword")
    public void onPasswordChange(Audience sender, UUID id, User user, String oldPass, @Single String newPass) {
        checkAuthorized(user);

        sender.sendMessage(getMessage("info-editing"));

        var hashed = user.getHashedPassword();
        var crypto = getCrypto(hashed);

        if (!crypto.matches(oldPass, hashed)) {
            throw new InvalidCommandArgument(getMessage("error-password-wrong"));
        }

        var defaultProvider = plugin.getDefaultCryptoProvider();

        user.setHashedPassword(defaultProvider.createHash(newPass));

        getDatabaseProvider().saveUser(user);

        sender.sendMessage(getMessage("info-edited"));

        plugin.getEventProvider().fire(PasswordChangeEvent.class, new AuthenticPasswordChangeEvent(user, sender, hashed));
    }

}
