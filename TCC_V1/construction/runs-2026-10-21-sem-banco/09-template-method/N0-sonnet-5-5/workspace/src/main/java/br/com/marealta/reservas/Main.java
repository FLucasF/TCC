package br.com.marealta.reservas;

import br.com.marealta.reservas.web.App;

import java.math.BigDecimal;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        String tax = System.getenv("TOURISM_TAX_PER_GUEST_NIGHT");
        new App(tax == null || tax.isBlank() ? App.DEFAULT_TOURISM_TAX : new BigDecimal(tax))
                .create()
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
