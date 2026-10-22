package br.com.marealta.reservation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private static final String PAID = "status in ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";

    public Reservation insert(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id")) {
            statement.setInt(1, r.roomNumber());
            statement.setString(2, r.guestName());
            statement.setString(3, r.guestEmail());
            statement.setString(4, r.guestCpf());
            statement.setDate(5, Date.valueOf(r.checkIn()));
            statement.setDate(6, Date.valueOf(r.checkOut()));
            statement.setInt(7, r.guests());
            statement.setString(8, r.ratePlan().name());
            statement.setInt(9, r.nights());
            statement.setBigDecimal(10, r.subtotal());
            statement.setBigDecimal(11, r.discount());
            statement.setBigDecimal(12, r.total());
            statement.setString(13, r.status().name());
            statement.setBigDecimal(14, r.amountPaid());
            statement.setBigDecimal(15, r.refund());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return withId(r, rs.getLong(1));
            }
        }
    }

    public void update(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update reservations set status = ?, amount_paid = ?, refund = ? where id = ?")) {
            statement.setString(1, r.status().name());
            statement.setBigDecimal(2, r.amountPaid());
            statement.setBigDecimal(3, r.refund());
            statement.setLong(4, r.id());
            statement.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection connection, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select " + COLUMNS + " from reservations where id = ?" + (forUpdate ? " for update" : ""))) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Há reserva não cancelada do quarto em alguma das noites de [checkIn, checkOut)? */
    public boolean overlaps(Connection connection, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select 1 from reservations where room_number = ? and status <> 'CANCELLED' "
                        + "and check_in < ? and check_out > ? limit 1")) {
            statement.setInt(1, roomNumber);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> paidOnNight(Connection connection, LocalDate night) throws SQLException {
        return query(connection, "select " + COLUMNS + " from reservations where " + PAID
                + " and check_in <= ? and check_out > ? order by room_number, id", night, night);
    }

    /** Reservas que começam no mês e deixaram dinheiro com a pousada. */
    public List<Reservation> keptStartingBetween(Connection connection, LocalDate first, LocalDate last)
            throws SQLException {
        return query(connection, "select " + COLUMNS + " from reservations where check_in >= ? and check_in <= ? "
                + "and amount_paid - coalesce(refund, 0) > 0 order by check_in, id", first, last);
    }

    private List<Reservation> query(Connection connection, String sql, LocalDate first, LocalDate second)
            throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(first));
            statement.setDate(2, Date.valueOf(second));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Reservation withId(Reservation r, long id) {
        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                r.status(), r.amountPaid(), r.refund());
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
