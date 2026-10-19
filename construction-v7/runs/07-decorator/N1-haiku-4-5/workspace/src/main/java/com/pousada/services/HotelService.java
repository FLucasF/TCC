package com.pousada.services;

import com.pousada.models.Message;
import com.pousada.models.RatePlan;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class HotelService {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReceiptService receiptService;
    private final ReportService reportService;

    public HotelService(boolean testMode) {
        this.roomService = new RoomService();
        this.reservationService = new ReservationService(roomService);
        this.messageService = new MessageService(testMode);
        this.receiptService = new ReceiptService(roomService);
        this.reportService = new ReportService(reservationService, roomService);
    }

    public Map<Integer, Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    public Reservation createReservation(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) throws IllegalArgumentException {
        return reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);
    }

    public Reservation getReservation(long id) {
        return reservationService.getReservation(id);
    }

    public Reservation confirmReservation(long id) throws IllegalArgumentException {
        Reservation reservation = reservationService.getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        reservationService.confirmReservation(id, reservation.getTotal());

        String receipt = receiptService.generateReceiptForEmail(reservation);
        String emailBody = "Olá, " + reservation.getGuestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";

        messageService.sendEmail(
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody);

        return reservationService.getReservation(id);
    }

    public Reservation checkInReservation(long id) throws IllegalArgumentException {
        reservationService.checkInReservation(id);
        return reservationService.getReservation(id);
    }

    public Reservation checkOutReservation(long id) throws IllegalArgumentException {
        reservationService.checkOutReservation(id);
        return reservationService.getReservation(id);
    }

    public Reservation cancelReservation(long id) throws IllegalArgumentException {
        reservationService.cancelReservation(id);
        return reservationService.getReservation(id);
    }

    public String getReceipt(long id) throws IllegalArgumentException {
        Reservation reservation = reservationService.getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return receiptService.generateReceipt(reservation);
    }

    public List<Message> getMessages() {
        return messageService.getMessages();
    }

    public String getOccupancyReport(LocalDate date) {
        return reportService.generateOccupancyReport(date);
    }

    public String getRevenueReport(int year, int month) {
        return reportService.generateRevenueReport(year, month);
    }
}
