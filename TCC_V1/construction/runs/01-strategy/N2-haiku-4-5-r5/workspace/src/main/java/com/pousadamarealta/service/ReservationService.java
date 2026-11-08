package com.pousadamarealta.service;

import com.pousadamarealta.api.ApiException;
import com.pousadamarealta.database.*;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.pricing.*;
import com.pousadamarealta.api.request.CreateReservationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.util.List;

public class ReservationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Reservation createReservation(CreateReservationRequest request) {
        try {
            validateRequest(request);

            LocalDate checkIn = LocalDate.parse(request.getCheckIn(), DATE_FORMAT);
            LocalDate checkOut = LocalDate.parse(request.getCheckOut(), DATE_FORMAT);
            RatePlanType ratePlanType = RatePlanType.valueOf(request.getRatePlan());

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
            }

            if (request.getGuests() < 1) {
                throw new ApiException(400, "INVALID_REQUEST", "At least 1 guest is required");
            }

            Room room = RoomDao.getRoomByNumber(request.getRoomNumber());
            if (room == null) {
                throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
            }

            if (request.getGuests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED", "Guest count exceeds room capacity");
            }

            RatePlan ratePlan = RatePlanFactory.createRatePlan(ratePlanType);
            ratePlan.validate(request.getRoomNumber(), checkIn, checkOut, request.getGuests(), request.getCompanyCode());

            checkRoomAvailability(request.getRoomNumber(), checkIn, checkOut);

            PriceCalculation price = ratePlan.calculatePrice(room, checkIn, checkOut);

            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

            long id = ReservationDao.createReservation(
                request.getRoomNumber(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getGuestCpf(),
                checkIn,
                checkOut,
                request.getGuests(),
                ratePlanType,
                request.getCompanyCode(),
                nights,
                price.subtotal(),
                price.discount(),
                price.total()
            );

            return ReservationDao.getReservationById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    public static Reservation confirmReservation(long id) {
        try {
            Reservation res = ReservationDao.getReservationById(id);
            if (res == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            if (res.getStatus() != ReservationStatus.PENDING) {
                throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
            }

            ReservationDao.updateReservationStatus(id, ReservationStatus.CONFIRMED);
            ReservationDao.updateReservationAmountPaid(id, res.getTotal());

            sendConfirmationEmail(res);

            return ReservationDao.getReservationById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    public static Reservation checkIn(long id) {
        try {
            Reservation res = ReservationDao.getReservationById(id);
            if (res == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            if (res.getStatus() != ReservationStatus.CONFIRMED) {
                throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
            }

            ReservationDao.updateReservationStatus(id, ReservationStatus.CHECKED_IN);

            return ReservationDao.getReservationById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    public static Reservation checkOut(long id) {
        try {
            Reservation res = ReservationDao.getReservationById(id);
            if (res == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            if (res.getStatus() != ReservationStatus.CHECKED_IN) {
                throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
            }

            ReservationDao.updateReservationStatus(id, ReservationStatus.CHECKED_OUT);

            return ReservationDao.getReservationById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    public static Reservation cancelReservation(long id) {
        try {
            Reservation res = ReservationDao.getReservationById(id);
            if (res == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            ReservationStatus status = res.getStatus();
            if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
                throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending or confirmed reservations can be cancelled");
            }

            RatePlan ratePlan = RatePlanFactory.createRatePlan(res.getRatePlan());
            BigDecimal refund = BigDecimal.ZERO;

            if (res.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                refund = ratePlan.calculateRefund(res.getAmountPaid());
            }

            ReservationDao.updateReservationStatus(id, ReservationStatus.CANCELLED);
            ReservationDao.updateReservationRefund(id, refund);

            return ReservationDao.getReservationById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    public static Reservation getReservation(long id) {
        try {
            Reservation res = ReservationDao.getReservationById(id);
            if (res == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            return res;
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    private static void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try {
            List<Reservation> existing = ReservationDao.getReservationsByRoomAndDateRange(roomNumber, checkIn, checkOut);
            if (!existing.isEmpty()) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for the requested dates");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (ApiException e) {
            throw e;
        }
    }

    private static void sendConfirmationEmail(Reservation res) {
        try {
            Room room = RoomDao.getRoomByNumber(res.getRoomNumber());

            String voucherText = formatVoucher(res, room.type().toString());
            String emailBody = "Olá, " + res.getGuestName() + "!\n\n" +
                    "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                    voucherText + "\n\n" +
                    "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                    "Até breve!";

            MessageDao.createMessage(
                "EMAIL",
                res.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody
            );
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    private static String formatVoucher(Reservation res, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.getId()).append("\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append(" (CPF ").append(formatCpf(res.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(res.getRoomNumber()).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(res.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(res.getCheckOut())).append(" | ");
        sb.append(res.getNights()).append(res.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(res.getSubtotal())).append("\n");

        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(res.getTotal())).append("\n");

        return sb.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9)
        );
    }

    private static void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == 0 || request.getGuestName() == null || request.getGuestEmail() == null ||
            request.getGuestCpf() == null || request.getCheckIn() == null || request.getCheckOut() == null ||
            request.getRatePlan() == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
        }

        try {
            LocalDate.parse(request.getCheckIn(), DATE_FORMAT);
            LocalDate.parse(request.getCheckOut(), DATE_FORMAT);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
        }

        try {
            RatePlanType.valueOf(request.getRatePlan());
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan");
        }
    }
}
