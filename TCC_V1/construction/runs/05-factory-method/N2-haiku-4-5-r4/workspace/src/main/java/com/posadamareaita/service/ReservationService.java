package com.posadamareaita.service;

import com.posadamareaita.domain.RatePlan;
import com.posadamareaita.domain.Reservation;
import com.posadamareaita.domain.ReservationStatus;
import com.posadamareaita.domain.Room;
import com.posadamareaita.repository.ReservationRepository;
import com.posadamareaita.repository.RoomRepository;
import com.posadamareaita.util.FormattingUtil;
import com.posadamareaita.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    public static Optional<Reservation> createReservation(int roomNumber, String guestName, String guestEmail,
                                                          LocalDate checkIn, LocalDate checkOut, int guests,
                                                          String ratePlanStr, String guestCpf, String companyCnpj,
                                                          String companyName, String guestPassport, String guestCountry)
            throws Exception {

        // Validate identification
        int identificationCount = (guestCpf != null ? 1 : 0) +
                                  (companyCnpj != null ? 1 : 0) +
                                  (guestPassport != null ? 1 : 0);
        if (identificationCount != 1) {
            return Optional.empty();
        }

        // Validate guests
        if (guests < 1) {
            return Optional.empty();
        }

        // Validate dates
        if (!checkOut.isAfter(checkIn)) {
            return Optional.empty();
        }

        // Get room
        Optional<Room> room = RoomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            return Optional.empty();
        }

        // Check capacity
        if (guests > room.get().getCapacity()) {
            return Optional.empty();
        }

        // Check availability
        List<Reservation> conflicts = ReservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            return Optional.empty();
        }

        // Calculate price
        RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);
        PriceCalculator.PriceResult priceResult = PriceCalculator.calculatePrice(room.get(), checkIn, checkOut, ratePlan);

        // Create reservation
        Reservation res = new Reservation(roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlan,
                guestCpf, companyCnpj, companyName, guestPassport, guestCountry);
        res.setNights((int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
        res.setSubtotal(priceResult.subtotal);
        res.setDiscount(priceResult.discount);
        res.setTotal(priceResult.total);

        long id = ReservationRepository.create(res);
        res.setId(id);

        return Optional.of(res);
    }

    public static Optional<Reservation> confirm(long reservationId, BigDecimal amountPaid) throws Exception {
        Optional<Reservation> opt = ReservationRepository.findById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Reservation res = opt.get();
        if (res.getStatus() != ReservationStatus.PENDING) {
            return Optional.empty();
        }

        res.setStatus(ReservationStatus.CONFIRMED);
        res.setAmountPaid(amountPaid);
        ReservationRepository.update(res);

        // Send confirmation email
        Optional<Room> room = RoomRepository.findByNumber(res.getRoomNumber());
        if (room.isPresent()) {
            String documentText;
            if (res.getGuestCpf() != null) {
                documentText = DocumentService.generateReceiptPT(res, room.get());
            } else if (res.getCompanyCnpj() != null) {
                documentText = DocumentService.generateInvoice(res, room.get());
            } else {
                documentText = DocumentService.generateBookingReceipt(res, room.get());
            }

            String emailBody = buildConfirmationEmail(res, documentText);
            MessageService.sendEmail(res.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);
        }

        return Optional.of(res);
    }

    public static Optional<Reservation> checkIn(long reservationId) throws Exception {
        Optional<Reservation> opt = ReservationRepository.findById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Reservation res = opt.get();
        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            return Optional.empty();
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        ReservationRepository.update(res);

        return Optional.of(res);
    }

    public static Optional<Reservation> checkOut(long reservationId) throws Exception {
        Optional<Reservation> opt = ReservationRepository.findById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Reservation res = opt.get();
        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            return Optional.empty();
        }

        res.setStatus(ReservationStatus.CHECKED_OUT);
        ReservationRepository.update(res);

        return Optional.of(res);
    }

    public static Optional<Reservation> cancel(long reservationId) throws Exception {
        Optional<Reservation> opt = ReservationRepository.findById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Reservation res = opt.get();
        if (res.getStatus() == ReservationStatus.CHECKED_IN ||
            res.getStatus() == ReservationStatus.CHECKED_OUT ||
            res.getStatus() == ReservationStatus.CANCELLED) {
            return Optional.empty();
        }

        // Calculate refund before changing status
        ReservationStatus originalStatus = res.getStatus();
        if (originalStatus == ReservationStatus.CONFIRMED) {
            if (res.getRatePlan() == RatePlan.STANDARD) {
                res.setRefund(res.getAmountPaid());
            } else {
                res.setRefund(BigDecimal.ZERO);
            }
        } else {
            res.setRefund(BigDecimal.ZERO);
        }

        res.setStatus(ReservationStatus.CANCELLED);

        ReservationRepository.update(res);

        return Optional.of(res);
    }

    public static Optional<Reservation> getDocument(long reservationId) throws Exception {
        return ReservationRepository.findById(reservationId);
    }

    private static String buildConfirmationEmail(Reservation res, String documentText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(res.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(documentText).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }
}
