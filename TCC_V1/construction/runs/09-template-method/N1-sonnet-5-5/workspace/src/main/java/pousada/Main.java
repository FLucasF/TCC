package pousada;

import pousada.web.Api;

public class Main {
    public static void main(String[] args) {
        String port = System.getenv("PORT");
        Api.create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
