package com.trading.bdd.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * When tests run from an IDE or forked JVM, {@code DOCKER_HOST} is often unset even though Docker
 * Desktop is running. Testcontainers only reads {@code docker.host} from the environment or
 * {@code ~/.testcontainers.properties}, not from classpath {@code testcontainers.properties}.
 * <p>
 * Before any container starts, we probe common socket locations and persist {@code docker.host} into
 * the loaded {@link TestcontainersConfiguration} so {@link org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy}
 * can resolve the client (same mechanism as manually editing ~/.testcontainers.properties).
 */
public final class TestcontainersDockerBootstrap {

    private TestcontainersDockerBootstrap() {}

    public static void ensureDockerHostConfigured() {
        String envHost = System.getenv("DOCKER_HOST");
        if (envHost != null && !envHost.isBlank()) {
            return;
        }
        for (Path candidate : socketCandidates()) {
            try {
                if (Files.exists(candidate) && Files.isReadable(candidate)) {
                    String uri = "unix://" + candidate.toAbsolutePath();
                    TestcontainersConfiguration.getInstance().updateUserConfig("docker.host", uri);
                    return;
                }
            } catch (Exception ignored) {
                // continue probing
            }
        }
    }

    private static Path[] socketCandidates() {
        String home = System.getProperty("user.home");
        return new Path[] {
            Paths.get(home, ".docker", "run", "docker.sock"),
            Paths.get(home, "Library", "Containers", "com.docker.docker", "Data", "docker-cli.sock"),
            Paths.get("/var/run/docker.sock"),
        };
    }
}
