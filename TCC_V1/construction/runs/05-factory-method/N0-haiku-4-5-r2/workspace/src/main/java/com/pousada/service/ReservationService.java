package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.model.Document;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.util.DocumentFormatter;
import com.pousada.util.Pricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;

    public ReservationService(Database db) {
        this.reservationRepo = new ReservationRepository(db);
        this.roomRepo = new RoomRepository(db);
        this.messageRepo = new MessageRepository(db);
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan)
            throws ValidationException {

        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "Número de hóspedes deve ser pelo menos 1");
        }

        Optional<Room> roomOpt = roomRepo.findByNumber(roomNumber);
        if (roomOpt.isEmpty()) {
            throw new ValidationException("ROOM_NOT_FOUND", "Quarto não encontrado");
        }

        Room room = roomOpt.get();
        if (guests > room.capacity) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Número de hóspedes excede capacidade do quarto");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("INVALID_REQUEST", "Data de saída deve ser depois da entrada");
        }

        List<Reservation> conflicting = reservationRepo.findConflicting(roomNumber, checkIn, checkOut);
        if (!conflicting.isEmpty()) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Quarto não disponível para as datas solicitadas");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal nightlyRate = room.nightlyRate;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            BigDecimal dailyRate = Pricing.calculateNightlyRate(nightlyRate, date);
            subtotal = Pricing.calculateSubtotal(dailyRate, 1).add(subtotal);
        }

        BigDecimal discount = Pricing.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = Pricing.calculateTotal(subtotal, discount);

        return reservationRepo.create(roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);
    }

    public Reservation getReservation(long id) throws ValidationException {
        Optional<Reservation> res = reservationRepo.findById(id);
        if (res.isEmpty()) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
        return res.get();
    }

    public Reservation confirmReservation(long id) throws ValidationException {
        Reservation res = getReservation(id);
        if (!"PENDING".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reserva não está pendente");
        }

        reservationRepo.updateStatus(id, "CONFIRMED");
        reservationRepo.updateAmountPaid(id, res.total);

        Reservation confirmed = getReservation(id);

        Document doc = generateDocument(confirmed);
        String emailBody = DocumentFormatter.emailComprovante(confirmed.guestName, doc.text);
        messageRepo.save("EMAIL", confirmed.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);

        return confirmed;
    }

    public Reservation checkIn(long id) throws ValidationException {
        Reservation res = getReservation(id);
        if (!"CONFIRMED".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reserva não está confirmada");
        }

        reservationRepo.updateStatus(id, "CHECKED_IN");
        return getReservation(id);
    }

    public Document checkOut(long id) throws ValidationException {
        Reservation res = getReservation(id);
        if (!"CHECKED_IN".equals(res.status)) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Hóspede não fez check-in");
        }

        reservationRepo.updateStatus(id, "CHECKED_OUT");
        Reservation checkedOut = getReservation(id);
        return generateDocument(checkedOut);
    }

    public Reservation cancelReservation(long id) throws ValidationException {
        Reservation res = getReservation(id);

        if (!("PENDING".equals(res.status) || "CONFIRMED".equals(res.status))) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Não é possível cancelar esta reserva");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if ("CONFIRMED".equals(res.status) && "STANDARD".equals(res.ratePlan)) {
            refund = res.amountPaid;
        }

        reservationRepo.updateStatus(id, "CANCELLED");
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            reservationRepo.updateRefund(id, refund);
        }

        return getReservation(id);
    }

    public Document getDocument(long id) throws ValidationException {
        Reservation res = getReservation(id);
        if (!("CONFIRMED".equals(res.status) || "CHECKED_IN".equals(res.status) || "CHECKED_OUT".equals(res.status))) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Documento só disponível para reservas confirmadas");
        }
        return generateDocument(res);
    }

    private Document generateDocument(Reservation res) throws ValidationException {
        Optional<Room> roomOpt = roomRepo.findByNumber(res.roomNumber);
        if (roomOpt.isEmpty()) {
            throw new ValidationException("ROOM_NOT_FOUND", "Quarto não encontrado");
        }
        Room room = roomOpt.get();

        String text;
        if (res.guestCpf != null) {
            text = DocumentFormatter.comprovante(res.id, res.guestName, res.guestCpf,
                    res.roomNumber, room.type, res.checkIn, res.checkOut, res.nights,
                    res.subtotal, res.discount, res.total);
        } else if (res.companyCnpj != null) {
            text = DocumentFormatter.fatura(res.id, res.guestName, res.companyCnpj, res.companyName,
                    res.roomNumber, room.type, res.checkIn, res.checkOut, res.nights,
                    res.subtotal, res.discount, res.total);
        } else {
            text = DocumentFormatter.bookingReceipt(res.id, res.guestName, res.guestPassport, res.guestCountry,
                    res.roomNumber, room.type, res.checkIn, res.checkOut, res.nights,
                    res.subtotal, res.discount, res.total);
        }

        return new Document("Comprovante de reserva", res.id, res.guestName, res.guestCpf,
                res.companyCnpj, res.companyName, res.guestPassport, res.guestCountry,
                res.roomNumber, room.type, res.checkIn, res.checkOut, res.nights,
                res.subtotal, res.discount, res.total, text);
    }

    public static class ValidationException extends Exception {
        public final String code;
        public final String message;

        public ValidationException(String code, String message) {
            super(message);
            this.code = code;
            this.message = message;
        }
    }
}
