package org.dqnylux.mincore.config.models;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Usado por kill_messages.yml / death_messages.yml: causa de daño -> subcategoría
 * (default/weapon/killer) -> variantes de mensaje.
 */
public class MessagePackCosmetic extends CosmeticItem {

    public Map<String, Map<String, List<String>>> messages = new LinkedHashMap<>();
}
