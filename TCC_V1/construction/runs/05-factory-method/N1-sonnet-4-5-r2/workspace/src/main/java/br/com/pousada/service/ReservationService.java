package br.com.pousada.service;

import br.com.pousada.domain.*;
import br.com.pousada.exception.*;
import br.com.pousada.repository.ReservationRepository;
import br.com.pousada.repository.RoomRepository;
import br.com.pousada.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            DocumentService documentService,
                            NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                       GuestIdentification identification, LocalDate checkIn,
                                       LocalDate checkOut, int guests, RatePlan ratePlan) {

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(guests, room.getCapacity());
        }

        List<Reservation> conflicts = reservationRepository.findActiveByRoomAndDateRange(
            roomNumber, checkIn, checkOut);

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException(roomNumber);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null, roomNumber, guestName, guestEmail, identification,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
            ReservationStatus.PENDING, BigDecimal.ZERO, null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.confirm();
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.generateDocument(reservation, room);
        notificationService.sendConfirmationEmail(reservation.getGuestName(), reservation.getGuestEmail(), document);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check-in reservation in status " + reservation.getStatus());
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check-out reservation in status " + reservation.getStatus());
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentService.generateDocument(reservation, room);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return documentService.generateDocument(reservation, room);
    }
}
