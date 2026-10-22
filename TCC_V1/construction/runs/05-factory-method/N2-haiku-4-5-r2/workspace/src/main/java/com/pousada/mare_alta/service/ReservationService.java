package com.pousada.mare_alta.service;

import com.pousada.mare_alta.document.DocumentData;
import com.pousada.mare_alta.document.DocumentFormatter;
import com.pousada.mare_alta.document.DocumentFormatterFactory;
import com.pousada.mare_alta.model.Document;
import com.pousada.mare_alta.model.Message;
import com.pousada.mare_alta.model.Reservation;
import com.pousada.mare_alta.model.ReservationStatus;
import com.pousada.mare_alta.model.Room;
import com.pousada.mare_alta.model.RatePlan;
import com.pousada.mare_alta.repository.MessageRepository;
import com.pousada.mare_alta.repository.ReservationRepository;
import com.pousada.mare_alta.repository.RoomRepository;
import com.pousada.mare_alta.util.DateUtil;
import com.pousada.mare_alta.util.NumberFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation createReservation(
            int roomNumber, String guestName, String guestEmail,
            String guestCpf, String companyCnpj, String companyName,
            String guestPassport, String guestCountry,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) throws Exception {

        // Validate inputs
        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        // Validate identification
        int identificationCount = (guestCpf != null ? 1 : 0) +
                                  (companyCnpj != null ? 1 : 0) +
                                  (guestPassport != null ? 1 : 0);
        if (identificationCount != 1) {
            throw new IllegalArgumentException("Exactly one identification type is required");
        }

        // Check room exists and capacity
        Room room = roomRepository.findById(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        // Check room availability
        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room is not available for selected dates");
        }

        // Calculate prices
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = subtotal.subtract(discount);

        // Create reservation
        Reservation reservation = new Reservation(
                0, roomNumber, guestName, guestEmail,
                guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total,
                ReservationStatus.PENDING, BigDecimal.ZERO, null);

        return reservationRepository.create(reservation);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be confirmed");
        }

        Reservation confirmed = reservation
                .withStatus(ReservationStatus.CONFIRMED)
                .withAmountPaidAndRefund(amountPaid, null);

        reservationRepository.update(confirmed);

        // Send confirmation email
        sendConfirmationEmail(confirmed);

        return confirmed;
    }

    public Reservation cancelReservation(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel this reservation");
        }

        BigDecimal refund = null;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            // STANDARD: full refund, NON_REFUNDABLE: no refund
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            } else {
                refund = BigDecimal.ZERO;
            }
        }

        Reservation cancelled = reservation
                .withStatus(ReservationStatus.CANCELLED)
                .withAmountPaidAndRefund(reservation.getAmountPaid(), refund);

        reservationRepository.update(cancelled);
        return cancelled;
    }

    public Reservation checkIn(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed reservations can check in");
        }

        Reservation checkedIn = reservation.withStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(checkedIn);
        return checkedIn;
    }

    public Document checkOut(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Only checked-in reservations can check out");
        }

        Reservation checkedOut = reservation.withStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(checkedOut);

        return generateDocument(checkedOut);
    }

    public Document getDocument(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Document is only available for paid reservations");
        }

        return generateDocument(reservation);
    }

    public Reservation getReservation(long id) throws Exception {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    private boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        return conflicts.isEmpty();
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            BigDecimal multiplier = DateUtil.getSeasonMultiplier(current);
            BigDecimal nightPrice = nightlyRate.multiply(multiplier);
            nightPrice = NumberFormatter.round(nightPrice);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return NumberFormatter.round(subtotal);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"));
            return NumberFormatter.round(discount);
        }
        return BigDecimal.ZERO;
    }

    private Document generateDocument(Reservation reservation) throws Exception {
        Room room = roomRepository.findById(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        DocumentData data = new DocumentData(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getCompanyCnpj(),
                reservation.getCompanyName(),
                reservation.getGuestPassport(),
                reservation.getGuestCountry(),
                reservation.getRoomNumber(),
                room.getType().getValue(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );

        DocumentFormatter formatter = DocumentFormatterFactory.getFormatter(
                reservation.getGuestCpf(),
                reservation.getCompanyCnpj(),
                reservation.getGuestPassport()
        );

        return formatter.format(data);
    }

    private void sendConfirmationEmail(Reservation reservation) throws Exception {
        Document document = generateDocument(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildEmailBody(reservation, document);

        messageRepository.create("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    private String buildEmailBody(Reservation reservation, Document document) {
        StringBuilder body = new StringBuilder();
        body.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        body.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        body.append(document.getText()).append("\n\n");
        body.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        body.append("Até breve!");

        return body.toString();
    }
}
