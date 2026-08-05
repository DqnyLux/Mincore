package org.dqnylux.mincore.managers.cosmetics;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.dqnylux.mincore.Mincore;
import org.dqnylux.mincore.config.MessagePackConfig;
import org.dqnylux.mincore.config.StandardCosmeticConfig;
import org.dqnylux.mincore.config.WingsConfig;
import org.dqnylux.mincore.config.models.CosmeticItem;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carga los 13 archivos "estándar" (un StandardCosmeticConfig reutilizado por
 * cada uno), los 2 paquetes de mensajes y wings.yml - los 16 catálogos viven
 * enteros como YAML embebido en src/main/resources/cosmetics/ (sección 51),
 * no como builders Java: a este volumen (~300 cosméticos) es la opción con
 * más fidelidad y menos riesgo de transcripción.
 *
 * Cada archivo es "plano" - el id del cosmético va directo en la raíz del
 * YAML (ej. "dragon_fuego:", igual que v1), sin envolver todo en una clave
 * "items:". Como okaeri (ConfigManager.create + withBindFile + load()) solo
 * sabe volcar el documento a un campo DECLARADO de la clase, un catálogo
 * plano no tiene ese campo para atarse - loadFlatCatalog() lo resuelve a
 * mano: parsea el YAML entero a un Map<String,Object> con el propio
 * Configurer de okaeri, y por cada clave de primer nivel usa
 * OkaeriConfig#get(key, Class) para materializar un CosmeticItem, con la
 * misma conversión de tipos/kebab-case que okaeri usaría en cualquier otro
 * lado - solo que sin la carpeta "items" de por medio.
 */
public class CosmeticConfigManager {

    public static final String[] STANDARD_CATEGORIES = {
            "namecolors", "chatcolors", "prefixes", "icons", "glows", "formats",
            "join-messages", "join-effects", "projectile-effects",
            "kill-effects", "death-effects", "elytra-effects", "trails"
    };

    private final Mincore plugin;
    private final Map<String, StandardCosmeticConfig> categories = new LinkedHashMap<>();
    private MessagePackConfig killMessages;
    private MessagePackConfig deathMessages;
    private WingsConfig wings;

    public CosmeticConfigManager(Mincore plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        for (String category : STANDARD_CATEGORIES) {
            categories.put(category, safeLoad(category, () -> loadStandard(category), new StandardCosmeticConfig()));
        }
        killMessages = safeLoad("kill-messages", () -> loadMessagePack("cosmetics/kill_messages.yml"), new MessagePackConfig());
        deathMessages = safeLoad("death-messages", () -> loadMessagePack("cosmetics/death_messages.yml"), new MessagePackConfig());
        wings = safeLoad("wings", this::loadWings, new WingsConfig());
    }

    /**
     * Red de seguridad final: cualquier error inesperado (no solo YAML mal
     * escrito, sino ej. permisos de disco) al cargar UNA categoría nunca debe
     * tumbar el arranque del servidor ni impedir que las otras 15 carguen -
     * se avisa por consola y esa categoría queda vacía hasta el próximo
     * "/mincore reload catalog" con el archivo corregido.
     */
    private <T> T safeLoad(String category, java.util.function.Supplier<T> loader, T fallback) {
        try {
            return loader.get();
        } catch (Exception e) {
            org.dqnylux.mincore.utils.ConsoleLogger.error(
                    "<#FF4C4C>No se pudo cargar la categoría <#FFFFFF>" + category + " <#FF4C4C>- quedará vacía hasta el próximo reload. Motivo: <#FFFFFF>" + errorMessage(e));
            return fallback;
        }
    }

    /**
     * ensureFromResource() solo copia el YAML embebido la PRIMERA vez que
     * arranca el servidor (si el archivo ya existe en el dataFolder, nunca lo
     * toca - así un admin no pierde ediciones propias en un reload normal).
     * Eso significa que, en desarrollo, editar el catálogo en
     * src/main/resources y reconstruir el jar NO se refleja en un servidor
     * de pruebas que ya arrancó una vez antes. Este método fuerza la
     * sobreescritura de los 16 YAML de cosméticos (los 13 estándar + wings +
     * kill/death messages) con el contenido actual del jar - lo llama
     * "/mincore reload catalog", nunca el arranque normal ni "/mincore
     * reload" a secas, para que en producción solo se dispare a propósito.
     */
    public void reloadFromResources() {
        for (String category : STANDARD_CATEGORIES) {
            forceFromResource("cosmetics/" + category.replace('-', '_') + ".yml");
        }
        forceFromResource("cosmetics/wings.yml");
        forceFromResource("cosmetics/kill_messages.yml");
        forceFromResource("cosmetics/death_messages.yml");
        loadConfigs();
    }

