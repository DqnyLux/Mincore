package org.dqnylux.mincore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.model.PlayerData;

import java.util.function.BiConsumer;

/**
 * Único punto de mutación de la economía interna. Solo opera sobre jugadores
 * online (la caché de PlayerManager es la fuente de verdad mientras el
 * jugador está conectado) - no hay edición de balance offline.
 */
public class EconomyAdminHandler {

    public enum Result {
        SUCCESS, PLAYER_OFFLINE, INVALID_AMOUNT
    }

    private final Mincore plugin;

    public EconomyAdminHandler(Mincore plugin) {
        this.plugin = plugin;
    }

    public Result give(String targetName, double amount) {
        return mutate(targetName, amount, PlayerData::addCoins);
    }

    public Result take(String targetName, double amount) {
        return mutate(targetName, amount, PlayerData::removeCoins);
    }

    public Result set(String targetName, double amount) {
        return mutate(targetName, amount, PlayerData::setCoins);
    }

    public Result giveSucres(String targetName, double amount) {
        return mutateSucres(targetName, amount, PlayerData::addSucres);
    }

    public Result takeSucres(String targetName, double amount) {
        return mutateSucres(targetName, amount, PlayerData::removeSucres);
    }

    public Result setSucres(String targetName, double amount) {
        return mutateSucres(targetName, amount, PlayerData::setSucres);
    }

    private Result mutate(String targetName, double amount, BiConsumer<PlayerData, Double> operation) {
        // "amount < 0" por sí solo NO rechaza NaN (NaN < 0 es false) - un
        // monto inválido (ej. /mincore eco give <jugador> NaN, que revxrsal
        // sí parsea como double "válido") pasaba este chequeo y envenenaba
        // coins a NaN, dejando a ese jugador con desbloqueos gratis
        // permanentes (ver PlayerData#setCoins). Se rechaza explícito acá
        // también para devolver INVALID_AMOUNT en vez de un no-op silencioso.
        if (!Double.isFinite(amount) || amount < 0) return Result.INVALID_AMOUNT;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return Result.PLAYER_OFFLINE;

        PlayerData data = plugin.getPlayerManager().get(target.getUniqueId());
        if (data == null) return Result.PLAYER_OFFLINE;

        operation.accept(data, amount);
        plugin.getPlayerManager().savePlayerAsync(data);
        return Result.SUCCESS;
    }

    private Result mutateSucres(String targetName, double amount, BiConsumer<PlayerData, Double> operation) {
        // Misma blindaje numérico que mutate(): NaN/Infinity NO pasan el
        // chequeo "amount < 0" y envenenarían sucres igual que coins.
        if (!Double.isFinite(amount) || amount < 0) return Result.INVALID_AMOUNT;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return Result.PLAYER_OFFLINE;

        PlayerData data = plugin.getPlayerManager().get(target.getUniqueId());
        if (data == null) return Result.PLAYER_OFFLINE;

        operation.accept(data, amount);
        plugin.getPlayerManager().savePlayerAsync(data);
        return Result.SUCCESS;
    }
}
