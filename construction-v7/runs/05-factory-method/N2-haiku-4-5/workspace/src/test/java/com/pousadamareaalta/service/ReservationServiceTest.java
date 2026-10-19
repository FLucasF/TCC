package com.pousadamareaalta.service;

import com.pousadamareaalta.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ReservationServiceTest {
    private ReservationService reservationService;
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private MessageService messageService;
    private DocumentService documentService;

    @BeforeEach
    void setup() {
        roomRepository = new RoomRepository();
        reservationRepository = new ReservationRepository();
        messageService = new MessageService();
        documentService = new DocumentService();
        reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );
    }

    @Test
    void shouldCreateReservationWithCpf() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            201,
            "Carla Dias",
            "carla@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            2,
            RatePlan.STANDARD
        );

        assertThat(reservation).isNotNull();
        assertThat(reservation.getId()).isEqualTo(1);
        assertThat(reservation.getRoomNumber()).isEqualTo(201);
        assertThat(reservation.getGuestName()).isEqualTo("Carla Dias");
        assertThat(reservation.getGuestCpf()).isEqualTo("111.222.333-44");
        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void shouldCalculatePriceCorrectlyInHighSeason() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "123.456.789-00",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.STANDARD
        );

        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldApplyNonRefundableDiscount() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            201,
            "Jane Doe",
            "jane@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.NON_REFUNDABLE
        );

        BigDecimal expectedSubtotal = new BigDecimal("779.80");
        BigDecimal expectedDiscount = new BigDecimal("116.97");
        BigDecimal expectedTotal = new BigDecimal("662.83");

        assertThat(reservation.getSubtotal()).isEqualByComparingTo(expectedSubtotal);
        assertThat(reservation.getDiscount()).isEqualByComparingTo(expectedDiscount);
        assertThat(reservation.getTotal()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void shouldRejectGuestCountExceedingCapacity() {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        assertThatThrownBy(() -> reservationService.createReservation(
            101,
            "John",
            "john@example.com",
            "123.456.789-00",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            3,
            RatePlan.STANDARD
        )).isInstanceOf(ReservationService.ValidationException.class)
            .hasFieldOrPropertyWithValue("errorCode", "CAPACITY_EXCEEDED");
    }

    @Test
    void shouldRejectRoomNotFound() {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        assertThatThrownBy(() -> reservationService.createReservation(
            999,
            "John",
            "john@example.com",
            "123.456.789-00",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.STANDARD
        )).isInstanceOf(ReservationService.ValidationException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ROOM_NOT_FOUND");
    }

    @Test
    void shouldRejectOverlappingDates() throws ReservationService.ValidationException {
        var checkIn1 = LocalDate.of(2027, 3, 10);
        var checkOut1 = LocalDate.of(2027, 3, 15);
        var checkIn2 = LocalDate.of(2027, 3, 12);
        var checkOut2 = LocalDate.of(2027, 3, 18);

        reservationService.createReservation(
            201,
            "Guest 1",
            "guest1@example.com",
            "111.111.111-11",
            null,
            null,
            null,
            null,
            checkIn1,
            checkOut1,
            1,
            RatePlan.STANDARD
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            201,
            "Guest 2",
            "guest2@example.com",
            "222.222.222-22",
            null,
            null,
            null,
            null,
            checkIn2,
            checkOut2,
            1,
            RatePlan.STANDARD
        )).isInstanceOf(ReservationService.ValidationException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ROOM_UNAVAILABLE");
    }

    @Test
    void shouldConfirmReservation() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation created = reservationService.createReservation(
            201,
            "Carla Dias",
            "carla@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(created.getId());

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualByComparingTo(created.getTotal());

        var messages = messageService.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).to()).isEqualTo("carla@example.com");
    }

    @Test
    void shouldNotConfirmNonPendingReservation() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation created = reservationService.createReservation(
            201,
            "Carla Dias",
            "carla@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(created.getId());

        assertThatThrownBy(() -> reservationService.confirmReservation(created.getId()))
            .isInstanceOf(ReservationService.ValidationException.class)
            .hasFieldOrPropertyWithValue("errorCode", "OPERATION_NOT_ALLOWED");
    }

    @Test
    void shouldCancelConfirmedReservationWithRefund() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation created = reservationService.createReservation(
            201,
            "Carla Dias",
            "carla@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(created.getId());
        Reservation cancelled = reservationService.cancelReservation(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(confirmed.getTotal());
    }

    @Test
    void shouldCancelNonRefundableReservationWithoutRefund() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation created = reservationService.createReservation(
            201,
            "Jane Doe",
            "jane@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(created.getId());
        Reservation cancelled = reservationService.cancelReservation(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRequireExactlyOneIdentificationType() {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        assertThatThrownBy(() -> reservationService.createReservation(
            201,
            "John",
            "john@example.com",
            null,
            null,
            null,
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.STANDARD
        )).isInstanceOf(ReservationService.ValidationException.class)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
    }

    @Test
    void shouldCreateReservationWithCompanyIdentification() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            201,
            "Ana Souza",
            "ana@example.com",
            null,
            "12.345.678/0001-90",
            "Tecnosul Ltda",
            null,
            null,
            checkIn,
            checkOut,
            1,
            RatePlan.STANDARD
        );

        assertThat(reservation.getCompanyCnpj()).isEqualTo("12.345.678/0001-90");
        assertThat(reservation.getCompanyName()).isEqualTo("Tecnosul Ltda");
    }

    @Test
    void shouldCreateReservationWithPassportIdentification() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            201,
            "John Smith",
            "john@example.com",
            null,
            null,
            null,
            "X1234567",
            "United Kingdom",
            checkIn,
            checkOut,
            1,
            RatePlan.STANDARD
        );

        assertThat(reservation.getGuestPassport()).isEqualTo("X1234567");
        assertThat(reservation.getGuestCountry()).isEqualTo("United Kingdom");
    }
}
