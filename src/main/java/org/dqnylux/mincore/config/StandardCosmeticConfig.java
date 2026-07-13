package org.dqnylux.mincore.config;

import org.dqnylux.mincore.config.models.CosmeticItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reutilizada para las 12 categorías "estándar" (namecolors, chatcolors,
 * prefixes, icons, glows, join-messages, join-effects, projectile-effects,
 * kill-effects, death-effects, elytra-effects, trails) - un archivo YAML por
 * categoría, cada uno con su propio catálogo de items por defecto.
 */
public class StandardCosmeticConfig extends MincoreConfig {

    public Map<String, CosmeticItem> items = new LinkedHashMap<>();
}
