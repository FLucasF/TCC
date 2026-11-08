package com.pousada;

import io.javalin.http.HttpStatus;

import java.sql.*;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation create(CreateReservationRequest req) {
        try {
            LocalDate checkIn = DateUtils.parseIsoDate(req.checkIn);
            LocalDate checkOut = DateUtils.parseIsoDate(req.checkOut);

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Check-out deve ser após check-in");
            }

            if (req.guests < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Deve ter pelo menos 1 hóspede");
            }

            Room room = RoomService.findByNumber(req.roomNumber);
            if (room == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Quarto não encontrado");
            }

            if (req.guests > room.capacity) {
                throw new ApiException(422, "CAPACITY_EXCEEDED", "Quarto comporta menos hóspedes");
            }

            if (isRoomUnavailable(req.roomNumber, checkIn, checkOut)) {
                throw new ApiException(HttpStatus.CONFLICT, "ROOM_UNAVAILABLE", "Quarto não disponível nesse período");
            }

            RatePlan ratePlan = RatePlan.fromString(req.ratePlan);
            var prices = PriceCalculator.calculate(checkIn, checkOut, room.nightlyRate, ratePlan);

            try (Connection conn = Database.getConnection()) {
                String query = String.format(
                    "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                    "VALUES (%d, '%s', '%s', '%s', '%s', '%s', %d, '%s', %d, %.2f, %.2f, %.2f, '%s', 0) RETURNING id",
                    req.roomNumber,
                    escapeString(req.guestName),
                    escapeString(req.guestEmail),
                    escapeString(req.guestCpf),
                    req.checkIn,
                    req.checkOut,
                    req.guests,
                    ratePlan.toString(),
                    prices.nights,
                    prices.subtotal,
                    prices.discount,
                    prices.total,
                    ReservationStatus.PENDING.toString()
                );

                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(query);
                    rs.next();
                    int id = rs.getInt("id");
                    return findById(id);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error creating reservation", e);
        }
    }

    public static Reservation confirm(int id, double amountPaid) {
        try {
            Reservation res = findById(id);
            if (res == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }
            if (!res.status.equals(ReservationStatus.PENDING.toString())) {
                throw new ApiException(HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", "Reserva deve estar pendente para confirmar");
            }

            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(String.format(
                    "UPDATE reservations SET status = '%s', amount_paid = %.2f WHERE id = %d",
                    ReservationStatus.CONFIRMED.toString(),
                    amountPaid,
                    id
                ));
            }

            var updated = findById(id);
            sendConfirmationEmail(updated);
            return updated;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error confirming reservation", e);
        }
    }

    public static Reservation checkIn(int id) {
        try {
            Reservation res = findById(id);
            if (res == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }
            if (!res.status.equals(ReservationStatus.CONFIRMED.toString())) {
                throw new ApiException(HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", "Reserva deve estar confirmada para check-in");
            }

            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(String.format(
                    "UPDATE reservations SET status = '%s' WHERE id = %d",
                    ReservationStatus.CHECKED_IN.toString(),
                    id
                ));
            }

            return findById(id);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error checking in reservation", e);
        }
    }

    public static ReservationDocument checkOut(int id) {
        try {
            Reservation res = findById(id);
            if (res == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }
            if (!res.status.equals(ReservationStatus.CHECKED_IN.toString())) {
                throw new ApiException(HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", "Reserva deve estar com check-in para fazer check-out");
            }

            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(String.format(
                    "UPDATE reservations SET status = '%s' WHERE id = %d",
                    ReservationStatus.CHECKED_OUT.toString(),
                    id
                ));
            }

            return getDocument(id);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error checking out reservation", e);
        }
    }

    public static Reservation cancel(int id) {
        try {
            Reservation res = findById(id);
            if (res == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
            }
            if (!res.status.equals(ReservationStatus.PENDING.toString()) && !res.status.equals(ReservationStatus.CONFIRMED.toString())) {
                throw new ApiException(HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", "Cancela nada é permitida apenas antes do check-in");
            }

            double refund = 0;
            if (res.status.equals(ReservationStatus.CONFIRMED.toString())) {
                RatePlan ratePlan = RatePlan.fromString(res.ratePlan);
                if (ratePlan == RatePlan.STANDARD) {
                    refund = res.amountPaid;
                }
            }

            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(String.format(
                    "UPDATE reservations SET status = '%s', refund = %.2f WHERE id = %d",
                    ReservationStatus.CANCELLED.toString(),
                    refund,
                    id
                ));
            }

            return findById(id);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error canceling reservation", e);
        }
    }

    public static Reservation findById(int id) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM reservations WHERE id = " + id);
            if (rs.next()) {
                return new Reservation(
                    rs.getInt("id"),
                    rs.getInt("room_number"),
                    rs.getString("guest_name"),
                    rs.getString("guest_email"),
                    rs.getString("guest_cpf"),
                    rs.getString("check_in"),
                    rs.getString("check_out"),
                    rs.getInt("guests"),
                    rs.getString("rate_plan"),
                    rs.getInt("nights"),
                    rs.getDouble("subtotal"),
                    rs.getDouble("discount"),
                    rs.getDouble("total"),
                    rs.getString("status"),
                    rs.getDouble("amount_paid"),
                    rs.getObject("refund") != null ? rs.getDouble("refund") : null
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reservation", e);
        }
        return null;
    }

    public static ReservationDocument getDocument(int id) {
        try {
            Reservation res = findById(id);
            if (res == null) {
                return null;
            }
            if (!res.status.equals(ReservationStatus.CONFIRMED.toString()) &&
                !res.status.equals(ReservationStatus.CHECKED_IN.toString()) &&
                !res.status.equals(ReservationStatus.CHECKED_OUT.toString())) {
                return null;
            }

            Room room = RoomService.findByNumber(res.roomNumber);
            return new ReservationDocument(
                res.id,
                res.guestName,
                res.guestCpf,
                res.roomNumber,
                room.type,
                res.checkIn,
                res.checkOut,
                res.nights,
                res.subtotal,
                res.discount > 0 ? res.discount : null,
                res.total
            );
        } catch (Exception e) {
            throw new RuntimeException("Error getting document", e);
        }
    }

    private static boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = String.format(
                "SELECT COUNT(*) as count FROM reservations " +
                "WHERE room_number = %d " +
                "AND status != '%s' " +
                "AND check_in < '%s' " +
                "AND check_out > '%s'",
                roomNumber,
                ReservationStatus.CANCELLED.toString(),
                checkOut,
                checkIn
            );
            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            return rs.getInt("count") > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking room availability", e);
        }
    }

    private static void sendConfirmationEmail(Reservation res) {
        var doc = getDocument(res.id);
        if (doc == null) return;

        String body = formatConfirmationEmail(doc);
        MessageService.sendEmail(res.guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }

    private static String formatConfirmationEmail(ReservationDocument doc) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            doc.guestName,
            formatDocument(doc)
        );
    }

    public static String formatDocument(ReservationDocument doc) {
        String nightsLabel = doc.nights == 1 ? "1 diária" : doc.nights + " diárias";
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.reservationId).append("\n");
        sb.append("Hóspede: ").append(doc.guestName).append(" (CPF ").append(doc.guestCpf).append(")\n");
        sb.append("Quarto: ").append(doc.roomNumber).append(" (").append(doc.roomType).append(")\n");
        sb.append("Entrada: ").append(DateUtils.formatDisplay(DateUtils.parseIsoDate(doc.checkIn)))
          .append(" | Saída: ").append(DateUtils.formatDisplay(DateUtils.parseIsoDate(doc.checkOut)))
          .append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(Money.formatBRL(doc.subtotal)).append("\n");
        if (doc.discount != null && doc.discount > 0) {
            sb.append("Desconto: ").append(Money.formatBRL(doc.discount)).append("\n");
        }
        sb.append("Total: ").append(Money.formatBRL(doc.total));
        return sb.toString();
    }

    private static String escapeString(String value) {
        return value.replace("'", "''").replace("\\", "\\\\");
    }
}
