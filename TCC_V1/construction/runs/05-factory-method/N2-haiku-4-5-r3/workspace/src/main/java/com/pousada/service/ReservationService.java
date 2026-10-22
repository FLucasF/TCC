package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.document.Document;
import com.pousada.document.DocumentGeneratorFactory;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private static final ReservationService instance = new ReservationService();
    private final ReservationRepository reservationRepo = ReservationRepository.getInstance();
    private final RoomRepository roomRepo = RoomRepository.getInstance();
    private final MessageRepository messageRepo = MessageRepository.getInstance();

    private ReservationService() {
    }

    public static ReservationService getInstance() {
        return instance;
    }

    public Reservation createReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {
        Room room = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (guests < 1 || guests > room.capacity()) {
            throw new IllegalArgumentException("Invalid number of guests");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("Invalid dates");
        }

        var conflicts = reservationRepo.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Room unavailable for those dates");
        }

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            BigDecimal nightPrice = PriceCalculator.calculateNightlyPrice(room.nightlyRate(), date);
            subtotal = subtotal.add(nightPrice);
        }

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return reservationRepo.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot confirm non-pending reservation");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepo.update(reservation);

        sendConfirmationEmail(reservation);

        return reservationRepo.findById(id).orElseThrow();
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot check in non-confirmed reservation");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepo.update(reservation);

        return reservationRepo.findById(id).orElseThrow();
    }

    public Document checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Cannot check out reservation that is not checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepo.update(reservation);

        Room room = roomRepo.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        return DocumentGeneratorFactory.create(reservation)
            .generate(reservation, room);
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel reservation in current status");
        }

        ReservationStatus previousStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CANCELLED);

        if (previousStatus == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        }

        reservationRepo.update(reservation);

        return reservationRepo.findById(id).orElseThrow();
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Document not available for this reservation");
        }

        Room room = roomRepo.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        return DocumentGeneratorFactory.create(reservation)
            .generate(reservation, room);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomRepo.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Document document = DocumentGeneratorFactory.create(reservation)
            .generate(reservation, room);

        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            document.text() + "\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";

        messageRepo.save("EMAIL", reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta", body);
    }
}
