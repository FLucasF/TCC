package br.com.marealta;

public final class Main {
    public static void main(String[] args) {
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        String portEnv = System.getenv("PORT");
        int port = (portEnv == null || portEnv.isBlank()) ? 7070 : Integer.parseInt(portEnv.trim());
        new App(testMode).build().start(port);
    }
}
