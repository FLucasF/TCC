package br.com.pousada.service;

import br.com.pousada.model.*;
import br.com.pousada.repository.ReservationRepository;
import br.com.pousada.repository.RoomRepository;
import br.com.pousada.util.DateUtil;
import br.com.pousada.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final NotificationService notificationService = new NotificationService();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        // Validações
        Optional<Room> room = roomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new ApiException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
        }

        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Quantidade de hóspedes deve ser maior que zero", 400);
        }

        if (guests > room.get().capacity) {
            throw new ApiException("CAPACITY_EXCEEDED", "Quarto não comporta esta quantidade de hóspedes", 422);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Data de saída deve ser após a data de entrada", 400);
        }

        // Verificar disponibilidade
        var conflictingReservations = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflictingReservations.isEmpty()) {
            throw new ApiException("ROOM_UNAVAILABLE", "Quarto não está disponível para o período solicitado", 409);
        }

        // Calcular preços
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Room r = room.get();

        double subtotal = 0;
        LocalDate date = checkIn;
        while (!date.equals(checkOut)) {
            double dailyRate = r.nightlyRate;
            if (DateUtil.isHighSeason(date)) {
                dailyRate = MoneyUtil.applyPercentage(dailyRate, 20);
            }
            dailyRate = MoneyUtil.round(dailyRate);
            subtotal += dailyRate;
            date = date.plusDays(1);
        }
        subtotal = MoneyUtil.round(subtotal);

        double discount = 0;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = MoneyUtil.calculateDiscount(subtotal, 15);
        }

        double total = MoneyUtil.round(subtotal - discount);

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
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            0,
            null
        );

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(long id, double amountPaid) {
        Optional<Reservation> opt = reservationRepository.findById(id);
        if (opt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }

        Reservation reservation = opt.get();
        if (reservation.status != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes podem ser confirmadas", 409);
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = MoneyUtil.round(amountPaid);
        reservationRepository.save(reservation);

        // Enviar notificações
        notificationService.sendGuestConfirmationEmail(reservation);
        notificationService.sendHousekeepingNotification(reservation);
        notificationService.sendFinanceNotificationConfirmation(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Optional<Reservation> opt = reservationRepository.findById(id);
        if (opt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }

        Reservation reservation = opt.get();
        if (reservation.status == ReservationStatus.CHECKED_OUT ||
            reservation.status == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Não é possível cancelar esta reserva", 409);
        }

        boolean wasConfirmed = reservation.status == ReservationStatus.CONFIRMED;
        double refundAmount = 0;

        if (wasConfirmed) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                refundAmount = reservation.amountPaid;
            }
            // NON_REFUNDABLE não retorna nada
        }
        // PENDING não tem o que devolver

        reservation.status = ReservationStatus.CANCELLED;
        if (refundAmount > 0) {
            reservation.refund = MoneyUtil.round(refundAmount);
        }

        reservationRepository.save(reservation);

        // Enviar notificações apenas se era confirmada
        if (wasConfirmed) {
            notificationService.sendHousekeepingCancellationNotification(reservation);
            if (refundAmount > 0) {
                notificationService.sendFinanceNotificationCancellation(reservation, refundAmount);
            }
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Optional<Reservation> opt = reservationRepository.findById(id);
        if (opt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }

        Reservation reservation = opt.get();
        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas confirmadas podem fazer check-in", 409);
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) {
        Optional<Reservation> opt = reservationRepository.findById(id);
        if (opt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }

        Reservation reservation = opt.get();
        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Apenas reservas com check-in podem fazer check-out", 409);
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        reservationRepository.save(reservation);

        return reservation;
    }
}
