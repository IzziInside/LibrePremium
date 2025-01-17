package xyz.kyngs.librepremium.common.authorization;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import xyz.kyngs.librepremium.api.authorization.AuthorizationProvider;
import xyz.kyngs.librepremium.api.database.User;
import xyz.kyngs.librepremium.api.event.events.AuthenticatedEvent;
import xyz.kyngs.librepremium.common.AuthenticLibrePremium;
import xyz.kyngs.librepremium.common.event.events.AuthenticAuthenticatedEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthenticAuthorizationProvider implements AuthorizationProvider {

    private final Set<UUID> unAuthorized;
    private final AuthenticLibrePremium plugin;

    public AuthenticAuthorizationProvider(AuthenticLibrePremium plugin) {
        this.plugin = plugin;
        unAuthorized = new HashSet<>();
    }

    @Override
    public boolean isAuthorized(UUID uuid) {
        return !unAuthorized.contains(uuid);
    }

    @Override
    public void authorize(UUID uuid, User user, Audience audience) {
        stopTracking(uuid);

        audience.clearTitle();
        plugin.getEventProvider().fire(AuthenticatedEvent.class, new AuthenticAuthenticatedEvent(user, audience));
        plugin.authorize(uuid, user, audience);
    }

    public void startTracking(UUID uuid, Audience audience) {
        unAuthorized.add(uuid);

        plugin.delay(() -> {
            sendInfoMessage(plugin.getDatabaseProvider().getByUUID(uuid), audience);
        }, 250);
    }

    private void sendInfoMessage(User user, Audience audience) {
        audience.sendMessage(plugin.getMessages().getMessage(user.isRegistered() ? "prompt-login" : "prompt-register"));
        if (!plugin.getConfiguration().useTitles()) return;
        audience.showTitle(Title.title(
                plugin.getMessages().getMessage(user.isRegistered() ? "title-login" : "title-register"),
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofSeconds(15),
                        Duration.ofMillis(0)
                )
        ));
    }

    public void stopTracking(UUID uuid) {
        unAuthorized.remove(uuid);
    }

    public void notifyUnauthorized() {
        for (UUID uuid : unAuthorized) {
            var audience = plugin.getAudienceForID(uuid);

            sendInfoMessage(plugin.getDatabaseProvider().getByUUID(uuid), audience);
        }
    }
}
