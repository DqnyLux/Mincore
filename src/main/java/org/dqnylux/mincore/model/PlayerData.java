package org.dqnylux.mincore.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private double coins;
    private boolean globalChat;
    private int chatWarnings;
    private boolean messagesEnabled;
    private boolean mentionsEnabled;

    /** categoría -> id del cosmético equipado en esa categoría. */
    private final Map<String, String> activeCosmetics = new HashMap<>();

    /**
     * "categoria:itemId" - namespaced a propósito (nota 8 del prompt original:
     * el Set plano sin categoría permitía colisiones entre categorías con el
     * mismo id de item).
     */
    private final Set<String> unlockedCosmetics = new HashSet<>();

    /**
     * IDs de cosmetics/formats.yml activos a la vez (bold/italic/underline...) -
     * a diferencia de las demás categorías, no es "equipar uno", son varios
     * flags independientes que se aplican todos juntos. Separado en dos
     * ámbitos independientes - "chat" (mensajes) y "name" (nombre mostrado) -
     * porque activar negrita para el chat no debería forzarla también en el
     * nombre, y viceversa; cada menú (chatcolors/namecolors) controla su
     * propio ámbito.
     */
    private final Set<String> activeChatFormats = new HashSet<>();
    private final Set<String> activeNameFormats = new HashSet<>();

    public PlayerData(UUID uuid, String name, double coins, boolean globalChat, int chatWarnings,
                       boolean messagesEnabled, boolean mentionsEnabled) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.globalChat = globalChat;
        this.chatWarnings = chatWarnings;
        this.messagesEnabled = messagesEnabled;
        this.mentionsEnabled = mentionsEnabled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCoins() {
        return coins;
    }

    /**
     * BUG encontrado: Math.max(0, coins) NO sanea NaN/Infinity (Math.max con
     * NaN devuelve NaN) - si alguna vez llega un monto inválido (ej.
     * "/mincore eco give <jugador> NaN", que revxrsal sí parsea como double
     * válido), coins queda envenenado en NaN para siempre, y
     * CosmeticsGui#purchase()'s "data.getCoins() < cosmetic.price" es SIEMPRE
     * false para NaN - ese jugador desbloquea todo gratis desde ese momento.
     * Se descarta cualquier valor no-finito antes del clamp normal.
     */
    public void setCoins(double coins) {
        if (!Double.isFinite(coins)) return;
        this.coins = Math.max(0, coins);
    }

    public void addCoins(double amount) {
        setCoins(this.coins + amount);
    }

    public void removeCoins(double amount) {
        setCoins(this.coins - amount);
    }

    public boolean isGlobalChat() {
        return globalChat;
    }

    public void setGlobalChat(boolean globalChat) {
        this.globalChat = globalChat;
    }

    public int getChatWarnings() {
        return chatWarnings;
    }

    public void setChatWarnings(int chatWarnings) {
        this.chatWarnings = Math.max(0, chatWarnings);
    }

    public void addChatWarning() {
        this.chatWarnings++;
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public void setMessagesEnabled(boolean messagesEnabled) {
        this.messagesEnabled = messagesEnabled;
    }

    public boolean isMentionsEnabled() {
        return mentionsEnabled;
    }

    public void setMentionsEnabled(boolean mentionsEnabled) {
        this.mentionsEnabled = mentionsEnabled;
    }

    public String getActiveCosmetic(String category) {
        return activeCosmetics.get(category);
    }

    public void setActiveCosmetic(String category, String itemId) {
        activeCosmetics.put(category, itemId);
    }

    public void clearActiveCosmetic(String category) {
        activeCosmetics.remove(category);
    }

    public Map<String, String> getActiveCosmetics() {
        return activeCosmetics;
    }

    public boolean hasCosmeticUnlocked(String category, String itemId) {
        return unlockedCosmetics.contains(category + ":" + itemId);
    }

    public void unlockCosmetic(String category, String itemId) {
        unlockedCosmetics.add(category + ":" + itemId);
    }

    public Set<String> getUnlockedCosmetics() {
        return unlockedCosmetics;
    }

    public static final String FORMAT_SCOPE_CHAT = "chat";
    public static final String FORMAT_SCOPE_NAME = "name";

    public boolean isFormatActive(String scope, String formatId) {
        return formatsFor(scope).contains(formatId);
    }

    public void toggleFormat(String scope, String formatId) {
        Set<String> formats = formatsFor(scope);
        if (!formats.remove(formatId)) formats.add(formatId);
    }

    public Set<String> getActiveFormats(String scope) {
        return formatsFor(scope);
    }

    private Set<String> formatsFor(String scope) {
        return FORMAT_SCOPE_NAME.equals(scope) ? activeNameFormats : activeChatFormats;
    }
}
