package br.com.pousadamarealta;

import java.time.Clock;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv(System::getenv);
        String port = System.getenv("PORT");
        int p = port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
        App.create(config, Clock.systemDefaultZone()).start(p);
        if (config.testMode()) {
            System.out.println("MODO DE TESTE: e-mails serão redirecionados para " + AppConfig.TEST_ADDRESS);
        }
    }
}
