package br.com.mareaalta.service;

import br.com.mareaalta.domain.*;
import br.com.mareaalta.domain.rateplan.*;
import br.com.mareaalta.repository.Database;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private final Database db;

    public ReservationService(Database db) {
        this.db = db;
    }

    public Result<Reservation> createReservation(int roomNumber, String guestName, String guestEmail,
                                                   String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                                   int guests, RatePlanType ratePlanType, String companyCode) {
        try {
            Room room = db.getRoomByNumber(roomNumber);
            if (room == null) {
                return Result.error("ROOM_NOT_FOUND", "Quarto não encontrado");
            }

            RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
            RatePlan.ValidationError validationError = ratePlan.validate(checkIn, checkOut, guests, room, companyCode);
            if (validationError != null) {
                return Result.error(validationError.code(), validationError.message());
            }

            if (!db.isRoomAvailable(roomNumber, checkIn, checkOut)) {
                return Result.error("ROOM_UNAVAILABLE", "Quarto não disponível para as datas");
            }

            int nights = DateUtils.nightsBetween(checkIn, checkOut);

            RatePlan.PricingResult pricing;
            if (ratePlanType == RatePlanType.CORPORATE) {
                CorporateRatePlan corpPlan = (CorporateRatePlan) ratePlan;
                pricing = corpPlan.calculatePriceWithCompany(checkIn, checkOut, room, companyCode);
            } else {
                pricing = ratePlan.calculatePrice(checkIn, checkOut, room);
            }

            Reservation res = new Reservation();
            res.setRoomNumber(roomNumber);
            res.setGuestName(guestName);
            res.setGuestEmail(guestEmail);
            res.setGuestCpf(guestCpf);
            res.setCheckIn(checkIn);
            res.setCheckOut(checkOut);
            res.setGuests(guests);
            res.setRatePlan(ratePlanType);
            res.setCompanyCode(companyCode);
            res.setNights(nights);
            res.setSubtotal(pricing.subtotal());
            res.setDiscount(pricing.discount());
            res.setTotal(pricing.total());
            res.setStatus(ReservationStatus.PENDING);
            res.setAmountPaid(0.0);

            Long id = db.createReservation(res);
            res.setId(id);

            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    public Result<Reservation> getReservation(Long id) {
        try {
            Reservation res = db.getReservationById(id);
            if (res == null) {
                return Result.error("RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }
            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    public Result<Reservation> confirmReservation(Long id, double amountPaid) {
        try {
            Reservation res = db.getReservationById(id);
            if (res == null) {
                return Result.error("RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }

            if (res.getStatus() != ReservationStatus.PENDING) {
                return Result.error("OPERATION_NOT_ALLOWED", "Reserva não está pendente");
            }

            res.setStatus(ReservationStatus.CONFIRMED);
            res.setAmountPaid(Money.round(amountPaid));
            db.updateReservation(res);

            sendConfirmationEmail(res);

            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    public Result<Reservation> checkIn(Long id) {
        try {
            Reservation res = db.getReservationById(id);
            if (res == null) {
                return Result.error("RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }

            if (res.getStatus() != ReservationStatus.CONFIRMED) {
                return Result.error("OPERATION_NOT_ALLOWED", "Reserva não está confirmada");
            }

            res.setStatus(ReservationStatus.CHECKED_IN);
            db.updateReservation(res);

            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    public Result<Reservation> checkOut(Long id) {
        try {
            Reservation res = db.getReservationById(id);
            if (res == null) {
                return Result.error("RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }

            if (res.getStatus() != ReservationStatus.CHECKED_IN) {
                return Result.error("OPERATION_NOT_ALLOWED", "Hóspede não fez check-in");
            }

            res.setStatus(ReservationStatus.CHECKED_OUT);
            db.updateReservation(res);

            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    public Result<Reservation> cancelReservation(Long id) {
        try {
            Reservation res = db.getReservationById(id);
            if (res == null) {
                return Result.error("RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }

            if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
                return Result.error("OPERATION_NOT_ALLOWED", "Reserva não pode ser cancelada");
            }

            res.setStatus(ReservationStatus.CANCELLED);

            RatePlan ratePlan = RatePlanFactory.create(res.getRatePlan());
            double refundPercentage = ratePlan.getRefundPercentage();
            double refundAmount = Money.round(res.getAmountPaid() * (refundPercentage / 100.0));
            res.setRefund(refundAmount);

            db.updateReservation(res);

            return Result.success(res);
        } catch (SQLException e) {
            return Result.error("DATABASE_ERROR", e.getMessage());
        }
    }

    private void sendConfirmationEmail(Reservation res) throws SQLException {
        Room room = db.getRoomByNumber(res.getRoomNumber());
        String receipt = generateReceipt(res, room);

        String body = "Olá, " + res.getGuestName() + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receipt + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        Message msg = new Message("EMAIL", res.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);

        db.saveMessage(msg);
    }

    public String generateReceipt(Reservation res, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.getId()).append("\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append(" (CPF ").append(res.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(res.getRoomNumber()).append(" (").append(room.getType().getDisplayName()).append(")\n");

        String checkInStr = DateUtils.formatBr(res.getCheckIn());
        String checkOutStr = DateUtils.formatBr(res.getCheckOut());
        String nightsStr = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";
        sb.append("Entrada: ").append(checkInStr).append(" | Saída: ").append(checkOutStr).append(" | ").append(nightsStr).append("\n");
        sb.append("Diárias: ").append(Money.formatBr(res.getSubtotal())).append("\n");

        if (res.getDiscount() > 0) {
            sb.append("Desconto: ").append(Money.formatBr(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Money.formatBr(res.getTotal()));

        return sb.toString();
    }

    public List<Room> getAllRooms() throws SQLException {
        return db.getAllRooms();
    }

    public List<Message> getAllMessages() throws SQLException {
        return db.getAllMessages();
    }

    public String getOccupancyReport(LocalDate date) throws SQLException {
        List<Reservation> reservations = db.getReservationsForOccupancy(date);
        List<Room> allRooms = db.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        Set<Integer> occupiedRooms = new HashSet<>();
        for (Reservation res : reservations) {
            occupiedRooms.add(res.getRoomNumber());
            Room room = db.getRoomByNumber(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",")
                    .append(room.getType().getDisplayName()).append(",")
                    .append(res.getId()).append(",")
                    .append(DateUtils.format(res.getCheckIn())).append(",")
                    .append(DateUtils.format(res.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupiedRooms.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String getRevenueReport(String monthStr) throws SQLException {
        var month = DateUtils.parseYearMonth(monthStr);
        if (month == null) {
            return null;
        }

        List<Reservation> reservations = db.getReservationsForRevenue(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0.0;
        for (Reservation res : reservations) {
            double amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount -= res.getRefund();
            }

            csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(DateUtils.format(res.getCheckIn())).append(",")
                    .append(DateUtils.format(res.getCheckOut())).append(",")
                    .append(res.getStatus()).append(",")
                    .append(Money.format(amount)).append("\n");

            totalAmount = Money.round(totalAmount + amount);
        }

        csv.append("total,,,,").append(Money.format(totalAmount));

        return csv.toString();
    }

    public static class Result<T> {
        private final T data;
        private final String errorCode;
        private final String errorMessage;
        private final boolean success;

        private Result(T data, String errorCode, String errorMessage) {
            this.data = data;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.success = errorCode == null;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(data, null, null);
        }

        public static <T> Result<T> error(String code, String message) {
            return new Result<>(null, code, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
