package br.com.marealta.web;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        App.create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
