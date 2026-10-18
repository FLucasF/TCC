package reference.suite;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * The agent's service as a process: start its jar, wait until it answers, stop it and restart it.
 *
 * Restarting is how the suite checks persistence ("if the service restarts, nothing can be lost"): the
 * suite stops the process, starts the same jar again against the same database, and compares.
 */
public final class AgentService {

    private final File jar;
    private final Map<String, String> environment;
    private final File log;
    private final int port;
    private Process process;

    public AgentService(File jar, int port, Map<String, String> environment, File log) {
        this.jar = jar;
        this.port = port;
        this.environment = environment;
        this.log = log;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    /** Starts the jar and waits until it answers HTTP. Returns false if it does not come up in time. */
    public boolean start(Duration timeout) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", jar.getAbsolutePath());
        builder.environment().putAll(environment);
        // the prompt asks for PORT (Javalin, since 18/10/2026); SERVER_PORT is what a Spring Boot service of the older
        // batches reads
        builder.environment().put("PORT", String.valueOf(port));
        builder.environment().put("SERVER_PORT", String.valueOf(port));
        builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        process = builder.start();
        return waitUntilAnswering(timeout);
    }

    public void stop() throws InterruptedException {
        if (process == null) return;
        process.destroy();                                   // a polite stop first, like a real restart
        if (!process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly().waitFor();
        process = null;
    }

    public boolean restart(Duration timeout) throws Exception {
        stop();
        return start(timeout);
    }

    /** "Up" means it answers any HTTP response on a known route; the content is checked by the suite. */
    private boolean waitUntilAnswering(Duration timeout) throws InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest probe = HttpRequest.newBuilder(URI.create(baseUrl() + "/products/CAM-001")).timeout(Duration.ofSeconds(2)).build();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) return false;            // it crashed while starting
            try {
                client.send(probe, HttpResponse.BodyHandlers.discarding());
                return true;
            } catch (Exception notYet) {
                Thread.sleep(500);
            }
        }
        return false;
    }
}