    private void forceFromResource(String fileName) {
        if (plugin.getResource(fileName) == null) return;
        plugin.saveResource(fileName, true);
    }

    private WingsConfig loadWings() {
        String fileName = "cosmetics/wings.yml";
        ensureFromResource(fileName);
        WingsConfig config = new WingsConfig();
        config.items = loadFlatCatalog(fileName, org.dqnylux.mincore.config.models.WingCosmetic.class);
        return config;
    }

    private MessagePackConfig loadMessagePack(String fileName) {
        ensureFromResource(fileName);
        MessagePackConfig config = new MessagePackConfig();
        config.items = loadFlatCatalog(fileName, org.dqnylux.mincore.config.models.MessagePackCosmetic.class);
        return config;
    }

    /**
     * Si el archivo no existe todavía en el dataFolder pero SÍ hay un recurso
     * embebido con ese mismo path en el JAR (src/main/resources/cosmetics/),
     * lo copia tal cual antes de que okaeri lo cargue. Si no hay recurso para
     * esa categoría y tampoco un archivo ya guardado, simplemente queda vacía
     * - el YAML es la única fuente de verdad del catálogo (sección 51/17).
     */
    private void ensureFromResource(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (file.exists()) return;
        if (plugin.getResource(fileName) == null) return;
        plugin.saveResource(fileName, false);
    }

