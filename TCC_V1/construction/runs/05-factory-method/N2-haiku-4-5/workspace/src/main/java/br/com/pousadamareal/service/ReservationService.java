package br.com.pousadamareal.service;

import br.com.pousadamareal.domain.Channel;
import br.com.pousadamareal.domain.Document;
import br.com.pousadamareal.domain.IdentificationType;
import br.com.pousadamareal.domain.Message;
import br.com.pousadamareal.domain.RatePlan;
import br.com.pousadamareal.domain.Reservation;
import br.com.pousadamareal.domain.ReservationStatus;
import br.com.pousadamareal.domain.Room;
import br.com.pousadamareal.repository.MessageRepository;
import br.com.pousadamareal.repository.ReservationRepository;
import br.com.pousadamareal.repository.RoomRepository;
import br.com.pousadamareal.util.DateUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final DocumentService documentService;
    private final PricingService pricingService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            MessageRepository messageRepository,
            DocumentService documentService,
            PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.documentService = documentService;
        this.pricingService = pricingService;
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
            RatePlan ratePlan) throws SQLException {

        Room room = roomRepository.findById(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException(
                guests > room.getCapacity() ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST"
            );
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        List<Reservation> existingReservations = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!existingReservations.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        var pricingResult = pricingService.calculatePrice(checkIn, checkOut, room.getNightlyRate(), ratePlan);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCompanyCnpj(companyCnpj);
        reservation.setCompanyName(companyName);
        reservation.setGuestPassport(guestPassport);
        reservation.setGuestCountry(guestCountry);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(pricingResult.nights);
        reservation.setSubtotal(pricingResult.subtotal);
        reservation.setDiscount(pricingResult.discount);
        reservation.setTotal(pricingResult.total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(0);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Document checkOut(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return documentService.generateDocument(reservation);
    }

    public Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationStatus previousStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CANCELLED);

        if (previousStatus == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(0.0);
            }
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation getReservation(long reservationId) throws SQLException {
        return reservationRepository.findById(reservationId);
    }

    public Document getDocument(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        return documentService.generateDocument(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) throws SQLException {
        Document doc = documentService.generateDocument(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = """
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s
            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """.formatted(reservation.getGuestName(), doc.getText());

        Message message = new Message(Channel.EMAIL, reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }
}
