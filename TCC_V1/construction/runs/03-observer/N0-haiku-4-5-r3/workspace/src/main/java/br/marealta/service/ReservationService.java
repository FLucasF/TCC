package br.marealta.service;

import br.marealta.db.MessageDAO;
import br.marealta.db.ReservationDAO;
import br.marealta.db.RoomDAO;
import br.marealta.model.*;
import br.marealta.util.Formatters;
import br.marealta.util.PricingCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final NotificationService notificationService;

    public ReservationService(ReservationDAO reservationDAO, RoomDAO roomDAO,
                              NotificationService notificationService) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                         int guests, RatePlan ratePlan) {
        validateInput(roomNumber, checkIn, checkOut, guests);

        Room room = roomDAO.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan);

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        return reservationDAO.save(reservation);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = reservationDAO.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationDAO.save(reservation);

        notificationService.notifyReservationConfirmed(reservation);
        sendGuestConfirmation(reservation);

        logger.info("Reservation {} confirmed", reservationId);
        return reservation;
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationDAO.save(reservation);

        logger.info("Reservation {} checked in", reservationId);
        return reservation;
    }

    public Reservation checkOut(long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationDAO.save(reservation);

        logger.info("Reservation {} checked out", reservationId);
        return reservation;
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = reservation.getStatus() != ReservationStatus.PENDING;

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (wasConfirmed) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        }

        reservationDAO.save(reservation);

        if (wasConfirmed) {
            notificationService.notifyReservationCancelled(reservation);
        }

        logger.info("Reservation {} cancelled", reservationId);
        return reservation;
    }

    public Reservation getReservation(long reservationId) {
        return reservationDAO.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return !reservationDAO.findByRoomAndDateRange(roomNumber, checkIn, checkOut).isEmpty();
    }

    private void validateInput(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private void sendGuestConfirmation(Reservation reservation) {
        String document = generateConfirmation(reservation);
        String emailBody = String.format("Olá, %s!\n\n" +
                        "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                        "%s\n\n" +
                        "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                        "Até breve!",
                reservation.getGuestName(),
                document);

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
        notificationService.send(notification);
    }

    public String generateConfirmation(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), reservation.getGuestCpf()));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(),
            roomDAO.findByNumber(reservation.getRoomNumber()).get().getType().name()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            Formatters.formatDate(reservation.getCheckIn()),
            Formatters.formatDate(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", Formatters.formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", Formatters.formatMoney(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", Formatters.formatMoney(reservation.getTotal())));

        return sb.toString();
    }
}