    private StandardCosmeticConfig loadStandard(String category) {
        String fileName = "cosmetics/" + category.replace('-', '_') + ".yml";
        ensureFromResource(fileName);

        // withBindFile() (sin load()) deja el configurer/bindFile listos para
        // que CosmeticSyncManager pueda llamar config.save() más adelante
        // (sync de red) - items se llena a mano con loadFlatCatalog(), nunca
        // con el load() normal de okaeri (que exigiría un catálogo plano
        // dentro de un campo declarado, y este archivo no tiene ninguno).
        StandardCosmeticConfig config = ConfigManager.create(StandardCosmeticConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(plugin.getDataFolder(), fileName));
        });
        config.items = loadFlatCatalog(fileName, CosmeticItem.class);
        return config;
    }

    /** Documento vacío usado solo como resolvedor - OkaeriConfig#get(key, Class) hace la conversión real. */
    public static class FlatDocument extends OkaeriConfig {
    }

    /**
     * Lee un YAML "plano" (id de cosmético directo en la raíz, sin envolver
     * en "items:") y devuelve el catálogo tipado. WingCosmetic/
     * MessagePackCosmetic extienden CosmeticItem para poder tratarse como
     * cualquier otro cosmético en el resto del plugin (menús, permisos,
     * compras) - pero okaeri, al materializar un tipo fuera de un campo
     * declarado (nuestro caso, justamente para lograr el formato plano), NO
     * recorre los campos HEREDADOS de la superclase (confirmado: para
     * WingCosmetic, ConfigDeclaration.of() solo ve "wings", ninguno de los
     * ~20 campos de CosmeticItem) - por eso, si itemClass no es CosmeticItem
     * directamente, se resuelve el ítem DOS veces (una como itemClass, para
     * los campos propios como "wings"/"messages"; otra como CosmeticItem
     * puro, para material/displayName/price/etc.) y se copian los campos de
     * la segunda sobre la primera.
     */
    private <T extends CosmeticItem> Map<String, T> loadFlatCatalog(String fileName, Class<T> itemClass) {
        Map<String, T> result = new LinkedHashMap<>();
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return result;

        FlatDocument doc = ConfigManager.create(FlatDocument.class, it -> it.withConfigurer(new YamlSnakeYamlConfigurer()));
        Configurer configurer = doc.getConfigurer();

        // Un YAML con la indentación rota, comillas sin cerrar, etc. NUNCA
        // debe tumbar el servidor - se avisa por consola con el motivo
        // exacto (SnakeYAML incluye línea/columna en el mensaje) y esa
        // categoría queda vacía en vez de crashear el arranque/reload entero.
        Map<String, Object> raw;
        try (FileInputStream in = new FileInputStream(file)) {
            raw = configurer.load(in, doc.getDeclaration());
        } catch (Exception e) {
            org.dqnylux.mincore.utils.ConsoleLogger.error(
                    "<#FF4C4C>No se pudo leer <#FFFFFF>" + fileName + " <#FF4C4C>- la categoría quedará vacía hasta que se corrija. Motivo: <#FFFFFF>" + errorMessage(e));
            return result;
        }
        doc.load(raw);

        // Un ítem individual con un campo del tipo equivocado (ej. price: "abc")
        // tampoco debe tumbar el archivo completo - se omite SOLO ese ítem, se
        // avisa cuál y por qué, y el resto del catálogo carga normal.
        for (String id : raw.keySet()) {
            try {
                T item = doc.get(id, itemClass);
                if (itemClass != CosmeticItem.class) {
                    copyInheritedFields(doc.get(id, CosmeticItem.class), item);
                }
                result.put(id, item);
            } catch (Exception e) {
                org.dqnylux.mincore.utils.ConsoleLogger.error(
                        "<#FF4C4C>El cosmético <#FFFFFF>'" + id + "' <#FF4C4C>en <#FFFFFF>" + fileName + " <#FF4C4C>no se pudo cargar y se omitió. Motivo: <#FFFFFF>" + errorMessage(e));
            }
        }
        return result;
    }

    /** okaeri ya envuelve el error de más bajo nivel con contexto útil (ej. qué campo falló) - ese mensaje externo es el más informativo, no la causa raíz. */
    private static String errorMessage(Throwable error) {
        String message = error.getMessage();
        return message != null ? message : error.toString();
    }

    private static void copyInheritedFields(CosmeticItem from, CosmeticItem to) {
        for (Field field : CosmeticItem.class.getFields()) {
            try {
                field.set(to, field.get(from));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Guarda una categoría estándar de vuelta a su YAML plano (sin "items:") -
     * usado por CosmeticSyncManager.pullFromDatabase() tras actualizar
     * precios/nombres desde la red. NUNCA usar config.save() directo acá: al
     * no tener "items" como campo declarado (se llenó a mano con
     * loadFlatCatalog), OkaeriConfig#save() serializa por DECLARACIÓN y
     * termina escribiendo el archivo entero envuelto en "items:" - exactamente
     * el formato verboso que loadFlatCatalog existe para evitar. Bug real
     * encontrado en vivo: como este método solo corre con sync de red activo
     * (MySQL/MariaDB), nunca se había disparado hasta que la conexión a la
     * base de datos empezó a funcionar.
     */
    public void saveStandardCategory(String category) {
        StandardCosmeticConfig config = categories.get(category);
        if (config == null) return;
        saveFlatCatalog("cosmetics/" + category.replace('-', '_') + ".yml", config.items);
    }

    private <T extends CosmeticItem> void saveFlatCatalog(String fileName, Map<String, T> items) {
        File file = new File(plugin.getDataFolder(), fileName);
        FlatDocument doc = ConfigManager.create(FlatDocument.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(file);
        });
        for (Map.Entry<String, T> entry : items.entrySet()) {
            doc.set(entry.getKey(), entry.getValue());
        }
        doc.save();
    }

    public StandardCosmeticConfig getCategory(String category) {
        return categories.get(category);
    }

    public CosmeticItem getItem(String category, String itemId) {
        // "wings"/"kill-messages"/"death-messages" no viven en "categories" -
        // son tipos aparte (WingsConfig/MessagePackConfig) con campos extra,
        // pero WingCosmetic/MessagePackCosmetic ambos extends CosmeticItem,
        // así que devolverlos acá es válido.
        return switch (category) {
            case "wings" -> wings.items.get(itemId);
            case "kill-messages" -> killMessages.items.get(itemId);
            case "death-messages" -> deathMessages.items.get(itemId);
            default -> {
                StandardCosmeticConfig config = categories.get(category);
                yield config == null ? null : config.items.get(itemId);
            }
        };
    }

    public Map<String, StandardCosmeticConfig> getCategories() {
        return categories;
    }

    public MessagePackConfig getKillMessages() {
        return killMessages;
    }

    public MessagePackConfig getDeathMessages() {
        return deathMessages;
    }

    public WingsConfig getWings() {
        return wings;
    }
}
