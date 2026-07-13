package org.dqnylux.mincore.config;

import eu.okaeri.configs.annotation.Comment;

public class ChatFormatConfig extends MincoreConfig {

    public Parts parts = new Parts();
    public Mentions mentions = new Mentions();

    public static class Parts extends MincoreConfig {
        @Comment("%luckperms_prefix% requiere LuckPerms+PlaceholderAPI - el cosmético de \"prefixes\" ya escribe ahí vía LuckPerms si está instalado.")
        public String prefix = "%luckperms_prefix%";

        @Comment("%player_name% y %namecolor% los resuelve Mincore directamente (no requieren PlaceholderAPI). %namecolor% toma el valor del cosmético equipado en la categoría \"namecolors\".")
        public String name = "%namecolor%%player_name%";

        public String icon = "";
        public String arrow = " <#888888>» ";

        @Comment("Color aplicado al cuerpo del mensaje. %chatcolor% toma el cosmético equipado en \"chatcolors\". El texto del mensaje en sí siempre se agrega después, nunca aquí (protección anti-inyección).")
        public String message = "%chatcolor%";
    }

    public static class Mentions extends MincoreConfig {
        @Comment("¿Activar la detección y resaltado de menciones @jugador?")
        public boolean enabled = true;

        public String highlightColor = "<#FFD700>";

        @Comment("¿Mostrar un actionbar al jugador mencionado?")
        public boolean actionbar = true;

        @Comment("¿Reproducir un sonido al jugador mencionado?")
        public boolean sound = true;

        public String actionbarMessage = "<#FFD700>%player% te ha mencionado.";
    }
}
