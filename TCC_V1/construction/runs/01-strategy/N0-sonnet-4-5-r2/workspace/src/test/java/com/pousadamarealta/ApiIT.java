package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ApiController;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiIT {

    private static Database database;
    private static RoomRepository roomRepository;
    private static ReservationRepository reservationRepository;
    private static MessageRepository messageRepository;
    private static ReservationService reservationService;
    private static ReportService reportService;

    @BeforeAll
    static void setup() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.out.println("Skipping integration tests - database not configured");
            Assumptions.assumeTrue(false);
            return;
        }

        try {
            database = new Database(dbUrl, dbUser, dbPassword);

            try (Connection conn = database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS reservations CASCADE");
                stmt.execute("DROP TABLE IF EXISTS rooms CASCADE");
                stmt.execute("DROP TABLE IF EXISTS messages CASCADE");
            }

            database.initialize();

            roomRepository = new RoomRepository(database);
            reservationRepository = new ReservationRepository(database);
            messageRepository = new MessageRepository(database);

            PricingService pricingService = new PricingService();
            reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                messageRepository,
                pricingService
            );
            reportService = new ReportService(reservationRepository);
        } catch (Exception e) {
            System.out.println("Database not available, skipping integration tests: " + e.getMessage());
            Assumptions.assumeTrue(false);
        }
    }

    @Test
    @Order(1)
    void testRoomsAreInitialized() {
        var rooms = roomRepository.findAll();
        assertEquals(5, rooms.size());

        var room101 = roomRepository.findByNumber("101").orElseThrow();
        assertEquals(RoomType.STANDARD, room101.getType());
        assertEquals(2, room101.getCapacity());
        assertEquals(new BigDecimal("249.90"), room101.getNightlyRate());
    }

    @Test
    @Order(2)
    void testCreateAndConfirmReservation() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setGuestName("Carla Dias");
        reservation.setGuestEmail("carla@email.com");
        reservation.setGuestCpf("11122233344");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        var created = reservationService.createReservation(reservation);
        assertNotNull(created.getId());
        assertEquals(ReservationStatus.PENDING, created.getStatus());
        assertEquals(2, created.getNights());
        assertEquals(0, new BigDecimal("779.80").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(created.getDiscount()));
        assertEquals(0, new BigDecimal("779.80").compareTo(created.getTotal()));

        var confirmed = reservationService.confirmReservation(created.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(0, confirmed.getAmountPaid().compareTo(confirmed.getTotal()));

        var messages = messageRepository.findAll();
        assertEquals(1, messages.size());
        assertEquals("EMAIL", messages.get(0).getChannel());
        assertEquals("carla@email.com", messages.get(0).getTo());
    }

    @Test
    @Order(3)
    void testCorporateReservation() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("202");
        reservation.setGuestName("Pedro Oliveira");
        reservation.setGuestEmail("pedro@email.com");
        reservation.setGuestCpf("11111111111");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.CORPORATE);
        reservation.setCompanyCode("TECNOSUL");

        var created = reservationService.createReservation(reservation);
        assertEquals(0, new BigDecimal("779.80").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("93.58").compareTo(created.getDiscount()));
        assertEquals(0, new BigDecimal("686.22").compareTo(created.getTotal()));

        reservationService.confirmReservation(created.getId());
    }

    @Test
    @Order(4)
    void testWeekendPackage() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setGuestName("Ana Costa");
        reservation.setGuestEmail("ana@email.com");
        reservation.setGuestCpf("22222222222");
        reservation.setCheckIn(LocalDate.of(2027, 3, 12));
        reservation.setCheckOut(LocalDate.of(2027, 3, 14));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.WEEKEND_PACKAGE);

        var created = reservationService.createReservation(reservation);
        assertEquals(0, new BigDecimal("820.00").compareTo(created.getTotal()));
    }

    @Test
    @Order(5)
    void testLongStay() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("Carlos Mendes");
        reservation.setGuestEmail("carlos@email.com");
        reservation.setGuestCpf("33333333333");
        reservation.setCheckIn(LocalDate.of(2027, 3, 1));
        reservation.setCheckOut(LocalDate.of(2027, 3, 8));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.LONG_STAY);

        var created = reservationService.createReservation(reservation);
        assertEquals(7, created.getNights());
        assertEquals(0, new BigDecimal("1749.30").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("174.93").compareTo(created.getDiscount()));
        assertEquals(0, new BigDecimal("1574.37").compareTo(created.getTotal()));
    }

    @Test
    @Order(6)
    void testReports() {
        String occupancy = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 10));
        assertTrue(occupancy.contains("room,type,reservation,check_in,check_out"));
        assertTrue(occupancy.contains("201,DELUXE"));
        assertTrue(occupancy.contains("202,DELUXE"));
        assertTrue(occupancy.contains("total,2/5"));

        String revenue = reportService.generateRevenueReport(java.time.YearMonth.of(2027, 3));
        assertTrue(revenue.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(revenue.contains("total,"));
    }
}
