package org.dqnylux.mincore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.DeathSystemConfig;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockMenuHandler {

    private BedrockMenuHandler() {
    }

    public static boolean isBedrockPlayer(Player player) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    /** Diálogo nativo (ModalForm) preguntando si activar el rastreo GPS - solo jugadores Bedrock. */
    public static void promptTracking(Mincore plugin, Player player, int deathId) {
        if (!isBedrockPlayer(player)) return;

        DeathSystemConfig.BedrockMenu menu = plugin.getConfigManager().getDeathSystemConfig().bedrockMenu;
        SimpleForm form = SimpleForm.builder()
                .title(menu.title)
                .content(menu.content)
                .button(menu.confirmName)
                .button(menu.cancelName)
                .validResultHandler(response -> {
                    if (response.clickedButtonId() != 0) return;
                    player.getScheduler().run(plugin, task ->
                            Bukkit.dispatchCommand(player, "trackcore " + deathId), () -> {
                    });
                })
                .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
