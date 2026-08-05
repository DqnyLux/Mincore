package org.dqnylux.mincore;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Las versiones de aquí DEBEN coincidir con las del pom.xml (scope provided):
 * el pom solo compila, este loader es quien realmente pone los jars en el
 * classpath del servidor. El servidor cachea lo descargado en su carpeta
 * libraries/ y no vuelve a tocar la red en arranques posteriores — si ves
 * descargas en cada arranque es porque una resolución está fallando y nada
 * llega a cachearse.
 *
 * No usar https://repo.papermc.io/repository/maven-central/ (mirror
 * descontinuado por PaperMC) ni jitpack.io (exige token desde 2025): Lamp y
 * XSeries están publicados en Maven Central.
 */
@SuppressWarnings({"UnstableApiUsage"})
public class MincoreLoader implements PluginLoader {

    private static final String CENTRAL_MIRROR = "https://maven-central.storage-download.googleapis.com/maven2";

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        RepositoryPolicy releasesOnly = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN);

        resolver.addRepository(repo("central-mirror", CENTRAL_MIRROR, releasesOnly));
        resolver.addRepository(repo("okaeri", "https://repo.okaeri.cloud/releases/", releasesOnly));
        resolver.addRepository(repo("codemc", "https://repo.codemc.io/repository/maven-releases/", releasesOnly));
        // NOTA: pvphub-tofaa (repo de EntityLib) NO va acá - EntityLib se
        // INCRUSTA en el jar de Mincore vía maven-shade-plugin (ver pom.xml),
        // no se resuelve en runtime como el resto de esta lista. El repo solo
        // hace falta para compilar (pom.xml -> <repositories>).

        resolver.addDependency(new Dependency(new DefaultArtifact("eu.okaeri:okaeri-configs-core:6.1.0-beta.4"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.1.0-beta.4"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:7.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("redis.clients:jedis:7.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.cryptomorin:XSeries:13.7.1"), null));
        // triumph-gui-paper (reemplaza InvUI - ver el comentario en pom.xml).
        // Trae triumph-gui (core) transitivamente vía su propio POM.
        resolver.addDependency(new Dependency(new DefaultArtifact("dev.triumphteam:triumph-gui-paper:3.1.13"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("io.github.revxrsal:lamp.common:4.0.0-rc.17"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.xerial:sqlite-jdbc:3.53.2.0"), null));
        // EntityLib (Tofaa) NO va acá - se incrusta directo en el jar de
        // Mincore vía maven-shade-plugin (ver pom.xml), no se resuelve como
        // librería aparte en runtime. Idem PacketEvents de siempre: nunca se
        // descarga acá, se consume del plugin "packetevents" instalado en el
        // servidor (dependencia declarada en paper-plugin.yml) - el propio
        // Mincore JAMÁS toca WrapperPlayServerXxx/SpigotConversionUtil desde
        // una copia propia, siempre desde PacketEvents.getAPI().
        // Los drivers de MariaDB/MySQL TAMPOCO van acá - a diferencia de
        // sqlite-jdbc arriba, van incrustados en el jar (ver pom.xml,
        // <dependency> de mariadb-java-client) porque un intento anterior de
        // resolverlos acá vía red falló en un host de hosting compartido sin
        // ningún error visible en consola (el MavenLibraryResolver de Paper
        // puede fallar en silencio para UN artefacto puntual sin loguear nada
        // claro) - incrustado no depende de que el server pueda salir a
        // internet al arrancar.

        classpathBuilder.addLibrary(resolver);
    }

    private RemoteRepository repo(String id, String url, RepositoryPolicy releasePolicy) {
        return new RemoteRepository.Builder(id, "default", url)
                .setReleasePolicy(releasePolicy)
                .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .build();
    }
}
