package org.dqnylux.mincore.config.models;

import org.dqnylux.mincore.config.MincoreConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo base reutilizado por las 12 categorías "estándar" de cosméticos.
 * Los parámetros visuales (particle/color/radius/speed/durationTicks/sound)
 * existen aquí - no en las clases de efecto Java - para cumplir el requisito
 * de cero hardcodeo (sección 17): un admin ajusta un efecto editando YAML,
 * nunca recompilando.
 */
public class CosmeticItem extends MincoreConfig {

    public String material = "PAPER";
    public String displayName = "Cosmético";
    public double price = 0.0;

    /** Uso según categoría: color/prefijo/mensaje de texto, o estilo de trail. */
    public String value = "";

    /** Solo para join-messages: mensaje difundido al salir el jugador (vacío = usar el default de messages.yml). */
    public String quitValue = "";

    /** ID que debe coincidir con CosmeticEffect/ElytraEffect/ProjectileEffect#getId(). */
    public String effectType = "";

    public String particle = "";

    /** Color de la partícula cuando aplica (DUST/REDSTONE). Formato: #RRGGBB o nombre (RED, AQUA...). */
    public String color = "";

    public double radius = 1.0;

    /** Velocidad/extra de las partículas. -1 = usar el valor por defecto del efecto. */
    public double speed = -1.0;

    /** Duración máxima en ticks para efectos continuos (ej. projectile-effects). 0 = sin límite. */
    public int durationTicks = 0;

    public String sound = "";
    public List<String> lore = new ArrayList<>();
}
