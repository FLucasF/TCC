package com.pousadamareaalta;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class TestAppRunner implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;
    private static Thread serverThread;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            context.getRoot().getStore(GLOBAL).put("test-app", this);
            startServer();
        }
    }

    private void startServer() {
        CountDownLatch latch = new CountDownLatch(1);

        serverThread = new Thread(() -> {
            try {
                System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/pousada_test");
                System.setProperty("DB_USER", "postgres");
                System.setProperty("DB_PASSWORD", "postgres");
                System.setProperty("PORT", "7070");

                latch.countDown();
                App.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        try {
            latch.await();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
}
