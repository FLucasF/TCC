package br.com.pousadamarealta.domain.service;

import br.com.pousadamarealta.domain.exception.*;
import br.com.pousadamarealta.domain.model.*;
import br.com.pousadamarealta.domain.rateplan.RatePlan;
import br.com.pousadamarealta.domain.rateplan.RatePlanFactory;
import br.com.pousadamarealta.domain.repository.ReservationRepository;
import br.com.pousadamarealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

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

    public Reservation createReservation(RatePlan.ReservationRequest request, RatePlanType ratePlanType) {
        Room room = roomRepository.findByNumber(request.roomNumber())
            .orElseThrow(() -> new RoomNotFoundException("Quarto não encontrado"));

        validateBasicRules(request, room);

        RatePlan ratePlan = RatePlanFactory.get(ratePlanType);
        ratePlan.validate(request, room);

        if (!reservationRepository.findConflicting(
            request.roomNumber(),
            request.checkIn(),
            request.checkOut(),
            null
        ).isEmpty()) {
            throw new RoomUnavailableException("Quarto não disponível para as datas solicitadas");
        }

        PriceCalculation price = ratePlan.calculatePrice(request, room);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(request.roomNumber());
        reservation.setGuestName(request.guestName());
        reservation.setGuestEmail(request.guestEmail());
        reservation.setGuestCpf(request.guestCpf());
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setGuests(request.guests());
        reservation.setRatePlan(ratePlanType);
        reservation.setCompanyCode(request.companyCode());
        reservation.setNights((int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()));
        reservation.setSubtotal(price.getSubtotal());
        reservation.setDiscount(price.getDiscount());
        reservation.setTotal(price.getTotal());

        return reservationRepository.save(reservation);
    }

    private void validateBasicRules(RatePlan.ReservationRequest request, Room room) {
        if (request.guests() < 1) {
            throw new InvalidRequestException("Número de hóspedes deve ser no mínimo 1");
        }

        if (request.guests() > room.getCapacity()) {
            throw new CapacityExceededException("Número de hóspedes excede a capacidade do quarto");
        }

        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new InvalidRequestException("Data de saída deve ser posterior à data de entrada");
        }
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Apenas reservas pendentes podem ser confirmadas");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        String emailBody = documentService.generateConfirmationEmail(reservation, room);
        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Apenas reservas confirmadas podem fazer check-in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Apenas reservas com check-in podem fazer check-out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Apenas reservas pendentes ou confirmadas podem ser canceladas");
        }

        RatePlan ratePlan = RatePlanFactory.get(reservation.getRatePlan());
        BigDecimal refund = ratePlan.calculateRefund(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada"));
    }
}
