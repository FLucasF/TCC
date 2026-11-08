package pousada.api;

import io.javalin.http.Context;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.repository.ReservationRepository;
import pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReportController {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportController(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            List<Reservation> reservations = reservationRepository.findByDate(date);
            List<Room> allRooms = roomRepository.findAll();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            for (Reservation reservation : reservations) {
                csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.roomNumber(),
                    reservation.roomType().name(),
                    reservation.id(),
                    reservation.checkIn(),
                    reservation.checkOut()
                ));
            }

            csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            List<Reservation> reservations = reservationRepository.findByMonth(year, month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;

            for (Reservation reservation : reservations) {
                BigDecimal revenue = reservation.amountPaid().subtract(
                    reservation.refund() != null ? reservation.refund() : BigDecimal.ZERO
                );

                if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                    csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                        reservation.id(),
                        reservation.roomNumber(),
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.status().name(),
                        formatCsvDecimal(revenue)
                    ));

                    total = total.add(revenue);
                }
            }

            csv.append(String.format("total,%s\n", formatCsvDecimal(total)));

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());

        } catch (NumberFormatException | DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private String formatCsvDecimal(BigDecimal value) {
        return value.toString();
    }
}
