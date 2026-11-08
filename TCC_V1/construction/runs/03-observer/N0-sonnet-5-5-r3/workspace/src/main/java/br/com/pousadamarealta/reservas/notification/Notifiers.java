package br.com.pousadamarealta.reservas.notification;

import java.util.List;

/** Quem é avisado dos eventos das reservas. Novos interessados (restaurante, transfer...) entram aqui. */
public final class Notifiers {
    public static final String HOUSEKEEPING_ADDRESS = "governanca@pousadamarealta.com.br";
    public static final String FINANCE_ADDRESS = "financeiro@pousadamarealta.com.br";

    private Notifiers() {
    }

    public static List<Notifier> standard() {
        return List.of(
                new GuestNotifier(),
                new HousekeepingNotifier(HOUSEKEEPING_ADDRESS),
                new FinanceNotifier(FINANCE_ADDRESS));
    }
}
