package br.com.marealta;

import br.com.marealta.web.App;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        new App().create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
