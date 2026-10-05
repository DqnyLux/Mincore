package org.dqnylux.mincore.menus;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.CategoriesMenuConfig;
import org.dqnylux.mincore.config.CosmeticsMenuConfig;
import org.dqnylux.mincore.config.MessagesConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;
import org.dqnylux.mincore.config.models.MenuItem;
import org.dqnylux.mincore.managers.cosmetics.EffectUtils;
import org.dqnylux.mincore.model.PlayerData;
import org.dqnylux.mincore.utils.MenuStructure;
import org.dqnylux.mincore.utils.TextUtils;
import org.dqnylux.mincore.utils.WorldValidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grid paginado genérico para UNA categoría - la misma clase sirve a todas
 * las categorías estándar. El layout completo (forma, filas, controles de
 * paginación, botón de volver, y qué slots reciben cosméticos) viene de
 * menus/cosmetics_menu.yml (CosmeticsMenuConfig), parseado como cuadrícula
 * de símbolos (MenuStructure) - sección 17, cero hardcodeo.
 *
 * Corrige la nota 5 del prompt original (la lógica de acceso/equipar estaba
 * duplicada ~150 líneas entre CategoryMenu y CosmeticsGui): aquí solo existe
 * una vez, en isAccessible()/handleClick(). Clic sobre el cosmético ya
 * equipado lo desequipa (y revierte su efecto persistente: glow, prefix o
 * suffix según la categoría).
 */
public class CosmeticsGui {

    public static boolean isCategoryEnabled(Mincore plugin, String category) {
        return plugin.getConfigManager().getMainConfig().modules.cosmetics.categories.getOrDefault(category, true);
    }

    /**
     * Orden del grid, elegido por el propio jugador con el botón sortSymbol
     * (cosmetics_menu.yml) - puramente de sesión, no se guarda en BD (es un
     * filtro de vista, no una preferencia de cuenta). DEFAULT respeta el
     * orden del catálogo YAML tal cual.
     */
    private enum SortMode {
        DEFAULT("<#AAAAAA>Catálogo"),
        UNLOCKED_FIRST("<#5CE65C>Desbloqueados primero"),
        PRICE_ASC("<#55FFFF>Precio: menor a mayor"),
        ALPHABETICAL("<#FFD700>Alfabético (A-Z)");

        final String label;

        SortMode(String label) {
            this.label = label;
        }

        SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static final Map<java.util.UUID, SortMode> sortPreference = new java.util.concurrent.ConcurrentHashMap<>();

    /** Llamado al desconectarse - evita acumular una entrada por cada jugador que alguna vez abrió un menú de cosméticos. */
    public static void clearSortPreference(java.util.UUID uuid) {
        sortPreference.remove(uuid);
    }

    private static Map<String, CosmeticItem> sortedItems(Mincore plugin, Player player, String category, Map<String, CosmeticItem> items, SortMode mode) {
        if (mode == SortMode.DEFAULT) return items;

        List<Map.Entry<String, CosmeticItem>> entries = new ArrayList<>(items.entrySet());
        Comparator<Map.Entry<String, CosmeticItem>> comparator = switch (mode) {
            case UNLOCKED_FIRST -> Comparator.comparing(e -> !isAccessible(plugin, player, category, e.getKey(), e.getValue()));
            case PRICE_ASC -> Comparator.comparingDouble(e -> e.getValue().price);
            case ALPHABETICAL -> Comparator.comparing(e -> plainName(e.getValue().displayName));
            default -> null;
        };
        if (comparator == null) return items;

        entries.sort(comparator);
        Map<String, CosmeticItem> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, CosmeticItem> entry : entries) sorted.put(entry.getKey(), entry.getValue());
        return sorted;
    }

    /** Nombre sin tags de MiniMessage, para comparar alfabéticamente sin que "<#FFD700>" decida el orden. */
    private static String plainName(String displayName) {
        return displayName == null ? "" : displayName.replaceAll("<[^>]*>", "").toLowerCase();
    }

    /**
     * "wings" (WingsConfig) y "kill-messages"/"death-messages" (MessagePackConfig)
     * no viven en StandardCosmeticConfig como el resto de categorías - son
     * tipos aparte con campos extra (ajustes de aleteo, mensajes por causa),
     * pero WingCosmetic/MessagePackCosmetic ambos extends CosmeticItem, así
     * que el mismo grid genérico (CosmeticsGui/CategoryMenu) sirve para las
     * tres con solo resolver el catálogo desde el lugar correcto. Devuelve
     * null si la categoría no existe en ningún lado.
     */
    public static Map<String, CosmeticItem> itemsForCategory(Mincore plugin, String category) {
        return switch (category) {
            case "wings" -> new LinkedHashMap<>(plugin.getCosmeticConfigManager().getWings().items);
            case "kill-messages" -> new LinkedHashMap<>(plugin.getCosmeticConfigManager().getKillMessages().items);
            case "death-messages" -> new LinkedHashMap<>(plugin.getCosmeticConfigManager().getDeathMessages().items);
            default -> {
                StandardCosmeticConfig config = plugin.getCosmeticConfigManager().getCategory(category);
                yield config == null ? null : config.items;
            }
        };
    }

    public static void open(Player player, Mincore plugin, String category) {
        open(player, plugin, category, 1);
    }

