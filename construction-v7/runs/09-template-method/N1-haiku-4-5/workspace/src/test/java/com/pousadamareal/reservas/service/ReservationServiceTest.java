package com.pousadamareal.reservas.service;

import com.pousadamareal.reservas.dto.CreateReservationRequest;
import com.pousadamareal.reservas.dto.ReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService service;

    @BeforeEach
    public void setup() {
        service = new ReservationService();
    }

    @Test
    public void testCreateStandardReservationJuly() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "João Silva";
        request.guestEmail = "joao@example.com";
        request.guestCpf = "12345678901";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "STANDARD";

        ReservationResponse response = service.createReservation(request);

        assertEquals(1, response.id);
        assertEquals(2, response.nights);
        assertEquals(new BigDecimal("599.76"), response.subtotal);
        assertEquals(new BigDecimal("0"), response.discount);
        assertEquals(new BigDecimal("599.76"), response.total);
        assertEquals("PENDING", response.status);
    }

    @Test
    public void testCreateNonRefundableReservation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 201;
        request.guestName = "Maria Santos";
        request.guestEmail = "maria@example.com";
        request.guestCpf = "98765432101";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "NON_REFUNDABLE";

        ReservationResponse response = service.createReservation(request);

        assertEquals(2, response.nights);
        assertEquals(new BigDecimal("935.76"), response.subtotal);
        assertEquals(new BigDecimal("140.36"), response.discount);
        assertEquals(new BigDecimal("795.40"), response.total);
    }

    @Test
    public void testConfirmReservation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "João Silva";
        request.guestEmail = "joao@example.com";
        request.guestCpf = "12345678901";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "STANDARD";

        ReservationResponse created = service.createReservation(request);
        ReservationResponse confirmed = service.confirmReservation(created.id, new BigDecimal("599.76"));

        assertEquals("CONFIRMED", confirmed.status);
        assertEquals(new BigDecimal("599.76"), confirmed.amountPaid);
    }

    @Test
    public void testCheckInAndCheckOut() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "João Silva";
        request.guestEmail = "joao@example.com";
        request.guestCpf = "12345678901";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "STANDARD";

        ReservationResponse created = service.createReservation(request);
        service.confirmReservation(created.id, new BigDecimal("599.76"));

        ReservationResponse checkedIn = service.checkIn(created.id);
        assertEquals("CHECKED_IN", checkedIn.status);

        var document = service.checkOut(created.id);
        assertEquals("Comprovante de reserva", document.title);
    }

    @Test
    public void testCancelStandardReservation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "João Silva";
        request.guestEmail = "joao@example.com";
        request.guestCpf = "12345678901";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "STANDARD";

        ReservationResponse created = service.createReservation(request);
        service.confirmReservation(created.id, new BigDecimal("599.76"));

        ReservationResponse cancelled = service.cancelReservation(created.id);
        assertEquals("CANCELLED", cancelled.status);
        assertEquals(new BigDecimal("599.76"), cancelled.refund);
    }

    @Test
    public void testCancelNonRefundableReservation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 201;
        request.guestName = "Maria Santos";
        request.guestEmail = "maria@example.com";
        request.guestCpf = "98765432101";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "NON_REFUNDABLE";

        ReservationResponse created = service.createReservation(request);
        service.confirmReservation(created.id, new BigDecimal("662.83"));

        ReservationResponse cancelled = service.cancelReservation(created.id);
        assertEquals("CANCELLED", cancelled.status);
        assertEquals(new BigDecimal("0"), cancelled.refund);
    }

    @Test
    public void testRoomUnavailableOnDoubleBooking() {
        CreateReservationRequest request1 = new CreateReservationRequest();
        request1.roomNumber = 101;
        request1.guestName = "Guest 1";
        request1.guestEmail = "guest1@example.com";
        request1.guestCpf = "11111111111";
        request1.checkIn = LocalDate.of(2027, 7, 10);
        request1.checkOut = LocalDate.of(2027, 7, 12);
        request1.guests = 1;
        request1.ratePlan = "STANDARD";

        service.createReservation(request1);

        CreateReservationRequest request2 = new CreateReservationRequest();
        request2.roomNumber = 101;
        request2.guestName = "Guest 2";
        request2.guestEmail = "guest2@example.com";
        request2.guestCpf = "22222222222";
        request2.checkIn = LocalDate.of(2027, 7, 10);
        request2.checkOut = LocalDate.of(2027, 7, 12);
        request2.guests = 1;
        request2.ratePlan = "STANDARD";

        assertThrows(ApiException.class, () -> service.createReservation(request2));
    }

    @Test
    public void testRoomAvailableAfterCheckout() {
        CreateReservationRequest request1 = new CreateReservationRequest();
        request1.roomNumber = 101;
        request1.guestName = "Guest 1";
        request1.guestEmail = "guest1@example.com";
        request1.guestCpf = "11111111111";
        request1.checkIn = LocalDate.of(2027, 7, 10);
        request1.checkOut = LocalDate.of(2027, 7, 12);
        request1.guests = 1;
        request1.ratePlan = "STANDARD";

        service.createReservation(request1);

        CreateReservationRequest request2 = new CreateReservationRequest();
        request2.roomNumber = 101;
        request2.guestName = "Guest 2";
        request2.guestEmail = "guest2@example.com";
        request2.guestCpf = "22222222222";
        request2.checkIn = LocalDate.of(2027, 7, 12);
        request2.checkOut = LocalDate.of(2027, 7, 14);
        request2.guests = 1;
        request2.ratePlan = "STANDARD";

        assertDoesNotThrow(() -> service.createReservation(request2));
    }

    @Test
    public void testCapacityExceeded() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "Guest";
        request.guestEmail = "guest@example.com";
        request.guestCpf = "11111111111";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 3;
        request.ratePlan = "STANDARD";

        assertThrows(ApiException.class, () -> service.createReservation(request));
    }

    @Test
    public void testEmailSentOnConfirmation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.roomNumber = 101;
        request.guestName = "João Silva";
        request.guestEmail = "joao@example.com";
        request.guestCpf = "12345678901";
        request.checkIn = LocalDate.of(2027, 7, 10);
        request.checkOut = LocalDate.of(2027, 7, 12);
        request.guests = 2;
        request.ratePlan = "STANDARD";

        ReservationResponse created = service.createReservation(request);
        service.confirmReservation(created.id, new BigDecimal("599.76"));

        var messages = service.getMessages();
        assertEquals(1, messages.size());
        assertEquals("EMAIL", messages.get(0).channel);
        assertEquals("joao@example.com", messages.get(0).to);
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).subject);
        assertTrue(messages.get(0).body.contains("João Silva"));
    }
}
