package com.pousadamarealta;

import com.pousadamarealta.domain.enums.RatePlan;
import com.pousadamarealta.domain.enums.ReservationStatus;
import com.pousadamarealta.domain.enums.RoomType;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.events.ReservationEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.event.EventListener;
import com.pousadamarealta.service.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;
    private EventPublisher eventPublisher;
    private ReservationService service;
    private TestEventListener testListener;

    @BeforeEach
    void setUp() {
        reservationRepository = new InMemoryReservationRepository();
        roomRepository = new InMemoryRoomRepository();
        eventPublisher = new EventPublisher();
        testListener = new TestEventListener();
        eventPublisher.subscribe(testListener);
        service = new ReservationService(reservationRepository, roomRepository, eventPublisher);

        roomRepository.save(new Room("101", RoomType.STANDARD));
        roomRepository.save(new Room("201", RoomType.DELUXE));
        roomRepository.save(new Room("301", RoomType.SUITE));
    }

    @Test
    void shouldCalculateCorrectPriceForLowSeason() {
        Reservation reservation = service.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(new BigDecimal("249.90"), reservation.getNightlyRate());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
    }

    @Test
    void shouldCalculateCorrectPriceForHighSeason() {
        Reservation reservation = service.createReservation(
            "101", "Maria Santos", "maria@example.com", "98765432109",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(new BigDecimal("299.88"), reservation.getNightlyRate());
        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        Reservation reservation = service.createReservation(
            "301", "Pedro Alves", "pedro@example.com", "11111111111",
            LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 3),
            3, RatePlan.NON_REFUNDABLE
        );

        assertEquals(new BigDecimal("1159.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("173.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("985.83"), reservation.getTotal());
    }

    @Test
    void shouldRejectCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            service.createReservation(
                "101", "Muita Gente", "muita@example.com", "22222222222",
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 2),
                5, RatePlan.STANDARD
            );
        });
    }

    @Test
    void shouldRejectRoomConflict() {
        service.createReservation(
            "201", "Primeiro", "primeiro@example.com", "11111111111",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        service.confirmReservation(1L);

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            service.createReservation(
                "201", "Segundo", "segundo@example.com", "22222222222",
                LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
                2, RatePlan.STANDARD
            );
        });
    }

    @Test
    void shouldConfirmReservationAndPublishEvent() {
        Reservation created = service.createReservation(
            "201", "Ana Costa", "ana@example.com", "33333333333",
            LocalDate.of(2027, 4, 15), LocalDate.of(2027, 4, 16),
            1, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, created.getStatus());
        assertEquals(BigDecimal.ZERO, created.getAmountPaid());

        Reservation confirmed = service.confirmReservation(created.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());

        assertEquals(1, testListener.events.size());
        assertTrue(testListener.events.get(0) instanceof ReservationConfirmedEvent);
    }

    @Test
    void shouldCancelPendingReservationWithoutNotification() {
        Reservation created = service.createReservation(
            "201", "Carlos Dias", "carlos@example.com", "44444444444",
            LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 2),
            1, RatePlan.STANDARD
        );

        Reservation cancelled = service.cancelReservation(created.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());

        assertEquals(1, testListener.events.size());
        ReservationCancelledEvent event = (ReservationCancelledEvent) testListener.events.get(0);
        assertFalse(event.wasConfirmed());
    }

    @Test
    void shouldCancelConfirmedReservationWithRefundForStandard() {
        Reservation created = service.createReservation(
            "201", "Lucia Ferreira", "lucia@example.com", "55555555555",
            LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 2),
            1, RatePlan.STANDARD
        );

        service.confirmReservation(created.getId());
        testListener.events.clear();

        Reservation cancelled = service.cancelReservation(created.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());

        assertEquals(1, testListener.events.size());
        ReservationCancelledEvent event = (ReservationCancelledEvent) testListener.events.get(0);
        assertTrue(event.wasConfirmed());
    }

    @Test
    void shouldCancelConfirmedReservationWithoutRefundForNonRefundable() {
        Reservation created = service.createReservation(
            "201", "Ricardo Lima", "ricardo@example.com", "66666666666",
            LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 2),
            1, RatePlan.NON_REFUNDABLE
        );

        service.confirmReservation(created.getId());
        testListener.events.clear();

        Reservation cancelled = service.cancelReservation(created.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());

        assertEquals(1, testListener.events.size());
        ReservationCancelledEvent event = (ReservationCancelledEvent) testListener.events.get(0);
        assertTrue(event.wasConfirmed());
    }

    static class TestEventListener implements EventListener {
        List<ReservationEvent> events = new ArrayList<>();

        @Override
        public void handle(ReservationEvent event) {
            events.add(event);
        }
    }

    static class InMemoryRoomRepository extends RoomRepository {
        private final List<Room> rooms = new ArrayList<>();

        @Override
        public void save(Room room) {
            rooms.removeIf(r -> r.getNumber().equals(room.getNumber()));
            rooms.add(room);
        }

        @Override
        public Optional<Room> findByNumber(String number) {
            return rooms.stream()
                    .filter(r -> r.getNumber().equals(number))
                    .findFirst();
        }

        @Override
        public List<Room> findAll() {
            return new ArrayList<>(rooms);
        }
    }

    static class InMemoryReservationRepository extends ReservationRepository {
        private final List<Reservation> reservations = new ArrayList<>();
        private long nextId = 1;

        @Override
        public Reservation save(Reservation reservation) {
            if (reservation.getId() == null) {
                reservation.setId(nextId++);
                reservations.add(reservation);
            }
            return reservation;
        }

        @Override
        public Optional<Reservation> findById(Long id) {
            return reservations.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst();
        }

        @Override
        public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
            return reservations.stream()
                    .filter(r -> r.getRoomNumber().equals(roomNumber))
                    .filter(r -> r.isActive())
                    .filter(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId))
                    .anyMatch(r -> r.getCheckIn().isBefore(checkOut) && r.getCheckOut().isAfter(checkIn));
        }
    }
}
