package com.pousada;

import com.pousada.domain.*;
import com.pousada.domain.rateplan.RatePlanFactory;
import com.pousada.repository.Database;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.DocumentService;
import com.pousada.service.PricingService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationIT {
    private static Database database;
    private static RoomRepository roomRepository;
    private static ReservationRepository reservationRepository;
    private static MessageRepository messageRepository;
    private static PricingService pricingService;
    private static DocumentService documentService;

    @BeforeAll
    static void setUp() {
        database = new Database(
            "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        database.initializeSchema();

        roomRepository = new RoomRepository(database);
        reservationRepository = new ReservationRepository(database);
        messageRepository = new MessageRepository(database);
        pricingService = new PricingService();
        documentService = new DocumentService();

        roomRepository.save(new Room("101", RoomType.STANDARD));
        roomRepository.save(new Room("102", RoomType.STANDARD));
        roomRepository.save(new Room("201", RoomType.DELUXE));
        roomRepository.save(new Room("202", RoomType.DELUXE));
        roomRepository.save(new Room("301", RoomType.SUITE));
    }

    @Test
    @Order(1)
    void shouldSaveAndFindRooms() {
        var rooms = roomRepository.findAll();
        assertEquals(5, rooms.size());
        assertEquals("101", rooms.get(0).getNumber());
        assertEquals(RoomType.STANDARD, rooms.get(0).getType());
    }

    @Test
    @Order(2)
    void shouldCreateReservation() {
        Room room = roomRepository.findByNumber("101").orElseThrow();
        var ratePlan = RatePlanFactory.get("STANDARD");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        var subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        var discount = pricingService.calculateDiscount(subtotal, ratePlan);
        var total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null, "101", "João Silva", "joao@example.com", "12345678901",
            checkIn, checkOut, 2, ratePlan, 2, subtotal, discount, total
        );

        reservationRepository.save(reservation);

        assertNotNull(reservation.getId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    @Order(3)
    void shouldConfirmReservationAndSendEmail() {
        Reservation reservation = reservationRepository.findById(1L).orElseThrow();
        reservation.confirm();
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = documentService.createDocument(reservation, room);
        String documentText = documentService.formatDocument(document);
        String emailBody = documentService.createConfirmationEmail(reservation.getGuestName(), documentText);

        Message message = new Message(
            null, "EMAIL", reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta", emailBody
        );
        messageRepository.save(message);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getAmountPaid());

        var messages = messageRepository.findAll();
        assertEquals(1, messages.size());
        assertEquals("EMAIL", messages.get(0).getChannel());
        assertTrue(messages.get(0).getBody().contains("João Silva"));
    }

    @Test
    @Order(4)
    void shouldDetectRoomConflict() {
        boolean hasConflict = reservationRepository.hasConflict(
            "101",
            LocalDate.of(2027, 7, 11),
            LocalDate.of(2027, 7, 13),
            null
        );

        assertTrue(hasConflict);
    }

    @Test
    @Order(5)
    void shouldCheckInAndCheckOut() {
        Reservation reservation = reservationRepository.findById(1L).orElseThrow();

        reservation.checkIn();
        reservationRepository.update(reservation);
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        reservationRepository.update(reservation);
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    @Order(6)
    void shouldCreateNonRefundableReservation() {
        Room room = roomRepository.findByNumber("201").orElseThrow();
        var ratePlan = RatePlanFactory.get("NON_REFUNDABLE");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        var discount = pricingService.calculateDiscount(subtotal, ratePlan);
        var total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null, "201", "Maria Santos", "maria@example.com", "98765432109",
            checkIn, checkOut, 2, ratePlan, 2, subtotal, discount, total
        );

        reservationRepository.save(reservation);

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("116.97"), reservation.getDiscount());
        assertEquals(new BigDecimal("662.83"), reservation.getTotal());
    }

    @Test
    @Order(7)
    void shouldCancelWithNoRefund() {
        Reservation reservation = reservationRepository.findById(2L).orElseThrow();
        reservation.confirm();
        reservationRepository.update(reservation);

        reservation.cancel();
        reservationRepository.update(reservation);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    @Order(8)
    void shouldGenerateDocument() {
        Reservation reservation = reservationRepository.findById(1L).orElseThrow();
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Document document = documentService.createDocument(reservation, room);
        String formatted = documentService.formatDocument(document);

        assertTrue(formatted.contains("Comprovante de reserva nº 1"));
        assertTrue(formatted.contains("João Silva"));
        assertTrue(formatted.contains("CPF 123.456.789-01"));
        assertTrue(formatted.contains("Quarto: 101 (STANDARD)"));
        assertTrue(formatted.contains("2 diárias"));
        assertTrue(formatted.contains("R$ 599,76"));
    }
}
