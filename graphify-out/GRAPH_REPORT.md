# Graph Report - .  (2026-07-12)

## Corpus Check
- Corpus is ~22,044 words - fits in a single context window. You may not need a graph.

## Summary
- 711 nodes · 1699 edges · 23 communities (21 shown, 2 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 175 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Listeners de Eventos
- Fuente y Centrado de Texto
- API de Efectos Cosméticos
- Configuración YAML
- Sincronización BD y Redis
- Comandos Principales
- Muerte y Mensajes Cosméticos
- Menús GUI y PlaceholderAPI
- Persistencia de Jugadores
- Rastreo GPS de Muertes
- Trails y Alas
- Config Servidor Dev
- Filtros de Chat
- Comandos Misc y Registro
- Utilidades de Texto
- Mensajería Privada
- Anuncios Programados
- Comandos Dinámicos
- Glow (Brillo)
- Economía Interna
- Hook LuckPerms
- Anti-Firma de Chat
- Paquete Raíz

## God Nodes (most connected - your core abstractions)
1. `Mincore` - 151 edges
2. `DefaultFontInfo` - 72 edges
3. `MincoreConfig` - 41 edges
4. `CosmeticItem` - 40 edges
5. `PlayerData` - 37 edges
6. `CoreConfigManager` - 24 edges
7. `DatabaseManager` - 23 edges
8. `CosmeticConfigManager` - 23 edges
9. `ChatFilterManager` - 20 edges
10. `PlayerManager` - 18 edges

## Surprising Connections (you probably didn't know these)
- `Mincore Plugin Descriptor (paper-plugin.yml)` --references--> `Mincore`  [EXTRACTED]
  src/main/resources/paper-plugin.yml → src/main/java/org/dqnylux/mincore/Mincore.java
- `Mincore Plugin Descriptor (paper-plugin.yml)` --conceptually_related_to--> `Purpur Server Configuration (purpur.yml)`  [INFERRED]
  src/main/resources/paper-plugin.yml → .ed-minecraft-dev/server/purpur.yml
- `Mincore Plugin Descriptor (paper-plugin.yml)` --references--> `MincoreLoader`  [EXTRACTED]
  src/main/resources/paper-plugin.yml → src/main/java/org/dqnylux/mincore/MincoreLoader.java
- `Bukkit Help Configuration (help.yml)` --conceptually_related_to--> `Bukkit Configuration (bukkit.yml)`  [INFERRED]
  .ed-minecraft-dev/server/help.yml → .ed-minecraft-dev/server/bukkit.yml
- `Spigot Server Configuration (spigot.yml)` --semantically_similar_to--> `Purpur Server Configuration (purpur.yml)`  [INFERRED] [semantically similar]
  .ed-minecraft-dev/server/spigot.yml → .ed-minecraft-dev/server/purpur.yml

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Local PurpurMC Development Server Environment for Mincore Plugin** — _ed_minecraft_dev_server_bukkit_config, _ed_minecraft_dev_server_config_paper_global_config, _ed_minecraft_dev_server_purpur_config, _ed_minecraft_dev_server_spigot_config, src_main_resources_paper_plugin_mincore [INFERRED 0.85]
- **Per-World Configuration Override Pattern** — _ed_minecraft_dev_server_config_paper_world_defaults_config, _ed_minecraft_dev_server_ed_dev_world_paper_world_config, _ed_minecraft_dev_server_ed_dev_world_nether_paper_world_config, _ed_minecraft_dev_server_ed_dev_world_the_end_paper_world_config [EXTRACTED 1.00]

## Communities (23 total, 2 thin omitted)

### Community 0 - "Listeners de Eventos"
Cohesion: 0.06
Nodes (35): Audience, JavaPlugin, Listener, PlayerChatTabCompleteEvent, PlayerJoinEvent, PlayerQuitEvent, ProjectileLaunchEvent, AutoResponderListener (+27 more)

### Community 1 - "Fuente y Centrado de Texto"
Cohesion: 0.03
Nodes (71): DefaultFontInfo, A, AMPERSAND, ASTERISK, AT_SYMBOL, B, BACK_SLASH, C (+63 more)

### Community 2 - "API de Efectos Cosméticos"
Cohesion: 0.07
Nodes (29): Entity, CosmeticItem, CosmeticEffect, Location, Player, ElytraEffect, Player, Projectile (+21 more)

### Community 3 - "Configuración YAML"
Cohesion: 0.08
Nodes (31): OkaeriConfig, AutoResponder, BotsConfig, CustomCommand, ChatFormatConfig, Mentions, Parts, DatabaseConfig (+23 more)

### Community 4 - "Sincronización BD y Redis"
Cohesion: 0.09
Nodes (14): HikariDataSource, JedisPool, ConfigSyncManager, Connection, ScheduledTask, CosmeticSyncManager, Connection, ScheduledTask (+6 more)

### Community 5 - "Comandos Principales"
Cohesion: 0.13
Nodes (12): ConsoleCommandSender, CosmeticsCommand, BukkitCommandActor, Command, Player, BukkitCommandActor, Command, CommandSender (+4 more)

### Community 6 - "Muerte y Mensajes Cosméticos"
Cohesion: 0.09
Nodes (13): DamageCause, PlayerRespawnEvent, MessagePackConfig, MessagePackCosmetic, StandardCosmeticConfig, WingsConfig, DeathListener, EventHandler (+5 more)

### Community 7 - "Menús GUI y PlaceholderAPI"
Cohesion: 0.09
Nodes (17): AbstractItem, Item, NotNull, PlaceholderExpansion, Override, Player, PAPIExpansion, CategoryMenu (+9 more)

### Community 8 - "Persistencia de Jugadores"
Cohesion: 0.10
Nodes (4): Player, Connection, PlayerManager, PlayerData

### Community 9 - "Rastreo GPS de Muertes"
Cohesion: 0.08
Nodes (14): BossBar, EnderCrystal, PluginCommand, BukkitCommandActor, Command, TrackCommand, Death, DeathTrackingManager (+6 more)

### Community 10 - "Trails y Alas"
Cohesion: 0.14
Nodes (11): WingCosmetic, WingSettings, Location, Particle, Player, TrailManager, Player, WingManager (+3 more)

### Community 11 - "Config Servidor Dev"
Cohesion: 0.11
Nodes (22): Command Aliases Moved to commands.yml Rationale, Bukkit Configuration (bukkit.yml), Bukkit Commands Configuration (commands.yml), Paper Global Configuration (paper-global.yml), Global vs Per-World Config Split Rationale, Paper World Defaults Configuration (paper-world-defaults.yml), Per-World Configuration Override Pattern, ed_dev_world_nether Config (paper-world.yml) (+14 more)

### Community 12 - "Filtros de Chat"
Cohesion: 0.15
Nodes (9): CancelReason, ADS, BAD_WORD, REPETITION, SPAM, ChatFilterManager, FilterResult, Pattern (+1 more)

### Community 13 - "Comandos Misc y Registro"
Cohesion: 0.20
Nodes (8): GameMode, Lamp, BukkitCommandActor, Command, MiscCommand, CommandManager, BukkitCommandActor, Commands

### Community 14 - "Utilidades de Texto"
Cohesion: 0.18
Nodes (7): MiniMessage, Plugin, getDefaultFontInfo(), Component, Pattern, Player, TextUtils

### Community 15 - "Mensajería Privada"
Cohesion: 0.20
Nodes (6): BukkitCommandActor, Command, CommandSender, Player, MessageCommand, ReplyManager

### Community 16 - "Anuncios Programados"
Cohesion: 0.23
Nodes (5): AnnouncementEntry, AnnouncementsConfig, Settings, AnnouncementManager, ScheduledTask

### Community 17 - "Comandos Dinámicos"
Cohesion: 0.20
Nodes (7): Command, CommandMap, DynamicCommand, CommandSender, Override, DynamicCommandManager, Command

### Community 18 - "Glow (Brillo)"
Cohesion: 0.38
Nodes (4): ChatColor, GlowManager, Player, Team

### Community 19 - "Economía Interna"
Cohesion: 0.31
Nodes (5): EconomyAdminHandler, Result, INVALID_AMOUNT, PLAYER_OFFLINE, SUCCESS

### Community 21 - "Anti-Firma de Chat"
Cohesion: 0.39
Nodes (5): PacketListenerAbstract, PacketReceiveEvent, PacketSendEvent, AntiSignatureListener, Override

## Knowledge Gaps
- **85 isolated node(s):** `org.dqnylux:Mincore`, `SQLITE`, `MYSQL`, `MARIADB`, `SUCCESS` (+80 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Mincore` connect `Listeners de Eventos` to `API de Efectos Cosméticos`, `Configuración YAML`, `Sincronización BD y Redis`, `Comandos Principales`, `Muerte y Mensajes Cosméticos`, `Menús GUI y PlaceholderAPI`, `Persistencia de Jugadores`, `Rastreo GPS de Muertes`, `Trails y Alas`, `Config Servidor Dev`, `Filtros de Chat`, `Comandos Misc y Registro`, `Mensajería Privada`, `Anuncios Programados`, `Comandos Dinámicos`, `Glow (Brillo)`, `Economía Interna`?**
  _High betweenness centrality (0.572) - this node is a cross-community bridge._
- **Why does `DefaultFontInfo` connect `Fuente y Centrado de Texto` to `Utilidades de Texto`?**
  _High betweenness centrality (0.187) - this node is a cross-community bridge._
- **Why does `Mincore Plugin Descriptor (paper-plugin.yml)` connect `Config Servidor Dev` to `Listeners de Eventos`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **What connects `org.dqnylux:Mincore`, `SQLITE`, `MYSQL` to the rest of the system?**
  _85 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Listeners de Eventos` be split into smaller, more focused modules?**
  _Cohesion score 0.05886708626434654 - nodes in this community are weakly interconnected._
- **Should `Fuente y Centrado de Texto` be split into smaller, more focused modules?**
  _Cohesion score 0.028169014084507043 - nodes in this community are weakly interconnected._
- **Should `API de Efectos Cosméticos` be split into smaller, more focused modules?**
  _Cohesion score 0.07344632768361582 - nodes in this community are weakly interconnected._