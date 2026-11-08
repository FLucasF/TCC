package com.pousada.service;

import com.pousada.model.Document;
import com.pousada.model.Message;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.rateplan.RatePlanFactory;
import com.pousada.rateplan.RatePlanStrategy;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
  private final ReservationRepository reservationRepository;
  private final RoomRepository roomRepository;
  private final MessageRepository messageRepository;

  public ReservationService(
      ReservationRepository reservationRepository, RoomRepository roomRepository, MessageRepository messageRepository) {
    this.reservationRepository = reservationRepository;
    this.roomRepository = roomRepository;
    this.messageRepository = messageRepository;
  }

  public Reservation createReservation(
      int roomNumber,
      String guestName,
      String guestEmail,
      String guestCpf,
      LocalDate checkIn,
      LocalDate checkOut,
      int guests,
      Reservation.RatePlan ratePlan,
      String companyCode)
      throws RatePlanStrategy.InvalidReservationException {
    Optional<Room> room = roomRepository.findById(roomNumber);
    if (room.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Room not found", "ROOM_NOT_FOUND");
    }

    Room r = room.get();

    if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
      throw new RatePlanStrategy.InvalidReservationException("Invalid dates", "INVALID_REQUEST");
    }

    if (!reservationRepository.findByRoomAndDates(roomNumber, checkIn, checkOut).isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Room unavailable for these dates", "ROOM_UNAVAILABLE");
    }

    RatePlanStrategy strategy = RatePlanFactory.create(ratePlan);
    strategy.validate(r, checkIn, checkOut, guests, companyCode);

    RatePlanStrategy.PriceCalculation pricing;
    if (ratePlan == Reservation.RatePlan.CORPORATE) {
      pricing = strategy.calculatePrice(r, checkIn, checkOut, companyCode);
    } else {
      pricing = strategy.calculatePrice(r, checkIn, checkOut);
    }

    Reservation reservation = new Reservation();
    reservation.setRoomNumber(roomNumber);
    reservation.setGuestName(guestName);
    reservation.setGuestEmail(guestEmail);
    reservation.setGuestCpf(guestCpf);
    reservation.setCheckIn(checkIn);
    reservation.setCheckOut(checkOut);
    reservation.setGuests(guests);
    reservation.setRatePlan(ratePlan);
    reservation.setCompanyCode(companyCode);
    reservation.setNights(pricing.nights);
    reservation.setSubtotal(pricing.subtotal);
    reservation.setDiscount(pricing.discount);
    reservation.setTotal(pricing.total);
    reservation.setStatus(Reservation.Status.PENDING);
    reservation.setAmountPaid(BigDecimal.ZERO);
    reservation.setRefund(null);

    return reservationRepository.save(reservation);
  }

  public Reservation confirmReservation(long reservationId, BigDecimal amountPaid)
      throws RatePlanStrategy.InvalidReservationException {
    Optional<Reservation> reservation = reservationRepository.findById(reservationId);
    if (reservation.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Reservation not found", "RESERVATION_NOT_FOUND");
    }

    Reservation r = reservation.get();
    if (r.getStatus() != Reservation.Status.PENDING) {
      throw new RatePlanStrategy.InvalidReservationException(
          "Operation not allowed for this reservation status", "OPERATION_NOT_ALLOWED");
    }

    r.setStatus(Reservation.Status.CONFIRMED);
    r.setAmountPaid(amountPaid);
    reservationRepository.update(r);

    sendConfirmationEmail(r);

    return r;
  }

  private void sendConfirmationEmail(Reservation reservation) {
    String subject = "Reserva confirmada - Pousada Maré Alta";
    String document = generateDocument(reservation);

    String body =
        String.format(
            "Olá, %s!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + "%s\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!",
            reservation.getGuestName(), document);

    Message message = new Message(Message.Channel.EMAIL, reservation.getGuestEmail(), subject, body);
    messageRepository.save(message);
  }

  private String generateDocument(Reservation reservation) {
    Document doc =
        new Document(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            getRoomType(reservation.getRoomNumber()),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal());

    return formatDocument(doc);
  }

  public Document getDocument(long reservationId) throws RatePlanStrategy.InvalidReservationException {
    Optional<Reservation> reservation = reservationRepository.findById(reservationId);
    if (reservation.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Reservation not found", "RESERVATION_NOT_FOUND");
    }

    Reservation r = reservation.get();
    if (r.getStatus() == Reservation.Status.PENDING || r.getStatus() == Reservation.Status.CANCELLED) {
      throw new RatePlanStrategy.InvalidReservationException(
          "Document not available for this reservation", "OPERATION_NOT_ALLOWED");
    }

    return new Document(
        r.getId(),
        r.getGuestName(),
        r.getGuestCpf(),
        r.getRoomNumber(),
        getRoomType(r.getRoomNumber()),
        r.getCheckIn(),
        r.getCheckOut(),
        r.getNights(),
        r.getSubtotal(),
        r.getDiscount(),
        r.getTotal());
  }

  public Reservation checkIn(long reservationId) throws RatePlanStrategy.InvalidReservationException {
    Optional<Reservation> reservation = reservationRepository.findById(reservationId);
    if (reservation.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Reservation not found", "RESERVATION_NOT_FOUND");
    }

    Reservation r = reservation.get();
    if (r.getStatus() != Reservation.Status.CONFIRMED) {
      throw new RatePlanStrategy.InvalidReservationException(
          "Operation not allowed for this reservation status", "OPERATION_NOT_ALLOWED");
    }

    r.setStatus(Reservation.Status.CHECKED_IN);
    reservationRepository.update(r);

    return r;
  }

  public Document checkOut(long reservationId) throws RatePlanStrategy.InvalidReservationException {
    Optional<Reservation> reservation = reservationRepository.findById(reservationId);
    if (reservation.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Reservation not found", "RESERVATION_NOT_FOUND");
    }

    Reservation r = reservation.get();
    if (r.getStatus() != Reservation.Status.CHECKED_IN) {
      throw new RatePlanStrategy.InvalidReservationException(
          "Operation not allowed for this reservation status", "OPERATION_NOT_ALLOWED");
    }

    r.setStatus(Reservation.Status.CHECKED_OUT);
    reservationRepository.update(r);

    return new Document(
        r.getId(),
        r.getGuestName(),
        r.getGuestCpf(),
        r.getRoomNumber(),
        getRoomType(r.getRoomNumber()),
        r.getCheckIn(),
        r.getCheckOut(),
        r.getNights(),
        r.getSubtotal(),
        r.getDiscount(),
        r.getTotal());
  }

  public Reservation cancel(long reservationId) throws RatePlanStrategy.InvalidReservationException {
    Optional<Reservation> reservation = reservationRepository.findById(reservationId);
    if (reservation.isEmpty()) {
      throw new RatePlanStrategy.InvalidReservationException("Reservation not found", "RESERVATION_NOT_FOUND");
    }

    Reservation r = reservation.get();
    if (r.getStatus() != Reservation.Status.PENDING && r.getStatus() != Reservation.Status.CONFIRMED) {
      throw new RatePlanStrategy.InvalidReservationException(
          "Operation not allowed for this reservation status", "OPERATION_NOT_ALLOWED");
    }

    RatePlanStrategy strategy = RatePlanFactory.create(r.getRatePlan());
    BigDecimal refund = strategy.calculateRefund(r.getAmountPaid());

    r.setStatus(Reservation.Status.CANCELLED);
    r.setRefund(refund);
    reservationRepository.update(r);

    return r;
  }

  private String getRoomType(int roomNumber) {
    Optional<Room> room = roomRepository.findById(roomNumber);
    return room.map(r -> r.getType().toString()).orElse("UNKNOWN");
  }

  private String formatDocument(Document doc) {
    StringBuilder sb = new StringBuilder();
    sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
    sb.append("Hóspede: ").append(doc.getGuestName()).append(" (CPF ").append(formatCpf(doc.getGuestCpf()))
        .append(")\n");
    sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
    sb.append("Entrada: ").append(formatDate(doc.getCheckIn())).append(" | Saída: ")
        .append(formatDate(doc.getCheckOut())).append(" | ").append(doc.getNights())
        .append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
    sb.append("Diárias: ").append(formatMoney(doc.getSubtotal())).append("\n");
    if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
      sb.append("Desconto: ").append(formatMoney(doc.getDiscount())).append("\n");
    }
    sb.append("Total: ").append(formatMoney(doc.getTotal())).append("\n");

    return sb.toString();
  }

  private String formatDate(LocalDate date) {
    return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
  }

  private String formatMoney(BigDecimal amount) {
    return String.format("R$ %,.2f", amount).replace(",", ".");
  }

  private String formatCpf(String cpf) {
    if (cpf == null || cpf.length() < 11) return cpf;
    return String.format("%s.%s.%s-%s", cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9),
        cpf.substring(9));
  }
}
