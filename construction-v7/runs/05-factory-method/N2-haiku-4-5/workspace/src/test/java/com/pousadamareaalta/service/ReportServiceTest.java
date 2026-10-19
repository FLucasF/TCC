package com.pousadamareaalta.service;

import com.pousadamareaalta.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ReportServiceTest {
    private ReportService reportService;
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    @BeforeEach
    void setup() {
        roomRepository = new RoomRepository();
        reservationRepository = new ReservationRepository();
        reportService = new ReportService(reservationRepository, roomRepository);
    }

    @Test
    void shouldGenerateOccupancyReport() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        Reservation r2 = reservationService.createReservation(
            201, "Guest 2", "guest2@example.com", "222.222.222-22", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r2.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertThat(report).contains("room,type,reservation,check_in,check_out");
        assertThat(report).contains("101,STANDARD");
        assertThat(report).contains("201,DELUXE");
        assertThat(report).contains("total,2/5");
    }

    @Test
    void shouldNotIncludePendingReservationsInOccupancy() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertThat(report).doesNotContain("101,STANDARD");
        assertThat(report).contains("total,0/5");
    }

    @Test
    void shouldNotIncludeCancelledReservationsInOccupancy() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());
        reservationService.cancelReservation(r1.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertThat(report).doesNotContain("101,STANDARD");
        assertThat(report).contains("total,0/5");
    }

    @Test
    void shouldGenerateRevenueReport() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        String report = reportService.generateRevenueReport("2027-07");

        assertThat(report).contains("reservation,room,check_in,check_out,status,amount");
        assertThat(report).contains("1,101,2027-07-10,2027-07-12,CONFIRMED");
        assertThat(report).contains("total");
    }

    @Test
    void shouldIncludeCancelledNonRefundableInRevenue() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(r1.getId());
        reservationService.cancelReservation(r1.getId());

        String report = reportService.generateRevenueReport("2027-07");

        assertThat(report).contains("1,101");
        assertThat(report).contains("CANCELLED");
    }

    @Test
    void shouldNotIncludePendingReservationsInRevenue() throws ReservationService.ValidationException {
        var checkIn = LocalDate.of(2027, 7, 10);
        var checkOut = LocalDate.of(2027, 7, 12);

        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService,
            documentService
        );

        Reservation r1 = reservationService.createReservation(
            101, "Guest 1", "guest1@example.com", "111.111.111-11", null, null, null, null,
            checkIn, checkOut, 1, RatePlan.STANDARD
        );

        String report = reportService.generateRevenueReport("2027-07");

        assertThat(report).doesNotContain("1,101");
    }
}
