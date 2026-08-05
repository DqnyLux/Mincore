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

    /**
     * Permiso EXTRA requerido para usar este cosmético, sin importar el
     * precio - vacío = sin restricción de permiso (comportamiento de
     * siempre). A diferencia de "coreec.cosmetic.<categoria>.<id>" (un
     * permiso alternativo de DESBLOQUEO que se suma al precio/compra), este
     * campo es una restricción DURA: si está seteado y el jugador no lo
     * tiene, isAccessible() devuelve false SIEMPRE, incluso si el ítem es
     * gratis (price=0) - pensado para cosas como formats.yml, donde "gratis"
     * no debería significar "cualquiera puede usarlo sin que un admin lo
     * habilite primero".
     */
    public String permission = "";

    /**
     * Rareza del cosmético (comun/raro/epico/legendario) - la usa Pozo
     * Millonario cuando una caja está en modo useCosmeticPool: la caja
     * sortea una rareza según sus porcentajes y entrega un cosmético al azar
     * de ESA rareza de todo el catálogo. Los ids deben coincidir con los ids
     * de rareza de pozo/box_types.yml.
     */
    public String rarity = "comun";

    /** Uso según categoría: color/prefijo/mensaje de texto, o estilo de trail. */
    public String value = "";

    /** Solo para join-messages: mensaje difundido al salir el jugador (vacío = usar el default de messages.yml). */
    public String quitValue = "";

    /** Solo para join-messages tipo "banner" multi-línea - si no está vacío, tiene prioridad sobre "value" (una sola línea). Cada línea se difunde por separado, admite &lt;center&gt;. */
    public List<String> lines = new ArrayList<>();

    /** Análogo a "lines" para el mensaje de salida - prioridad sobre "quitValue". */
    public List<String> quitLines = new ArrayList<>();

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

    /**
     * Cuántos ticks dura la previsualización (clic derecho en el menú) de
     * ESTE cosmético en particular - sesión de zona/cierre de menú incluidos.
     * 0 = usar el default global (modules.cosmetics.previewZone.sessionDurationTicks
     * o previewMenuCloseTicks según haya zona configurada). Cada efecto anima
     * durante un tiempo distinto (un portal dimensional dura más que un burst
     * instantáneo) - este campo deja ajustarlo por cosmético sin tocar Java.
     */
    public int previewDurationTicks = 0;

    public String sound = "";
    public List<String> lore = new ArrayList<>();

    /** Cantidad de partículas por spawn. -1 = usar el valor por defecto del efecto. */
    public int count = -1;

    /** Número de puntos/orbes/estelas de la figura (anillos, hélices, orbitas...). -1 = usar el valor por defecto del efecto. */
    public int pointCount = -1;

    /** Segundo color, para efectos de dos tonos (ej. hélice de dos hebras). Vacío = usar "color". */
    public String secondaryColor = "";

    /** Segunda partícula, para efectos con capas de partículas distintas. Vacío = ninguna. */
    public String secondaryParticle = "";

    /** Lista de materiales para efectos que eligen uno al azar por iteración (ej. volcán). */
    public List<String> materials = new ArrayList<>();
}
