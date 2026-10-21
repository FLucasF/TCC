package com.pousadamareaita.service;

import com.pousadamareaita.exception.*;
import com.pousadamareaita.model.*;
import com.pousadamareaita.pricing.*;
import com.pousadamareaita.storage.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests,
                                         RatePlan ratePlan, String companyCode) {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException(roomNumber);
        }

        RatePlanStrategy strategy = RatePlanStrategyFactory.create(ratePlan, room.getType(), companyCode);

        try {
            strategy.validate(checkIn, checkOut, guests, room.getCapacity(), companyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        PriceCalculation priceCalc = strategy.calculatePrice(checkIn, checkOut, room.getNightlyRate(), guests);

        int reservationId = reservationRepository.getNextId();
        Reservation reservation = new Reservation(
            reservationId,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            companyCode,
            priceCalc.getNights(),
            priceCalc.getSubtotal(),
            priceCalc.getDiscount(),
            priceCalc.getTotal()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(int id) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException(id);
        }
        return reservation;
    }

    public Reservation confirmReservation(int id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm a pending reservation");
        }

        reservation.confirm(amountPaid);
        sendConfirmationEmail(reservation);
        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(int id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check in a confirmed reservation");
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(int id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check out a checked-in reservation");
        }

        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(int id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only cancel a pending or confirmed reservation");
        }

        RatePlanStrategy strategy = RatePlanStrategyFactory.create(
            reservation.getRatePlan(),
            getRoomType(reservation.getRoomNumber()),
            reservation.getCompanyCode()
        );

        BigDecimal refund = strategy.calculateRefund(reservation.getAmountPaid());
        reservation.cancel(refund);
        return reservationRepository.save(reservation);
    }

    private void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservationRepository.findAll()) {
            if (res.getRoomNumber() != roomNumber) {
                continue;
            }

            if (res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            // Overlap if: checkIn < res.getCheckOut() AND checkOut > res.getCheckIn()
            if (checkIn.isBefore(res.getCheckOut()) && checkOut.isAfter(res.getCheckIn())) {
                throw new RoomUnavailableException(roomNumber);
            }
        }
    }

    private RoomType getRoomType(int roomNumber) {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException(roomNumber);
        }
        return room.getType();
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String receipt = generateReceipt(reservation);
        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                      "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                      receipt + "\n\n" +
                      "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                      "Até breve!";

        Message message = new Message(
            messageRepository.getNextId(),
            "EMAIL",
            reservation.getGuestEmail(),
            subject,
            body
        );
        messageRepository.save(message);
    }

    public String generateReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(reservation.getCheckOut())).append(" | ");
        sb.append(nightsText).append("\n");
        sb.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", ".");
    }
}
