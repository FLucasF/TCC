package br.com.marealta;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        int p = port == null || port.isBlank() ? 7070 : Integer.parseInt(port.strip());
        Outbox outbox = new Outbox();
        new Api(new ReservationService(outbox), outbox).create().start(p);
    }
}
