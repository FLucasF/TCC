package com.pousadamaretalta;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String env = System.getenv("PORT");
        int port = (env == null || env.isBlank()) ? 7070 : Integer.parseInt(env.trim());
        App.create(new ReservationService()).start(port);
    }
}
