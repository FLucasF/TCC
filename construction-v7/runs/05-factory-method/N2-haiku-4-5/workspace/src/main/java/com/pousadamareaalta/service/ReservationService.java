package com.pousadamareaalta.service;

import com.pousadamareaalta.model.*;
import com.pousadamareaalta.util.DateUtils;
import com.pousadamareaalta.util.MoneyRounder;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final DocumentService documentService;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        MessageService messageService,
        DocumentService documentService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.documentService = documentService;
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
    ) throws ValidationException {
        Room room = roomRepository.findById(roomNumber);
        if (room == null) {
            throw new ValidationException("ROOM_NOT_FOUND", "Quarto não encontrado");
        }

        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "Número de hóspedes inválido");
        }

        if (guests > room.capacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Quarto não comporta esse número de hóspedes");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ValidationException("INVALID_REQUEST", "Data de saída deve ser após data de entrada");
        }

        int identificationCount = countIdentifications(guestCpf, companyCnpj, guestPassport);
        if (identificationCount != 1) {
            throw new ValidationException("INVALID_REQUEST", "Deve informar exatamente um tipo de identificação");
        }

        if (companyCnpj != null && (companyName == null || companyName.isEmpty())) {
            throw new ValidationException("INVALID_REQUEST", "Razão social é obrigatória com CNPJ");
        }

        if (guestPassport != null && (guestCountry == null || guestCountry.isEmpty())) {
            throw new ValidationException("INVALID_REQUEST", "País é obrigatório com passaporte");
        }

        java.util.List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(
            roomNumber, checkIn, checkOut
        );
        for (Reservation r : conflicting) {
            if (r.getStatus() != ReservationStatus.CANCELLED) {
                throw new ValidationException("ROOM_UNAVAILABLE", "Quarto indisponível nessas datas");
            }
        }

        int nights = DateUtils.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room.nightlyRate(), checkIn, checkOut, nights);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = MoneyRounder.round(subtotal.subtract(discount));

        long reservationId = reservationRepository.generateId();
        Reservation reservation = new Reservation(
            reservationId,
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
            total
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long reservationId) throws ValidationException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes podem ser confirmadas");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        Room room = roomRepository.findById(reservation.getRoomNumber());
        Document document = documentService.generateDocument(reservation, room);

        String emailBody = buildConfirmationEmail(reservation, document.text());
        messageService.send("EMAIL", reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);

        return reservation;
    }

    public Reservation checkin(long reservationId) throws ValidationException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Apenas reservas confirmadas podem fazer check-in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Document checkout(long reservationId) throws ValidationException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Apenas hóspedes com check-in podem fazer checkout");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        Room room = roomRepository.findById(reservation.getRoomNumber());
        return documentService.generateDocument(reservation, room);
    }

    public Reservation cancelReservation(long reservationId) throws ValidationException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes ou confirmadas podem ser canceladas");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                reservation.setRefund(reservation.getTotal());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        }

        return reservation;
    }

    public Document getDocument(long reservationId) throws ValidationException {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Apenas reservas pagas podem gerar segunda via");
        }

        Room room = roomRepository.findById(reservation.getRoomNumber());
        return documentService.generateDocument(reservation, room);
    }

    private BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal rate = nightlyRate;
            if (DateUtils.isHighSeason(current)) {
                rate = MoneyRounder.round(rate.multiply(new BigDecimal("1.20")));
            }
            total = total.add(rate);
            current = current.plusDays(1);
        }
        return MoneyRounder.round(total);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return MoneyRounder.round(subtotal.multiply(new BigDecimal("0.15")));
        }
        return BigDecimal.ZERO;
    }

    private String buildConfirmationEmail(Reservation reservation, String documentText) {
        return "Olá, " + reservation.getGuestName() + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               documentText + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    private int countIdentifications(String guestCpf, String companyCnpj, String guestPassport) {
        int count = 0;
        if (guestCpf != null && !guestCpf.isEmpty()) count++;
        if (companyCnpj != null && !companyCnpj.isEmpty()) count++;
        if (guestPassport != null && !guestPassport.isEmpty()) count++;
        return count;
    }

    public static class ValidationException extends Exception {
        public final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}
