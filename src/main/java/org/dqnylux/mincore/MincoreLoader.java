package org.dqnylux.mincore;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings({"UnstableApiUsage"})
public class MincoreLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.papermc.io/repository/maven-central/").build());
        resolver.addRepository(new RemoteRepository.Builder("okaeri", "default", "https://repo.okaeri.cloud/releases/").build());
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/").build());
        resolver.addRepository(new RemoteRepository.Builder("jitpack", "default", "https://jitpack.io/").build());
        resolver.addRepository(new RemoteRepository.Builder("codemc", "default", "https://repo.codemc.io/repository/maven-releases/").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("eu.okaeri:okaeri-configs-core:6.1.0-beta.4"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.1.0-beta.4"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:5.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("redis.clients:jedis:5.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.apache.commons:commons-pool2:2.12.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.cryptomorin:XSeries:11.2.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("xyz.xenondevs.invui:invui:pom:2.2.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("io.github.revxrsal:lamp.common:4.0.0-rc.17"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("io.github.revxrsal:lamp.paper:4.0.0-beta.19"), null));

        classpathBuilder.addLibrary(resolver);
    }
}