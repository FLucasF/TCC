package com.pousadamarealta;

import com.pousadamarealta.messaging.ArchiveCopyRule;
import com.pousadamarealta.messaging.EmailValidationRule;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    @Test
    void endToEndReservationFlow() {
        RoomService roomService = new RoomService();
        ReservationService reservationService = new ReservationService(roomService);
        MessageProcessor messageProcessor = new MessageProcessor(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule("arquivo@pousadamarealta.com.br", 1000L)
        ));
        MessageService messageService = new MessageService(messageProcessor);
        ReportService reportService = new ReportService(reservationService, roomService);

        assertEquals(5, roomService.getAllRooms().size());

        var reservation = reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, com.pousadamarealta.domain.RatePlan.STANDARD
        );

        assertEquals(1, reservation.id());
        assertEquals(com.pousadamarealta.domain.ReservationStatus.PENDING, reservation.status());

        reservationService.confirm(1);
        var confirmedReservation = reservationService.getById(1);
        assertEquals(com.pousadamarealta.domain.ReservationStatus.CONFIRMED, confirmedReservation.status());

        var document = com.pousadamarealta.domain.Document.fromReservation(confirmedReservation);
        messageService.sendConfirmationEmail(
            confirmedReservation.guestEmail(),
            confirmedReservation.guestName(),
            document
        );

        var messages = messageService.getAllMessages();
        assertEquals(2, messages.size());
        assertEquals("joao@example.com", messages.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());

        String occupancyReport = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));
        assertTrue(occupancyReport.contains("101,STANDARD,1"));

        String revenueReport = reportService.generateRevenueReport(2027, 7);
        assertTrue(revenueReport.contains("1,101,2027-07-10,2027-07-12"));
    }
}
