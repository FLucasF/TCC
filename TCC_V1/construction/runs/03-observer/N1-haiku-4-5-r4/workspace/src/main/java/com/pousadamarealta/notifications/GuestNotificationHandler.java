package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.utils.FormatterUtils;

public class GuestNotificationHandler implements NotificationHandler {

    @Override
    public void handleConfirmation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        String receipt = FormatterUtils.generateReceipt(reservation);
        String body = "Olá, " + reservation.guestName + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            receipt + "\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";

        messageRepo.create("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }

    @Override
    public void handleCancellation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        // Guests don't receive cancellation notifications
    }
}
