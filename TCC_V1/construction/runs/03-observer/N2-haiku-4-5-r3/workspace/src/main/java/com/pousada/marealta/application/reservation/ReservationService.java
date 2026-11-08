package com.pousada.marealta.application.reservation;

import com.pousada.marealta.domain.notification.DomainEvent;
import com.pousada.marealta.domain.notification.NotificationSubscriber;
import com.pousada.marealta.domain.notification.PersistedNotification;
import com.pousada.marealta.domain.notification.ReservationCancelled;
import com.pousada.marealta.domain.notification.ReservationConfirmed;
import com.pousada.marealta.domain.pricing.RateCalculator;
import com.pousada.marealta.domain.pricing.RatePlan;
import com.pousada.marealta.domain.repository.NotificationRepository;
import com.pousada.marealta.domain.repository.ReservationRepository;
import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.reservation.ReservationStatus;
import com.pousada.marealta.domain.room.Room;
import com.pousada.marealta.domain.shared.DateRange;
import com.pousada.marealta.domain.shared.Guest;
import com.pousada.marealta.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationRepository notificationRepository;
    private final OccupancyCalculator occupancyCalculator;
    private final List<NotificationSubscriber> notificationSubscribers;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            NotificationRepository notificationRepository,
            List<NotificationSubscriber> notificationSubscribers
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationRepository = notificationRepository;
        this.occupancyCalculator = new OccupancyCalculator(reservationRepository);
        this.notificationSubscribers = notificationSubscribers;
    }

    public ReservationRepository getReservationRepository() {
        return reservationRepository;
    }

    public Reservation createReservation(CreateReservationRequest request) {
        // Validate input
        if (request.roomNumber <= 0) {
            throw new IllegalArgumentException("Invalid room number");
        }
        if (request.checkIn == null || request.checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }

        // Get room
        Room room = roomRepository.findByNumber(request.roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Validate dates and occupancy
        DateRange dates = new DateRange(request.checkIn, request.checkOut);
        occupancyCalculator.validateOccupancy(room, dates, request.guests);

        // Create guest
        Guest guest = new Guest(request.guestName, request.guestEmail, request.guestCpf);

        // Calculate price
        RatePlan ratePlan = RatePlan.fromString(request.ratePlan);
        RateCalculator calculator = new RateCalculator(room.nightlyRate());
        var quote = calculator.quote(dates, ratePlan);

        // Create reservation
        long reservationId = reservationRepository.nextId();
        Reservation reservation = new Reservation(
                reservationId,
                room,
                guest,
                dates,
                request.guests,
                ratePlan,
                quote.subtotal(),
                quote.discount(),
                quote.total()
        );

        reservationRepository.save(reservation);
        logger.info("Created reservation {}", reservationId);

        return reservation;
    }

    public Reservation confirmReservation(long reservationId, Money amountPaid) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation is not pending");
        }

        ReservationStatus previousStatus = reservation.status();
        reservation.confirm(amountPaid);
        reservationRepository.save(reservation);

        // Emit event and dispatch to subscribers
        DomainEvent event = new ReservationConfirmed(reservation, previousStatus);
        dispatchEvent(event);

        logger.info("Confirmed reservation {}", reservationId);
        return reservation;
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Reservation is not confirmed");
        }

        reservation.checkIn();
        reservationRepository.save(reservation);

        logger.info("Checked in reservation {}", reservationId);
        return reservation;
    }

    public Reservation checkOut(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Reservation is not checked in");
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        logger.info("Checked out reservation {}", reservationId);
        return reservation;
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status() == ReservationStatus.CHECKED_OUT ||
            reservation.status() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel reservation with status " + reservation.status());
        }

        ReservationStatus previousStatus = reservation.status();
        reservation.cancel();
        reservationRepository.save(reservation);

        // Only dispatch event if reservation was confirmed
        if (previousStatus == ReservationStatus.CONFIRMED) {
            DomainEvent event = new ReservationCancelled(reservation, reservation.refund());
            dispatchEvent(event);
        }

        logger.info("Cancelled reservation {}", reservationId);
        return reservation;
    }

    private void dispatchEvent(DomainEvent event) {
        for (NotificationSubscriber subscriber : notificationSubscribers) {
            try {
                PersistedNotification notification = subscriber.handle(event);
                if (notification != null) {
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                logger.error("Error dispatching event to subscriber {}", subscriber.name(), e);
            }
        }
    }
}
