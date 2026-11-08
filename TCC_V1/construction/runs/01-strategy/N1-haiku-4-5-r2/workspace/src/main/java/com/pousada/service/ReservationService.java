package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.domain.rateplans.CorporateRatePlan;
import com.pousada.domain.rateplans.InvalidReservationException;
import com.pousada.domain.rateplans.RatePlan;
import com.pousada.domain.rateplans.RatePlanRegistry;
import com.pousada.infra.MessageRepository;
import com.pousada.infra.ReservationRepository;
import com.pousada.infra.RoomRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;

public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository,
                             MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                         String companyCode) throws SQLException {
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Reserva deve ter pelo menos 1 hóspede", 400);
        }

        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new ApiException("INVALID_REQUEST", "Data de saída deve ser após a entrada", 400);
        }

        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
        }

        if (guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto", 422);
        }

        if (!RatePlanRegistry.exists(ratePlan)) {
            throw new ApiException("INVALID_REQUEST", "Plano de tarifa inválido", 400);
        }

        RatePlan plan = RatePlanRegistry.get(ratePlan);
        try {
            plan.validate(roomNumber, checkIn, checkOut, guests, companyCode);
        } catch (InvalidReservationException e) {
            throw new ApiException(e.getCode(), e.getMessage(), 400);
        }

        var conflictingReservations = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflictingReservations.isEmpty()) {
            throw new ApiException("ROOM_UNAVAILABLE", "Quarto não disponível nas datas solicitadas", 409);
        }

        RatePlan.PriceCalculation priceCalc;
        if (plan instanceof CorporateRatePlan corpPlan) {
            priceCalc = corpPlan.calculatePrice(room, checkIn, checkOut, companyCode);
        } else {
            priceCalc = plan.calculatePrice(room, checkIn, checkOut);
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            companyCode,
            nights,
            priceCalc.subtotal(),
            priceCalc.discount(),
            priceCalc.total(),
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }
        return reservation;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = getReservation(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes podem ser confirmadas", 409);
        }

        reservationRepository.updateConfirmation(id, ReservationStatus.CONFIRMED, amountPaid);
        reservation = getReservation(id);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) throws SQLException {
        Reservation reservation = getReservation(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas confirmadas podem fazer check-in", 409);
        }

        reservationRepository.updateStatus(id, ReservationStatus.CHECKED_IN);
        return getReservation(id);
    }

    public Reservation checkOut(long id) throws SQLException {
        Reservation reservation = getReservation(id);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas com check-in podem fazer check-out", 409);
        }

        reservationRepository.updateStatus(id, ReservationStatus.CHECKED_OUT);
        return getReservation(id);
    }

    public Reservation cancelReservation(long id) throws SQLException {
        Reservation reservation = getReservation(id);
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Reserva não pode ser cancelada", 409);
        }

        RatePlan plan = RatePlanRegistry.get(reservation.getRatePlan());
        BigDecimal refund = plan.calculateRefund(reservation.getAmountPaid());

        reservationRepository.updateCancellation(id, refund);
        return getReservation(id);
    }

    private void sendConfirmationEmail(Reservation reservation) throws SQLException {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        String receipt = generateReceipt(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = formatConfirmationEmail(reservation, receipt);

        messageRepository.save("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    public String generateReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCPF(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(reservation.getCheckOut())).append(" | ");
        sb.append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal())).append("\n");
        return sb.toString();
    }

    private String formatConfirmationEmail(Reservation reservation, String receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public String formatMoney(BigDecimal amount) {
        var symbols = new java.text.DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        var df = new java.text.DecimalFormat("#,##0.00", symbols);
        return "R$ " + df.format(amount);
    }

    public String formatCPF(String cpf) {
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
