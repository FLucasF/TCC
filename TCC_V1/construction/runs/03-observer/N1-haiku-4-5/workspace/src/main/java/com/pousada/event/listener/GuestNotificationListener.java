package com.pousada.event.listener;

import com.pousada.database.MessageRepository;
import com.pousada.database.RoomRepository;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEventListener;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.service.FormattingService;
import java.util.Optional;

public class GuestNotificationListener implements ReservationEventListener {
  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String receipt = buildReceipt(reservation);

    String subject = "Reserva confirmada - Pousada Maré Alta";
    String body =
        "Olá, "
            + reservation.guestName
            + "!\n\n"
            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
            + receipt
            + "\n\n"
            + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
            + "Até breve!";

    MessageRepository.save("EMAIL", reservation.guestEmail, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
  }

  private String buildReceipt(Reservation reservation) {
    Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber);
    String roomType = room.isPresent() ? room.get().type : "UNKNOWN";

    String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
    String discountLine =
        reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0
            ? "Desconto: " + FormattingService.formatCurrency(reservation.discount) + "\n"
            : "";

    return "Comprovante de reserva nº "
        + reservation.id
        + "\n"
        + "Hóspede: "
        + reservation.guestName
        + " (CPF "
        + reservation.guestCpf
        + ")\n"
        + "Quarto: "
        + reservation.roomNumber
        + " ("
        + roomType
        + ")\n"
        + "Entrada: "
        + FormattingService.formatDate(reservation.checkIn)
        + " | Saída: "
        + FormattingService.formatDate(reservation.checkOut)
        + " | "
        + nightsLabel
        + "\n"
        + "Diárias: "
        + FormattingService.formatCurrency(reservation.subtotal)
        + "\n"
        + discountLine
        + "Total: "
        + FormattingService.formatCurrency(reservation.total);
  }
}
