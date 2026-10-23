package br.com.maralta;

import br.com.maralta.repository.*;
import br.com.maralta.service.*;
import br.com.maralta.model.*;
import br.com.maralta.util.DatabaseInitializer;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class ReservationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    private static DataSource dataSource;
    private static ReservationService reservationService;
    private static RoomRepository roomRepository;

    @BeforeAll
    static void setup() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        dataSource = ds;

        DatabaseInitializer.initialize(dataSource);

        roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        EmailService emailService = new EmailService(messageRepository);

        reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, documentService, emailService
        );
    }

    @Test
    void testCreateReservationWithCpf() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("João Silva");
        reservation.setGuestEmail("joao@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("123.456.789-00"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);

        assertNotNull(created.getId());
        assertEquals(ReservationStatus.PENDING, created.getStatus());
        assertEquals(2, created.getNights());
        assertEquals(new BigDecimal("499.80"), created.getSubtotal());
        assertEquals(BigDecimal.ZERO, created.getDiscount());
        assertEquals(new BigDecimal("499.80"), created.getTotal());
    }

    @Test
    void testCreateReservationWithCompany() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setGuestName("Ana Souza");
        reservation.setGuestEmail("ana@empresa.com");
        reservation.setIdentification(GuestIdentification.withCompany("12.345.678/0001-90", "Tecnosul Ltda"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);

        assertNotNull(created.getId());
        assertEquals("Tecnosul Ltda", created.getIdentification().getCompanyName());
    }

    @Test
    void testCreateReservationWithPassport() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("301");
        reservation.setGuestName("John Smith");
        reservation.setGuestEmail("john@email.com");
        reservation.setIdentification(GuestIdentification.withPassport("X1234567", "United Kingdom"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);

        assertNotNull(created.getId());
        assertEquals("United Kingdom", created.getIdentification().getGuestCountry());
    }

    @Test
    void testHighSeasonPricing() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("Maria Costa");
        reservation.setGuestEmail("maria@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("111.222.333-44"));
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);

        assertEquals(new BigDecimal("599.76"), created.getSubtotal());
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setGuestName("Pedro Alves");
        reservation.setGuestEmail("pedro@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("222.333.444-55"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Reservation created = reservationService.createReservation(reservation);

        assertEquals(new BigDecimal("779.80"), created.getSubtotal());
        assertEquals(new BigDecimal("116.97"), created.getDiscount());
        assertEquals(new BigDecimal("662.83"), created.getTotal());
    }

    @Test
    void testReservationLifecycle() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("102");
        reservation.setGuestName("Carlos Dias");
        reservation.setGuestEmail("carlos@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("333.444.555-66"));
        reservation.setCheckIn(LocalDate.of(2027, 4, 10));
        reservation.setCheckOut(LocalDate.of(2027, 4, 12));
        reservation.setGuests(1);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);
        assertEquals(ReservationStatus.PENDING, created.getStatus());

        Reservation confirmed = reservationService.confirmReservation(created.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());

        Reservation checkedIn = reservationService.checkIn(confirmed.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Document document = reservationService.checkOut(checkedIn.getId());
        assertNotNull(document);
        assertEquals("Comprovante de reserva", document.getTitle());
        assertTrue(document.getText().contains("Carlos Dias"));
    }

    @Test
    void testCancellationWithRefund() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("202");
        reservation.setGuestName("Lucia Ferreira");
        reservation.setGuestEmail("lucia@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("444.555.666-77"));
        reservation.setCheckIn(LocalDate.of(2027, 5, 10));
        reservation.setCheckOut(LocalDate.of(2027, 5, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);
        Reservation confirmed = reservationService.confirmReservation(created.getId());

        Reservation cancelled = reservationService.cancelReservation(confirmed.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(confirmed.getTotal(), cancelled.getRefund());
    }

    @Test
    void testCancellationNonRefundable() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("Roberto Santos");
        reservation.setGuestEmail("roberto@email.com");
        reservation.setIdentification(GuestIdentification.withCpf("555.666.777-88"));
        reservation.setCheckIn(LocalDate.of(2027, 6, 15));
        reservation.setCheckOut(LocalDate.of(2027, 6, 17));
        reservation.setGuests(1);
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Reservation created = reservationService.createReservation(reservation);
        Reservation confirmed = reservationService.confirmReservation(created.getId());

        Reservation cancelled = reservationService.cancelReservation(confirmed.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }
}
