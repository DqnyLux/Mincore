package org.dqnylux.mincore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }
}