    /** page es 1-indexado (triumph-gui) - usado al reabrir el menú tras una previsualización, para volver a la misma página y no siempre a la primera. */
    public static void open(Player player, Mincore plugin, String category, int page) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (!isCategoryEnabled(plugin, category)) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.categoryDisabled));
            return;
        }

        Map<String, CosmeticItem> items = itemsForCategory(plugin, category);
        if (items == null) return;

        CosmeticsMenuConfig layout = plugin.getConfigManager().getCosmeticsMenuConfig();
        String categoryDisplayName = categoryDisplayName(plugin, category);

        PaginatedGui gui = Gui.paginated()
                .title(TextUtils.format(messages.menus.categoryTitle.replace("%category%", categoryDisplayName)))
                .rows(layout.rows)
                .disableAllInteractions()
                .create();

        Map<Character, List<MenuStructure.Cell>> cells = MenuStructure.parse(layout.structure);
        char contentSymbol = symbol(layout.contentSymbol);
        char formatsSymbol = symbol(layout.formatsSymbol);
        char previousSymbol = symbol(layout.previousSymbol);
        char nextSymbol = symbol(layout.nextSymbol);
        char backSymbol = symbol(layout.backSymbol);
        char infoHeadSymbol = symbol(layout.infoHeadSymbol);
        char sortSymbol = symbol(layout.sortSymbol);
        SortMode sortMode = sortPreference.getOrDefault(player.getUniqueId(), SortMode.DEFAULT);
        items = sortedItems(plugin, player, category, items, sortMode);

        // "formats" no tiene menú propio - sus toggles (negrita, cursiva...) se
        // ven mejor probados junto al color de nombre/chat que van a acompañar,
        // así que se fijan en una fila propia (formatsSymbol) de esos dos
        // grids en vez de mezclarse con la paginación del resto del catálogo.
        // namecolors controla el formato del NOMBRE, chatcolors el del CHAT -
        // son estados independientes (PlayerData.FORMAT_SCOPE_NAME/CHAT), así
        // que activarlo en un menú no lo prende en el otro.
        String formatsScope = "namecolors".equals(category) ? PlayerData.FORMAT_SCOPE_NAME
                : "chatcolors".equals(category) ? PlayerData.FORMAT_SCOPE_CHAT : null;
        boolean showFormatsRow = formatsScope != null && isCategoryEnabled(plugin, FORMATS_CATEGORY);
        StandardCosmeticConfig formats = showFormatsRow ? plugin.getCosmeticConfigManager().getCategory(FORMATS_CATEGORY) : null;

        // Se guardan los slots del botón "anterior" para poder ocultarlo en la
        // página 1 (no tiene sentido un botón que no lleva a ningún lado) y
        // volver a mostrarlo/ocultarlo cada vez que el jugador navega - un
        // array de 1 porque el símbolo puede no existir aún cuando se arma el
        // click action de "siguiente" (el orden de iteración del Map de
        // símbolos no está garantizado).
        List<MenuStructure.Cell>[] previousCellsHolder = new List[1];

        for (Map.Entry<Character, List<MenuStructure.Cell>> entry : cells.entrySet()) {
            char sym = entry.getKey();
            // El área de contenido se deja SIN ocupar a propósito: PaginatedGui
            // llena automáticamente cualquier slot libre (sin GuiItem) con los
            // ítems de la página, en orden de slot.
            if (sym == contentSymbol) continue;

            if (sym == formatsSymbol) {
                if (formats != null) {
                    placeFormatsRow(gui, plugin, player, formats, formatsScope, entry.getValue());
                    continue;
                }
                // Fuera de namecolors/chatcolors esta fila no tiene formatos que
                // mostrar - se libera como contenido normal (igual que
                // contentSymbol) en vez de quedar reservada/vacía, o esas
                // categorías perderían una fila entera de cosméticos por nada.
                continue;
            }

            // "." son los 2 slots decorativos que enmarcan la fila de formats
            // (a los lados de las F) - solo tienen sentido como marco fijo
            // cuando esa fila realmente muestra formatos (namecolors/chatcolors).
            // En cualquier otra categoría, con toda la fila ya liberada como
            // contenido, dejar esos 2 slots reservados se veía como si los
            // cosméticos "esquivaran" ese punto exacto de la cuadrícula - se
            // liberan también.
            if (sym == '.' && formats == null) continue;

            GuiItem item;
            if (sym == '#') {
                item = background(plugin, layout.fillerMaterial);
            } else if (sym == previousSymbol) {
                previousCellsHolder[0] = entry.getValue();
                item = previousButtonItem(plugin, layout, gui, page, previousCellsHolder);
            } else if (sym == nextSymbol) {
                item = controlButton(plugin, layout.nextButton, click -> {
                    gui.next();
                    refreshPreviousButton(plugin, layout, gui, previousCellsHolder);
                });
            } else if (sym == backSymbol) {
                item = controlButton(plugin, layout.backButton, click -> CategoryMenu.open(player, plugin));
            } else if (sym == infoHeadSymbol) {
                item = infoHead(plugin, player, layout.infoHead);
            } else if (sym == sortSymbol) {
                item = sortButtonItem(plugin, player, layout, category, sortMode);
            } else {
                // "." o cualquier símbolo no reconocido (incluye formatsSymbol
                // fuera de namecolors/chatcolors): slot reservado y vacío -
                // IMPORTANTE ocuparlo con un GuiItem (aunque sea AIR) para que
                // PaginatedGui no lo confunda con área de contenido.
                item = reserved();
            }

            for (MenuStructure.Cell cell : entry.getValue()) {
                gui.setItem(cell.row(), cell.col(), item);
            }
        }

        // "formats" no es "equipar uno" como el resto - son flags independientes
        // que se prenden/apagan todos a la vez, así que "Nada" no aplica ahí.
        if (!FORMATS_CATEGORY.equals(category)) {
            gui.addItem(buildNoneItem(gui, plugin, player, category, layout.noneItem));
        }
        for (Map.Entry<String, CosmeticItem> entry : items.entrySet()) {
            gui.addItem(buildItem(gui, plugin, player, category, entry.getKey(), entry.getValue()));
        }

        gui.open(player, page);
    }

    /** Fija (no pagina) los toggles de formats.yml en los slots reservados con formatsSymbol - visibles en TODAS las páginas, siempre en la misma fila. */
    private static void placeFormatsRow(PaginatedGui gui, Mincore plugin, Player player, StandardCosmeticConfig formats, String scope, List<MenuStructure.Cell> slots) {
        String pseudoCategory = formatsPseudoCategory(scope);
        int i = 0;
        for (Map.Entry<String, CosmeticItem> entry : formats.items.entrySet()) {
            if (i >= slots.size()) break;
            MenuStructure.Cell cell = slots.get(i++);
            gui.setItem(cell.row(), cell.col(),
                    buildItem(gui, plugin, player, pseudoCategory, entry.getKey(), entry.getValue()));
        }
        for (; i < slots.size(); i++) {
            MenuStructure.Cell cell = slots.get(i);
            gui.setItem(cell.row(), cell.col(), reserved());
        }
    }

    /** Nombre configurable de la categoría (menus/categories_menu.yml) - si no está mapeada, usa el id crudo. */
    private static String categoryDisplayName(Mincore plugin, String category) {
        CategoriesMenuConfig categoriesLayout = plugin.getConfigManager().getCategoriesMenuConfig();
        return categoriesLayout.categories.values().stream()
                .filter(button -> category.equals(button.category))
                .map(button -> button.displayName)
                .findFirst()
                .orElse(category);
    }

    private static char symbol(String value) {
        return value == null || value.isEmpty() ? '\0' : value.charAt(0);
    }

    private static GuiItem background(Mincore plugin, String material) {
        return PaperItemBuilder.from(parseMaterial(material))
                .name(Component.empty())
                .flags(itemFlags(plugin))
                .asGuiItem();
    }

    private static GuiItem reserved() {
        return PaperItemBuilder.from(Material.AIR).asGuiItem();
    }

    /** %sort% en sortButton.displayName/lore se reemplaza por la etiqueta del modo actual - clic cicla al siguiente y reabre el menú (el orden cambia, así que siempre vuelve a la página 1). */
    private static GuiItem sortButtonItem(Mincore plugin, Player player, CosmeticsMenuConfig layout, String category, SortMode currentMode) {
        List<Component> lore = new ArrayList<>();
        for (String line : layout.sortButton.lore) {
            lore.add(TextUtils.format(line.replace("%sort%", currentMode.label)));
        }

        return PaperItemBuilder.from(parseMaterial(layout.sortButton.material))
                .name(TextUtils.format(layout.sortButton.displayName.replace("%sort%", currentMode.label)))
                .lore(lore)
                .flags(itemFlags(plugin))
                .asGuiItem(click -> {
                    sortPreference.put(player.getUniqueId(), currentMode.next());
                    CosmeticsGui.open(player, plugin, category, 1);
                });
    }

    /**
     * Cabeza decorativa con la skin real del jugador - misma info (monedas,
     * cosméticos desbloqueados en total...) en todos los menús, no solo el de
     * perfil. %player%/%coins%/%global_unlocked%/%global_total% se resuelven
     * acá mismo; cualquier otro placeholder (ej. %luckperms_prefix%) pasa por
     * PlaceholderAPI vía TextUtils.format(Player, String) si está instalado.
     */
    public static GuiItem infoHead(Mincore plugin, Player viewer, MenuItem item) {
        ItemStack skull = new ItemStack(parseMaterial(item.material));
        if (skull.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwnerProfile(viewer.getPlayerProfile());
                skull.setItemMeta(meta);
            }
        }

        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        double sucres = data == null ? 0 : data.getSucres();
        long globalUnlocked = globalUnlocked(plugin, viewer);
        long globalTotal = globalTotal(plugin);

        List<Component> lore = new ArrayList<>();
        for (String line : item.lore) {
            lore.add(TextUtils.format(viewer, line
                    .replace("%player%", viewer.getName())
                    .replace("%coins%", String.valueOf(sucres))
                    .replace("%global_unlocked%", String.valueOf(globalUnlocked))
                    .replace("%global_total%", String.valueOf(globalTotal))));
        }

        return PaperItemBuilder.from(skull)
                .name(TextUtils.format(viewer, item.displayName.replace("%player%", viewer.getName())))
                .lore(lore)
                .flags(itemFlags(plugin))
                .asGuiItem();
    }

    /** Botón "anterior" real (con acción) si currentPage > 1, o relleno (igual que el resto del fondo) si ya está en la página 1 - no tiene sentido un botón que no lleva a ningún lado, y un AIR ahí dejaría un hueco visual distinto al resto del fondo. */
    private static GuiItem previousButtonItem(Mincore plugin, CosmeticsMenuConfig layout, PaginatedGui gui, int currentPage, List<MenuStructure.Cell>[] previousCellsHolder) {
        if (currentPage <= 1) return background(plugin, layout.fillerMaterial);

        return controlButton(plugin, layout.previousButton, click -> {
            gui.previous();
            refreshPreviousButton(plugin, layout, gui, previousCellsHolder);
        });
    }

    /** Reconstruye el botón "anterior" tras cada navegación, para que aparezca/desaparezca según la página en la que quede el jugador. */
    private static void refreshPreviousButton(Mincore plugin, CosmeticsMenuConfig layout, PaginatedGui gui, List<MenuStructure.Cell>[] previousCellsHolder) {
        List<MenuStructure.Cell> cells = previousCellsHolder[0];
        if (cells == null) return;

        GuiItem item = previousButtonItem(plugin, layout, gui, gui.getCurrentPageNum(), previousCellsHolder);
        for (MenuStructure.Cell cell : cells) {
            gui.updateItem(cell.row(), cell.col(), item);
        }
    }

    private static GuiItem controlButton(Mincore plugin, MenuItem item, GuiAction<InventoryClickEvent> action) {
        List<Component> lore = new ArrayList<>();
        for (String line : item.lore) lore.add(TextUtils.format(line));

        return PaperItemBuilder.from(parseMaterial(item.material))
                .name(TextUtils.format(item.displayName))
                .lore(lore)
                .flags(itemFlags(plugin))
                .asGuiItem(action);
    }

    private static final String FORMATS_CATEGORY = "formats";

    /**
     * Categorías "pseudo" usadas SOLO para la fila fija de formats.yml -
     * nunca tienen catálogo propio ni menú propio, existen para que
     * buildItem()/buildDisplay() sepan si el clic activa el formato del
     * NOMBRE (namecolors) o del CHAT (chatcolors), sin mezclar ambos estados.
     */
    private static final String FORMATS_NAME_CATEGORY = "formats:name";
    private static final String FORMATS_CHAT_CATEGORY = "formats:chat";

    private static String formatsPseudoCategory(String scope) {
        return PlayerData.FORMAT_SCOPE_NAME.equals(scope) ? FORMATS_NAME_CATEGORY : FORMATS_CHAT_CATEGORY;
    }

    private static boolean isFormatsPseudoCategory(String category) {
        return FORMATS_NAME_CATEGORY.equals(category) || FORMATS_CHAT_CATEGORY.equals(category);
    }

    private static String formatsScopeOf(String category) {
        return FORMATS_NAME_CATEGORY.equals(category) ? PlayerData.FORMAT_SCOPE_NAME : PlayerData.FORMAT_SCOPE_CHAT;
    }

    private static GuiItem buildItem(PaginatedGui gui, Mincore plugin, Player player, String category, String id, CosmeticItem cosmetic) {
        return PaperItemBuilder.from(buildDisplay(plugin, player, category, id, cosmetic))
                .asGuiItem(click -> {
                    if (click.getClick() == ClickType.RIGHT) {
                        // Cierra el menú mientras dura la previsualización - si se queda
                        // abierto, el inventario tapa al jugador/NPC y no se ve el efecto -
                        // y lo reabre en la misma categoría y página al terminar.
                        int page = gui.getCurrentPageNum();
                        Integer closeTicks = preview(plugin, player, category, cosmetic);
                        if (closeTicks != null) {
                            player.closeInventory();
                            player.getScheduler().runDelayed(plugin, task -> CosmeticsGui.open(player, plugin, category, page), () -> {}, Math.max(1, closeTicks));
                        }
                        return;
                    }

                    MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                    PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
                    if (data == null) return;

                    if (isFormatsPseudoCategory(category)) {
                        // El toggle de formats no pasa por isAccessible()/purchase()
                        // (no hay "comprar", es prender/apagar) - el permiso se
                        // chequea acá directo, si no un formato "gratis" (price=0,
                        // como TODOS los de formats.yml) sería usable por cualquiera.
                        if (cosmetic.permission != null && !cosmetic.permission.isBlank() && !player.hasPermission(cosmetic.permission)) {
                            player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.noAccess));
                            return;
                        }
                        String scope = formatsScopeOf(category);
                        data.toggleFormat(scope, id);
                        plugin.getPlayerManager().savePlayerAsync(data);
                        String formatMessage = (data.isFormatActive(scope, id)
                                ? messages.cosmetics.equipped : messages.cosmetics.unequipped)
                                .replace("%cosmetic%", cosmetic.displayName);
                        player.sendMessage(TextUtils.format(messages.prefix + formatMessage));
                        // Fila fija (placeFormatsRow, gui.setItem) - NO forma parte
                        // del pool paginado, así que se refresca con updateItem()
                        // (slot fijo), no updatePageItem() (índice de página).
                        gui.updateItem(click.getSlot(), buildDisplay(plugin, player, category, id, cosmetic));
                        return;
                    }

                    if (id.equals(data.getActiveCosmetic(category))) {
                        data.clearActiveCosmetic(category);
                        plugin.getPlayerManager().savePlayerAsync(data);
                        removeSideEffects(plugin, player, category);
                        player.sendMessage(TextUtils.format(messages.prefix
                                + messages.cosmetics.unequipped.replace("%cosmetic%", cosmetic.displayName)));
                        gui.updatePageItem(click.getSlot(), buildDisplay(plugin, player, category, id, cosmetic));
                        return;
                    }

                    String equipMessage = messages.cosmetics.equipped;
                    if (!isAccessible(plugin, player, category, id, cosmetic)) {
                        if (!purchase(plugin, player, data, category, id, cosmetic)) return;
                        equipMessage = messages.cosmetics.purchased;
                    }

                    data.setActiveCosmetic(category, id);
                    plugin.getPlayerManager().savePlayerAsync(data);
                    applySideEffects(plugin, player, category, cosmetic);
                    player.sendMessage(TextUtils.format(messages.prefix + equipMessage.replace("%cosmetic%", cosmetic.displayName)));
                    gui.updatePageItem(click.getSlot(), buildDisplay(plugin, player, category, id, cosmetic));
                });
    }

    /** Descuenta el precio y desbloquea el cosmético si el jugador tiene suficientes sucres; si no, avisa cuántas le faltan. */
    private static boolean purchase(Mincore plugin, Player player, PlayerData data, String category, String id, CosmeticItem cosmetic) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        if (data.getSucres() < cosmetic.price) {
            double missing = cosmetic.price - data.getSucres();
            player.sendMessage(TextUtils.format(messages.prefix
                    + messages.cosmetics.noCoins.replace("%missing%", String.valueOf(missing))));
            return false;
        }

        data.removeSucres(cosmetic.price);
        data.unlockCosmetic(category, id);
        plugin.getPlayerManager().persistUnlock(data.getUuid(), category, id);
        return true;
    }

    /**
     * En v1 esto quedó a medio hacer - mensajes de "previsualizando" y un lore
     * de "clic derecho" en el YAML, pero sin listener que los conectara nunca.
     * Aquí sí funciona: clic derecho reproduce el cosmético sin equiparlo (no
     * gasta monedas ni cambia lo que el jugador tiene puesto). glows/prefixes/
     * icons quedan fuera a propósito - dependen de estado externo persistente
     * (equipos de scoreboard, nodos de LuckPerms) que no tiene sentido activar
     * y desactivar solo por unos segundos.
     */
    private static final Set<String> VISUAL_ONLY_CATEGORIES = Set.of("glows", "prefixes", "icons", "formats");

    /**
     * namecolors/chatcolors (previewChatLine), join-messages (previewMessage)
     * y kill-messages/death-messages (previewMessagePack) solo mandan texto
     * al chat - no hay nada que reproducir sobre un NPC en el mundo, así que
     * no tiene sentido teletransportar al jugador a la zona de preview por
     * ellas (el menú igual se cierra un rato, ver preview()). Antes
     * kill-messages/death-messages caían al "default" del switch de abajo,
     * que busca item.effectType en el registro de efectos de partículas -
     * pero MessagePackCosmetic no tiene effectType (usa "messages", un mapa
     * causa->variante->líneas), así que ese preview no mostraba nada.
     */
    private static final Set<String> TEXT_ONLY_CATEGORIES =
            Set.of("namecolors", "chatcolors", "join-messages", "kill-messages", "death-messages");

    /**
     * Devuelve cuántos ticks debe quedar cerrado el menú antes de reabrirse
     * (para dejar ver el efecto), o null si no se disparó ninguna
     * previsualización (desactivada, o categoría visual-only).
     */
    private static Integer preview(Mincore plugin, Player player, String category, CosmeticItem cosmetic) {
        var cosmeticsConfig = plugin.getConfigManager().getMainConfig().modules.cosmetics;
        if (!cosmeticsConfig.previewEnabled) return null;

        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        if (isFormatsPseudoCategory(category) || VISUAL_ONLY_CATEGORIES.contains(category)) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.cannotPreview));
            return null;
        }

        if (TEXT_ONLY_CATEGORIES.contains(category)) {
            runPreview(plugin, player, category, cosmetic, player.getLocation());
            return cosmeticsConfig.previewMenuCloseTicks;
        }

        // cosmetic.previewDurationTicks (por-ítem, YAML) manda sobre el
        // default global cuando está seteado (>0) - cada efecto anima
        // durante un tiempo distinto y el admin puede ajustarlo sin tocar Java.
        // Si hay una zona de previsualización configurada (/mincore
        // setpreviewzone), el efecto se reproduce sobre un NPC falso allá en
        // vez de encima del jugador donde sea que esté parado (arena de
        // minijuego, lobby...) - runSession() teletransporta, spawnea el NPC
        // y devuelve su ubicación; si no hay zona o ya está previsualizando,
        // no hace nada y se cae al comportamiento antiguo (in-place).
        int zoneDuration = cosmetic.previewDurationTicks > 0
                ? cosmetic.previewDurationTicks : cosmeticsConfig.previewZone.sessionDurationTicks;
        boolean started = plugin.getPreviewZoneManager().runSession(player, zoneDuration,
                npcLocation -> runPreview(plugin, player, category, cosmetic, npcLocation));
        if (started) return zoneDuration;

        runPreview(plugin, player, category, cosmetic, player.getLocation());
        return cosmetic.previewDurationTicks > 0 ? cosmetic.previewDurationTicks : cosmeticsConfig.previewMenuCloseTicks;
    }

    private static void runPreview(Mincore plugin, Player player, String category, CosmeticItem cosmetic, Location effectLocation) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        switch (category) {
            case "namecolors" -> previewChatLine(player, messages.cosmetics.previewNamecolorLine, cosmetic);
            case "chatcolors" -> previewChatLine(player, messages.cosmetics.previewChatcolorLine, cosmetic);
            case "join-messages" -> previewMessage(player, messages, cosmetic);
            case "kill-messages", "death-messages" ->
                    previewMessagePack(plugin, player, messages, category, (org.dqnylux.mincore.config.models.MessagePackCosmetic) cosmetic);
            case "trails" -> previewTrail(plugin, player, cosmetic);
            case "wings" -> previewWings(plugin, player, (org.dqnylux.mincore.config.models.WingCosmetic) cosmetic);
            case "projectile-effects" -> {
                player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.previewingEffect));
                // playProjectileEffect() necesita un proyectil real volando (lo llama
                // ProjectileLaunchEvent en CombatCosmeticsListener) - no un CosmeticEffect
                // simple como los demás, así que lanzamos una bola de nieve real e
                // "inyectamos" el cosmético directamente, sin pasar por PlayerData.
                plugin.getEffectRegistry().playProjectileEffect(plugin, player.launchProjectile(Snowball.class), cosmetic);
            }
            default -> {
                player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.previewingEffect));
                plugin.getEffectRegistry().playEffect(plugin, player, effectLocation, cosmetic);
            }
        }
    }

    private static void previewChatLine(Player player, String template, CosmeticItem cosmetic) {
        String color = cosmetic.value != null && !cosmetic.value.isBlank() ? cosmetic.value : "<#FFFFFF>";
        player.sendMessage(TextUtils.format(template
                .replace("%color%", color)
                .replace("%player%", player.getName())));
    }

    /**
     * "lines" (banner multi-línea, ej. con &lt;center&gt;) tiene prioridad
     * sobre "value" (una sola línea) cuando el cosmético define ambos - mismo
     * criterio que PlayerConnectionListener#broadcastJoinMessage al unirse de
     * verdad. Antes esto solo miraba "value" y si estaba vacío no mostraba
     * NADA - cualquier cosmético de entrada tipo banner quedaba sin preview.
     */
    private static void previewMessage(Player player, MessagesConfig messages, CosmeticItem cosmetic) {
        if (cosmetic.lines != null && !cosmetic.lines.isEmpty()) {
            player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.previewingMessage));
            for (String line : cosmetic.lines) {
                player.sendMessage(TextUtils.format(line.replace("%player%", player.getName())));
            }
            return;
        }

        if (cosmetic.value == null || cosmetic.value.isBlank()) return;
        player.sendMessage(TextUtils.format(messages.prefix + messages.cosmetics.previewingMessage));
        player.sendMessage(TextUtils.format(cosmetic.value.replace("%player%", player.getName())));
    }

    /**
     * kill_messages.yml/death_messages.yml no tienen un "value" plano como
     * join-messages - son un mapa causa->variante->líneas (MessagePackCosmetic).
     * Para la previsualización no hay un asesino/arma real, así que se toma
     * la variante "killer" (o "default" si no existe) de la causa "DEFAULT",
     * la misma resolución de respaldo que usa DeathListener al morir de verdad.
     * El header ahora es específico por categoría (antes reusaba el de
     * "mensaje de entrada" incluso para muertes/bajas, quedaba mal etiquetado).
     * %weapon% se reemplaza por un componente hoverable con el ítem REAL en
     * la mano del jugador - mismo mecanismo que el token [item] del chat
     * (ChatFormatHandler#applyItemToken) en vez de un texto fijo ("Espada").
     */
    private static void previewMessagePack(Mincore plugin, Player player, MessagesConfig messages, String category,
                                            org.dqnylux.mincore.config.models.MessagePackCosmetic cosmetic) {
        java.util.Map<String, java.util.List<String>> bucket = cosmetic.messages.get("DEFAULT");
        if (bucket == null || bucket.isEmpty()) return;

        java.util.List<String> lines = bucket.getOrDefault("killer", bucket.get("default"));
        if (lines == null || lines.isEmpty()) return;

        String template = lines.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(lines.size()));
        String header = category.equals("death-messages") ? messages.cosmetics.previewingDeathMessage : messages.cosmetics.previewingKillMessage;
        player.sendMessage(TextUtils.format(messages.prefix + header));

        Component message = TextUtils.format(player, template
                .replace("%player%", player.getName())
                .replace("%killer%", player.getName()));
        message = message.replaceText(builder -> builder.matchLiteral("%weapon%")
                .replacement((matchResult, componentBuilder) -> weaponPreviewComponent(plugin, player)));
        player.sendMessage(message);
    }

    private static Component weaponPreviewComponent(Mincore plugin, Player player) {
        var interactiveItem = plugin.getConfigManager().getChatFormatConfig().interactiveItem;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            return TextUtils.format(player, interactiveItem.emptyHand);
        }
        return Component.text("[")
                .append(Component.translatable(hand.translationKey()))
                .append(Component.text("]"))
                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                .hoverEvent(hand.asHoverEvent());
    }

    /** drawTrail() no tiene estado propio (lo llama ActiveCosmeticsTask cada tick del jugador equipado) - aquí se reutiliza igual, pero por un rato corto y sin tocar PlayerData. */
    private static final long PREVIEW_TRAIL_TICKS = 40L;

    private static void previewTrail(Mincore plugin, Player player, CosmeticItem cosmetic) {
        if (!WorldValidator.isAllowed(plugin, player.getWorld())) return;

        long[] elapsed = {0L};
        player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            plugin.getTrailManager().drawTrail(player, cosmetic, true, true);
            if (++elapsed[0] >= PREVIEW_TRAIL_TICKS) scheduled.cancel();
        }, () -> {}, 1L, 1L);
    }

    /**
     * WingManager.render() nunca pasa por EffectRegistry (wings.yml usa
     * particleLayout, no effectType) - sin este caso, el clic derecho en
     * cualquier ala caía en el default del switch, buscaba el effectType
     * (vacío) en cosmeticEffects y no encontraba nada: la previsualización
     * de wings no mostraba absolutamente nada. Se llama a render() en un
     * loop corto, igual que previewTrail(), siempre sobre la ubicación real
     * del jugador (WingManager no admite un Location aparte, las alas van
     * ancladas a los hombros de quien las lleva puestas).
     */
    private static void previewWings(Mincore plugin, Player player, org.dqnylux.mincore.config.models.WingCosmetic wings) {
        if (!WorldValidator.isAllowed(plugin, player.getWorld())) return;

        long[] elapsed = {0L};
        player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            // false = forma completa (renderResting) en vez de la estela
            // reducida de "moviéndose" - una previsualización quieta luce
            // mejor mostrando el ala entera, no solo un par de partículas.
            plugin.getWingManager().render(player, wings, false);
            if (++elapsed[0] >= PREVIEW_TRAIL_TICKS) scheduled.cancel();
        }, () -> {}, 1L, 1L);
    }

    /** Ítem "Nada" - siempre accesible, deja al jugador desequipar explícitamente sin tener que reencontrar el cosmético ya equipado. */
    private static GuiItem buildNoneItem(PaginatedGui gui, Mincore plugin, Player player, String category, MenuItem template) {
        return PaperItemBuilder.from(buildNoneDisplay(plugin, player, category, template))
                .asGuiItem(click -> {
                    MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
                    PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
                    String previousId = data == null ? null : data.getActiveCosmetic(category);
                    if (previousId == null) return;

                    CosmeticItem previous = itemsForCategory(plugin, category) != null ? itemsForCategory(plugin, category).get(previousId) : null;
                    String previousName = previous != null ? previous.displayName : previousId;

                    data.clearActiveCosmetic(category);
                    plugin.getPlayerManager().savePlayerAsync(data);
                    removeSideEffects(plugin, player, category);
                    player.sendMessage(TextUtils.format(messages.prefix
                            + messages.cosmetics.unequipped.replace("%cosmetic%", previousName)));
                    gui.updatePageItem(click.getSlot(), buildNoneDisplay(plugin, player, category, template));
                });
    }

    private static ItemStack buildNoneDisplay(Mincore plugin, Player viewer, String category, MenuItem template) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        List<Component> lore = new ArrayList<>();
        for (String line : template.lore) lore.add(TextUtils.format(line));

        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        boolean equipped = data == null || data.getActiveCosmetic(category) == null;
        for (String line : (equipped ? messages.cosmetics.statusEquipped : messages.cosmetics.statusClickToEquip)) {
            lore.add(TextUtils.format(line));
        }

        return PaperItemBuilder.from(parseMaterial(template.material))
                .name(TextUtils.format(template.displayName))
                .lore(lore)
                .flags(itemFlags(plugin))
                .build();
    }

    private static ItemStack buildDisplay(Mincore plugin, Player viewer, String category, String id, CosmeticItem cosmetic) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();

        List<Component> lore = new ArrayList<>();
        for (String line : cosmetic.lore) {
            lore.add(TextUtils.format(line));
        }

        if (cosmetic.price > 0) {
            lore.add(TextUtils.format(messages.cosmetics.priceLore.replace("%price%", String.valueOf(cosmetic.price))));
        }

        PlayerData data = plugin.getPlayerManager().get(viewer.getUniqueId());
        boolean equipped = isFormatsPseudoCategory(category)
                ? data != null && data.isFormatActive(formatsScopeOf(category), id)
                : data != null && id.equals(data.getActiveCosmetic(category));
        boolean accessible = isAccessible(plugin, viewer, category, id, cosmetic);
        for (String line : lockedStatus(messages, data, cosmetic, equipped, accessible, viewer)) {
            lore.add(TextUtils.format(line));
        }

        boolean previewEnabled = plugin.getConfigManager().getMainConfig().modules.cosmetics.previewEnabled;
        if (previewEnabled && !isFormatsPseudoCategory(category) && !VISUAL_ONLY_CATEGORIES.contains(category)) {
            lore.add(TextUtils.format(messages.cosmetics.previewHint));
        }

        // Los toggles de formats.yml muestran su estado también en el color
        // del NOMBRE (no solo en el lore) - más fácil de ver de un vistazo
        // cuáles de los 5 están prendidos en un menú con 30 colores al lado.
        // El color de estado se antepone al string CRUDO antes de parsear (no
        // se parsea primero y se llama .color() encima) porque formats.yml
        // trae su propio "<#FFFFFF>" al inicio de cada displayName - anteponer
        // como tag padre real dentro del mismo parseo de MiniMessage es lo
        // único que garantiza que gane sobre ese blanco fijo sin depender de
        // los detalles internos de cómo Adventure arma el árbol de Components.
        String rawName = cosmetic.displayName;
        String material = cosmetic.material;
        if (isFormatsPseudoCategory(category)) {
            String hex = equipped ? messages.cosmetics.formatActiveColor : messages.cosmetics.formatInactiveColor;
            if (hex == null || hex.isBlank()) hex = equipped ? "#5CE65C" : "#888888";
            rawName = "<" + hex + ">" + rawName;
            // El MATERIAL también refleja el estado (tinte verde prendido /
            // gris apagado) - el color del nombre solo no se distingue de un
            // vistazo entre 30 ítems de colores.
            String stateMaterial = equipped ? messages.cosmetics.formatActiveMaterial : messages.cosmetics.formatInactiveMaterial;
            if (stateMaterial != null && !stateMaterial.isBlank()) material = stateMaterial;
        } else if (!equipped && !accessible) {
            // Bloqueado (ni equipado ni comprable todavía): ícono gris
            // genérico en vez del material real del YAML - una vez
            // desbloqueado (isAccessible true) vuelve a mostrar el ítem
            // establecido tal cual, sin tocar nada más de esta rama.
            String locked = messages.cosmetics.lockedMaterial;
            if (locked != null && !locked.isBlank()) material = locked;
        }
        Component name = TextUtils.format(rawName);

        return PaperItemBuilder.from(parseMaterial(material))
                .name(name)
                .lore(lore)
                .flags(itemFlags(plugin))
                .build();
    }

    /** Lore dinámico: no basta un "Bloqueado" fijo - distingue entre "sin permiso", "aún te faltan monedas" y "ya puedes comprarlo". */
    public static List<String> lockedStatus(MessagesConfig messages, PlayerData data, CosmeticItem cosmetic, boolean equipped, boolean accessible, Player player) {
        if (equipped) return messages.cosmetics.statusEquipped;
        if (accessible) return messages.cosmetics.statusClickToEquip;

        // Sin el permiso extra no hay nada que "comprar" - se corta acá antes
        // de entrar a la lógica de precio, que asumiría precio=0 -> ya
        // comprable, mostrando un lore engañoso ("clic para comprar" sobre
        // algo gratis que en realidad está bloqueado por permiso).
        if (cosmetic.permission != null && !cosmetic.permission.isBlank() && !player.hasPermission(cosmetic.permission)) {
            return messages.cosmetics.statusLockedNoPermission;
        }

        double sucres = data == null ? 0 : data.getSucres();
        double missing = cosmetic.price - sucres;
        if (missing > 0) {
            return messages.cosmetics.statusLockedMissingCoins.stream()
                    .map(line -> line.replace("%missing%", String.valueOf(missing)))
                    .toList();
        }
        return messages.cosmetics.statusLockedCanBuy.stream()
                .map(line -> line.replace("%price%", String.valueOf(cosmetic.price)))
                .toList();
    }

    /**
     * Efectos que persisten fuera del propio render del chat/partículas y
     * deben aplicarse/revertirse al equipar/desequipar: glow (scoreboard),
     * prefijo/icono (nametag) y namecolor (tablist).
     *
     * prefixes/icons YA NO tocan LuckPerms (antes: LuckPermsHook.
     * setCustomPrefix/setCustomSuffix, un nodo de prioridad alta que
     * SUSTITUÍA el %luckperms_prefix%/%luckperms_suffix% real del rango
     * mientras el cosmético estuviera equipado). Eso rompía cualquier cosa
     * externa que leyera ese mismo placeholder para algo más que texto -
     * en particular, la línea de nombre del tablist de TAB Reborn, que en
     * la práctica también lo usa para el badge/posición del rango: equipar
     * un "tag" cosmético tapaba el prefijo real del jugador ahí, dando la
     * sensación de que "cambió de rango". Ahora el valor crudo del
     * cosmético se expone SOLO vía los placeholders propios de Mincore
     * (%coreec_prefix%/%coreec_icon%, PAPIExpansion) - nunca pisa el LP
     * real, así que %luckperms_prefix%/%luckperms_suffix% siempre reflejan
     * el rango real del jugador sin importar qué tenga equipado. El chat y
     * el nametag propios ya vienen wireados para mostrar ambos (ver
     * ChatFormatConfig/NametagConfig) - si además querés que el tag
     * aparezca en el tablist de TAB, se agrega %coreec_prefix%/%coreec_icon%
     * al formato de nombre de TAB (fuera de este repo, config del server).
     */
    private static void applySideEffects(Mincore plugin, Player player, String category, CosmeticItem cosmetic) {
        switch (category) {
            case "glows" -> {
                plugin.getGlowManager().applyGlow(player, cosmetic.value);
                plugin.getNametagDisplayManager().refresh(player);
            }
            case "prefixes", "icons" -> plugin.getNametagDisplayManager().refresh(player);
            case "namecolors" -> {
                plugin.getTabListManager().apply(player);
                plugin.getNametagDisplayManager().refresh(player);
            }
            default -> {
            }
        }
    }

    private static void removeSideEffects(Mincore plugin, Player player, String category) {
        switch (category) {
            case "glows" -> {
                plugin.getGlowManager().removeGlow(player);
                plugin.getNametagDisplayManager().refresh(player);
            }
            case "prefixes", "icons" -> plugin.getNametagDisplayManager().refresh(player);
            case "namecolors" -> {
                plugin.getTabListManager().apply(player);
                plugin.getNametagDisplayManager().refresh(player);
            }
            default -> {
            }
        }
    }

    public static boolean isAccessible(Mincore plugin, Player player, String category, String id, CosmeticItem cosmetic) {
        // Restricción DURA: si el cosmético pide un permiso explícito, sin
        // ese permiso no hay acceso sin importar precio/compra/desbloqueo -
        // usado por formats.yml para que "gratis" no signifique "cualquiera".
        if (cosmetic.permission != null && !cosmetic.permission.isBlank() && !player.hasPermission(cosmetic.permission)) {
            return false;
        }

        PlayerData data = plugin.getPlayerManager().get(player.getUniqueId());
        return cosmetic.price <= 0
                || player.hasPermission("coreec.cosmetic." + category + "." + id)
                || (data != null && data.hasCosmeticUnlocked(category, id));
    }

    /** Categorías que no viven en StandardCosmeticConfig (ver itemsForCategory) pero sí cuentan para el total global. */
    private static final String[] EXTRA_CATEGORIES = {"wings", "kill-messages", "death-messages"};

    /** Total de cosméticos desbloqueados por el jugador sumando TODAS las categorías - para %global_unlocked% en la cabeza de perfil. */
    public static long globalUnlocked(Mincore plugin, Player player) {
        long unlocked = 0;
        for (String category : allCategories()) {
            Map<String, CosmeticItem> items = itemsForCategory(plugin, category);
            if (items == null) continue;
            for (Map.Entry<String, CosmeticItem> entry : items.entrySet()) {
                if (isAccessible(plugin, player, category, entry.getKey(), entry.getValue())) unlocked++;
            }
        }
        return unlocked;
    }

    /** Total de cosméticos existentes sumando TODAS las categorías - para %global_total% en la cabeza de perfil. */
    public static long globalTotal(Mincore plugin) {
        long total = 0;
        for (String category : allCategories()) {
            Map<String, CosmeticItem> items = itemsForCategory(plugin, category);
            if (items != null) total += items.size();
        }
        return total;
    }

    private static List<String> allCategories() {
        List<String> all = new ArrayList<>(List.of(org.dqnylux.mincore.managers.cosmetics.CosmeticConfigManager.STANDARD_CATEGORIES));
        all.addAll(List.of(EXTRA_CATEGORIES));
        return all;
    }

    /** Vía XMaterial (XSeries) primero - resuelve alias históricos de material entre versiones, igual que EffectUtils. */
    public static Material parseMaterial(String name) {
        return EffectUtils.parseMaterial(name, Material.PAPER);
    }

    /** Vacío si modules.hideItemAttributes=false - deja ver la info extra que Minecraft agrega al ítem (atributos, irrompible...). */
    public static ItemFlag[] itemFlags(Mincore plugin) {
        return plugin.getConfigManager().getMainConfig().modules.hideItemAttributes ? ItemFlag.values() : new ItemFlag[0];
    }
}
