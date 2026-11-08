package br.com.maralta.controller;

import br.com.maralta.domain.Reservation;
import br.com.maralta.domain.Room;
import br.com.maralta.repository.RoomRepository;
import br.com.maralta.service.InvalidRequestException;
import br.com.maralta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReportController {

    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReportController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void occupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isEmpty()) {
            throw new InvalidRequestException("Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Expected: yyyy-MM-dd");
        }

        List<Reservation> reservations = reservationService.getOccupancyByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType().name(),
                reservation.getId(),
                reservation.getCheckIn(),
                reservation.getCheckOut()
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv.toString());
    }

    public void revenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            throw new InvalidRequestException("Month parameter is required");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format. Expected: yyyy-MM");
        }

        List<Reservation> reservations = reservationService.getRevenueByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid().subtract(
                reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO
            );

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getStatus().name(),
                formatDecimal(amount)
            ));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s\n", formatDecimal(total)));

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv.toString());
    }

    private String formatDecimal(BigDecimal value) {
        return value.toString();
    }
}
