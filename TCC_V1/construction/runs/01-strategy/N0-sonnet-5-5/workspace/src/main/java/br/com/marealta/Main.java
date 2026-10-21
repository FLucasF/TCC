package br.com.marealta;

import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        Api.create(new ReservationService()).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
