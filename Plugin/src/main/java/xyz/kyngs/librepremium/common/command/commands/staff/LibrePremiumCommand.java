package xyz.kyngs.librepremium.common.command.commands.staff;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.audience.Audience;
import xyz.kyngs.librepremium.api.configuration.CorruptedConfigurationException;
import xyz.kyngs.librepremium.api.database.User;
import xyz.kyngs.librepremium.api.event.events.PremiumLoginSwitchEvent;
import xyz.kyngs.librepremium.common.AuthenticLibrePremium;
import xyz.kyngs.librepremium.common.command.InvalidCommandArgument;
import xyz.kyngs.librepremium.common.event.events.AuthenticPremiumLoginSwitchEvent;
import xyz.kyngs.librepremium.common.util.GeneralUtil;

import javax.annotation.Syntax;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static xyz.kyngs.librepremium.common.AuthenticLibrePremium.DATE_TIME_FORMATTER;

@CommandAlias("librepremium")
public class LibrePremiumCommand extends StaffCommand {

    public LibrePremiumCommand(AuthenticLibrePremium plugin) {
        super(plugin);
    }

    @Subcommand("reload configuration")
    @CommandPermission("librepremium.reload.configuration")
    public void onReloadConfiguration(Audience audience) {

        audience.sendMessage(getMessage("info-reloading"));

        try {
            plugin.getConfiguration().reload(plugin);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidCommandArgument(getMessage("error-unknown"));
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            throw new InvalidCommandArgument(getMessage("error-corrupted-configuration",
                    "%cause%", "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()))
            );
        }

        audience.sendMessage(getMessage("info-reloaded"));

    }

    @Subcommand("reload messages")
    @CommandPermission("librepremium.reload.messages")
    public void onReloadMessages(Audience audience) {

        audience.sendMessage(getMessage("info-reloading"));

        try {
            plugin.getMessages().reload(plugin);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidCommandArgument(getMessage("error-unknown"));
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            throw new InvalidCommandArgument(getMessage("error-corrupted-messages",
                    "%cause%", "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()))
            );
        }

        audience.sendMessage(getMessage("info-reloaded"));

    }

    @Subcommand("user info")
    @CommandPermission("librepremium.user.info")
    @Syntax("<name>")
    @CommandCompletion("@players")
    public void onUserInfo(Audience audience, String name) {
        var user = getUserOtherWiseInform(name);

        audience.sendMessage(getMessage("info-user",
                "%uuid%", user.getUuid().toString(),
                "%premium_uuid%", user.getPremiumUUID() == null ? "N/A" : user.getPremiumUUID().toString(),
                "%last_seen%", DATE_TIME_FORMATTER.format(user.getLastSeen().toLocalDateTime()),
                "%joined%", DATE_TIME_FORMATTER.format(user.getJoinDate().toLocalDateTime())
        ));
    }

    public static void enablePremium(Audience audience, User user, AuthenticLibrePremium plugin) {
        var id = plugin.getUserOrThrowICA(user.getLastNickname());

        if (id == null) throw new InvalidCommandArgument(plugin.getMessages().getMessage("error-not-paid"));

        user.setPremiumUUID(id.uuid());

        plugin.getEventProvider().fire(PremiumLoginSwitchEvent.class, new AuthenticPremiumLoginSwitchEvent(user, audience));
    }

    @Subcommand("user migrate")
    @CommandPermission("librepremium.user.migrate")
    @Syntax("<name> <newName>")
    @CommandCompletion("@players newName")
    public void onUserMigrate(Audience audience, String name, String newName) {
        var user = getUserOtherWiseInform(name);

        if (getDatabaseProvider().getByName(newName) != null)
            throw new InvalidCommandArgument(getMessage("error-occupied-user",
                    "%name%", newName
            ));

        requireOffline(user);

        audience.sendMessage(getMessage("info-editing"));

        user.setLastNickname(newName);
        if (user.getPremiumUUID() != null) {
            user.setPremiumUUID(null);
            plugin.getEventProvider().fire(PremiumLoginSwitchEvent.class, new AuthenticPremiumLoginSwitchEvent(user, audience));
        }
        getDatabaseProvider().saveUser(user);

        audience.sendMessage(getMessage("info-edited"));
    }

    @Subcommand("user unregister")
    @CommandPermission("librepremium.user.unregister")
    @Syntax("<name>")
    @CommandCompletion("@players")
    public void onUserUnregister(Audience audience, String name) {
        var user = getUserOtherWiseInform(name);

        requireOffline(user);

        audience.sendMessage(getMessage("info-editing"));

        user.setHashedPassword(null);
        getDatabaseProvider().saveUser(user);

        audience.sendMessage(getMessage("info-edited"));
    }

    @Subcommand("user delete")
    @CommandPermission("librepremium.user.delete")
    @Syntax("<name>")
    @CommandCompletion("@players")
    public void onUserDelete(Audience audience, String name) {
        var user = getUserOtherWiseInform(name);

        requireOffline(user);

        audience.sendMessage(getMessage("info-deleting"));

        getDatabaseProvider().deleteUser(user);

        audience.sendMessage(getMessage("info-deleted"));
    }

    @Subcommand("user premium")
    @CommandPermission("librepremium.user.premium")
    @Syntax("<name>")
    @CommandCompletion("@players")
    public void onUserPremium(Audience audience, String name) {
        var user = getUserOtherWiseInform(name);

        requireOffline(user);

        audience.sendMessage(getMessage("info-editing"));

        enablePremium(audience, user, plugin);

        getDatabaseProvider().saveUser(user);

        audience.sendMessage(getMessage("info-edited"));
    }

    @Subcommand("user cracked")
    @CommandPermission("librepremium.user.cracked")
    @Syntax("<name>")
    @CommandCompletion("@players")
    public void onUserCracked(Audience audience, String name) {
        var user = getUserOtherWiseInform(name);

        requireOffline(user);

        audience.sendMessage(getMessage("info-editing"));

        user.setPremiumUUID(null);
        getDatabaseProvider().saveUser(user);

        audience.sendMessage(getMessage("info-edited"));
    }

    @Subcommand("user register")
    @CommandPermission("librepremium.user.register")
    @Syntax("<name> <password>")
    @CommandCompletion("@players password")
    public void onUserRegister(Audience audience, String name, String password) {
        audience.sendMessage(getMessage("info-registering"));

        var user = getDatabaseProvider().getByName(name);

        if (user != null) {
            throw new InvalidCommandArgument(getMessage("error-occupied-user"));
        }

        user = new User(
                plugin.generateNewUUID(name, plugin.getUserOrThrowICA(name).uuid()),
                null,
                plugin.getDefaultCryptoProvider().createHash(password),
                name,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
        );

        getDatabaseProvider().saveUser(user);

        audience.sendMessage(getMessage("info-registered"));
    }

}
