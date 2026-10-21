package br.com.pousadamarealta.reservas;

import br.com.pousadamarealta.reservas.web.App;
import br.com.pousadamarealta.reservas.web.Config;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Config config = Config.fromEnvironment();
        App.create(config).start(config.port());
    }
}
