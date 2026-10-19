package com.marealta;

public class Main {
    public static void main(String[] args) {
        String port = System.getenv("PORT");
        new App().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
