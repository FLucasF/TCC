package com.pousadamarealta.services;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.infrastructure.MessageRepository;
import com.pousadamarealta.infrastructure.RoomRepository;
import java.time.format.DateTimeFormatter;

public class HostelNotificationListener implements ReservationEventListener {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Override
  public void onReservationConfirmed(ReservationEvent event) throws Exception {
    Reservation res = event.reservation();
    var room = RoomRepository.findById(res.roomNumber).orElseThrow();

    String receipt = buildReceipt(res, room);

    // Email to guest
    String guestEmailBody = buildGuestConfirmationEmail(res, receipt);
    MessageRepository.insert(
        MessageChannel.EMAIL,
        res.guestEmail,
        "Reserva confirmada - Pousada Maré Alta",
        guestEmailBody);

    // Email to housekeeping
    String housekeepingEmailBody =
        String.format(
            "Nova reserva confirmada:%n%n"
                + "Quarto: %d%n"
                + "Entrada: %s%n"
                + "Saída: %s%n"
                + "Hóspedes: %d",
            res.roomNumber,
            res.checkIn.format(DATE_FORMATTER),
            res.checkOut.format(DATE_FORMATTER),
            res.guests);
    MessageRepository.insert(
        MessageChannel.EMAIL,
        "governanca@pousadamarealta.com.br",
        "Reserva confirmada - quarto " + res.roomNumber,
        housekeepingEmailBody);

    // Email to accounting
    String accountingEmailBody =
        String.format(
            "Pagamento recebido:%n%n"
                + "Reserva nº %d%n"
                + "Valor: R$ %.2f",
            res.id, res.total);
    MessageRepository.insert(
        MessageChannel.EMAIL,
        "financeiro@pousadamarealta.com.br",
        "Pagamento recebido - reserva " + res.id,
        accountingEmailBody);
  }

  @Override
  public void onReservationCancelled(ReservationEvent event) throws Exception {
    Reservation current = event.reservation();
    Reservation previous = event.previousReservation();

    if (previous == null || previous.status.equals(com.pousadamarealta.domain.ReservationStatus.PENDING)) {
      return;
    }

    // Email to housekeeping
    String housekeepingEmailBody =
        String.format(
            "Reserva cancelada:%n%n"
                + "Quarto: %d%n"
                + "Entrada: %s%n"
                + "Saída: %s",
            current.roomNumber,
            current.checkIn.format(DATE_FORMATTER),
            current.checkOut.format(DATE_FORMATTER));
    MessageRepository.insert(
        MessageChannel.EMAIL,
        "governanca@pousadamarealta.com.br",
        "Reserva cancelada - quarto " + current.roomNumber,
        housekeepingEmailBody);

    // Email to accounting only if there's a refund
    if (current.refund != null && current.refund.signum() > 0) {
      String accountingEmailBody =
          String.format(
              "Reembolso para reserva cancelada:%n%n"
                  + "Reserva nº %d%n"
                  + "Valor: R$ %.2f",
              current.id, current.refund);
      MessageRepository.insert(
          MessageChannel.EMAIL,
          "financeiro@pousadamarealta.com.br",
          "Reembolso - reserva " + current.id,
          accountingEmailBody);
    }
  }

  @Override
  public void onCheckedIn(ReservationEvent event) throws Exception {}

  @Override
  public void onCheckedOut(ReservationEvent event) throws Exception {}

  private String buildReceipt(Reservation res, com.pousadamarealta.domain.Room room) {
    StringBuilder receipt = new StringBuilder();
    receipt.append("Comprovante de reserva nº ").append(res.id).append("\n");
    receipt.append("Hóspede: ").append(res.guestName).append(" (CPF ")
        .append(formatCpf(res.guestCpf)).append(")\n");
    receipt.append("Quarto: ").append(res.roomNumber).append(" (").append(room.type.value).append(")\n");

    String nightsLabel = res.nights == 1 ? "1 diária" : res.nights + " diárias";
    receipt.append("Entrada: ").append(res.checkIn.format(DATE_FORMATTER)).append(" | ");
    receipt.append("Saída: ").append(res.checkOut.format(DATE_FORMATTER)).append(" | ");
    receipt.append(nightsLabel).append("\n");

    receipt.append("Diárias: R$ ").append(formatMoney(res.subtotal)).append("\n");

    if (res.discount.signum() > 0) {
      receipt.append("Desconto: R$ ").append(formatMoney(res.discount)).append("\n");
    }

    receipt.append("Total: R$ ").append(formatMoney(res.total));

    return receipt.toString();
  }

  private String buildGuestConfirmationEmail(Reservation res, String receipt) {
    return String.format(
        "Olá, %s!%n%n"
            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:%n%n"
            + "%s%n%n"
            + "A entrada é a partir das 14h, e a saída, até as 12h.%n"
            + "Até breve!",
        res.guestName, receipt);
  }

  private String formatCpf(String cpf) {
    if (cpf.length() != 11) return cpf;
    return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-"
        + cpf.substring(9);
  }

  private String formatMoney(java.math.BigDecimal value) {
    return String.format("%.2f", value);
  }
}
