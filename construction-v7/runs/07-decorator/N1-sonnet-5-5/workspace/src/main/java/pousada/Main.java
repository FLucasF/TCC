package pousada;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        String port = System.getenv("PORT");
        HotelApp.create(testMode).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
