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

    /** categoría -> id del cosmético equipado en esa categoría. */
    private final Map<String, String> activeCosmetics = new HashMap<>();

    /**
     * "categoria:itemId" - namespaced a propósito (nota 8 del prompt original:
     * el Set plano sin categoría permitía colisiones entre categorías con el
     * mismo id de item).
     */
    private final Set<String> unlockedCosmetics = new HashSet<>();

    public PlayerData(UUID uuid, String name, double coins, boolean globalChat, int chatWarnings, boolean messagesEnabled) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.globalChat = globalChat;
        this.chatWarnings = chatWarnings;
        this.messagesEnabled = messagesEnabled;
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

    public void setCoins(double coins) {
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
}
