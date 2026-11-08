package com.pousadamarealta.handler;

import io.javalin.http.Context;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.util.Formatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportHandler {
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    public ReportHandler(Database database) {
        this.reservationRepository = new ReservationRepository(database);
        this.roomRepository = new RoomRepository(database);
    }

    public void occupancy(Context ctx) throws Exception {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            List<Room> rooms = roomRepository.findAll();
            StringBuilder csv = new StringBuilder();

            csv.append("Pousada Maré Alta,occupancy,").append(Formatter.formatIsoDate(date)).append("\n");
            csv.append("room,type,reservation,check_in,check_out\n");

            int occupiedCount = 0;
            for (Room room : rooms) {
                List<Reservation> reservations = reservationRepository.findByRoomAndDate(room.getNumber(), date);
                if (!reservations.isEmpty()) {
                    for (Reservation reservation : reservations) {
                        if (isPaidReservation(reservation)) {
                            csv.append(room.getNumber()).append(",");
                            csv.append(room.getType()).append(",");
                            csv.append(reservation.getId()).append(",");
                            csv.append(Formatter.formatIsoDate(reservation.getCheckIn())).append(",");
                            csv.append(Formatter.formatIsoDate(reservation.getCheckOut())).append("\n");
                            occupiedCount++;
                        }
                    }
                }
            }

            csv.append("total,").append(occupiedCount).append("/").append(rooms.size()).append("\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void revenue(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            List<Reservation> reservations = reservationRepository.findPaidByMonth(yearMonth.getYear(), yearMonth.getMonthValue());

            StringBuilder csv = new StringBuilder();
            csv.append("Pousada Maré Alta,revenue,").append(monthStr).append("\n");
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;
            for (Reservation reservation : reservations) {
                BigDecimal amount = reservation.getAmountPaid().subtract(reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO);
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(Formatter.formatIsoDate(reservation.getCheckIn())).append(",");
                csv.append(Formatter.formatIsoDate(reservation.getCheckOut())).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(Formatter.formatCsvCurrency(amount)).append("\n");
                total = total.add(amount);
            }

            csv.append("total,,,,").append(Formatter.formatCsvCurrency(total)).append("\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void tourismTax(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            List<Reservation> reservations = reservationRepository.findPaidByMonth(yearMonth.getYear(), yearMonth.getMonthValue());

            StringBuilder csv = new StringBuilder();
            csv.append("Pousada Maré Alta,tourism-tax,").append(monthStr).append("\n");
            csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

            BigDecimal tourismTaxRate = new BigDecimal("4.50");
            BigDecimal total = BigDecimal.ZERO;

            for (Reservation reservation : reservations) {
                BigDecimal tax = BigDecimal.valueOf(reservation.getGuests())
                        .multiply(BigDecimal.valueOf(reservation.getNights()))
                        .multiply(tourismTaxRate);
                tax = tax.setScale(2, java.math.RoundingMode.HALF_EVEN);

                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(Formatter.formatIsoDate(reservation.getCheckIn())).append(",");
                csv.append(Formatter.formatIsoDate(reservation.getCheckOut())).append(",");
                csv.append(reservation.getGuests()).append(",");
                csv.append(reservation.getNights()).append(",");
                csv.append(Formatter.formatCsvCurrency(tax)).append("\n");
                total = total.add(tax);
            }

            csv.append("total,,,,,").append(Formatter.formatCsvCurrency(total)).append("\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void ratePlans(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            List<Reservation> reservations = reservationRepository.findPaidByMonth(yearMonth.getYear(), yearMonth.getMonthValue());

            Map<RatePlan, List<Reservation>> grouped = reservations.stream()
                    .collect(Collectors.groupingBy(Reservation::getRatePlan));

            StringBuilder csv = new StringBuilder();
            csv.append("Pousada Maré Alta,rate-plans,").append(monthStr).append("\n");
            csv.append("rate_plan,reservations,amount\n");

            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalReservations = 0;

            for (RatePlan plan : new RatePlan[]{RatePlan.STANDARD, RatePlan.NON_REFUNDABLE}) {
                List<Reservation> planReservations = grouped.getOrDefault(plan, List.of());
                BigDecimal amount = BigDecimal.ZERO;

                for (Reservation reservation : planReservations) {
                    BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
                    amount = amount.add(reservation.getAmountPaid().subtract(refund));
                }

                csv.append(plan).append(",");
                csv.append(planReservations.size()).append(",");
                csv.append(Formatter.formatCsvCurrency(amount)).append("\n");

                totalAmount = totalAmount.add(amount);
                totalReservations += planReservations.size();
            }

            csv.append("total,").append(totalReservations).append(",").append(Formatter.formatCsvCurrency(totalAmount)).append("\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    private boolean isPaidReservation(Reservation reservation) {
        String status = reservation.getStatus().name();
        return status.equals("CONFIRMED") || status.equals("CHECKED_IN") || status.equals("CHECKED_OUT");
    }
}
