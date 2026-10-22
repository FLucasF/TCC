package br.com.marealta.service;

import br.com.marealta.identity.DocumentRenderer;

/** E-mail de confirmação: sempre em português; muda só o documento que vai no meio. */
record ConfirmationEmail(String subject, String body) {
    static ConfirmationEmail of(ReservationService.Stay stay) {
        String body = "Olá, " + stay.reservation().guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + DocumentRenderer.render(stay.document()) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new ConfirmationEmail("Reserva confirmada - Pousada Maré Alta", body);
    }
}
