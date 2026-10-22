package br.com.marealta.persistence;

import br.com.marealta.domain.Identity;
import br.com.marealta.domain.IdentityKinds;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Rooms;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReservationRepository {

    public void insert(Connection c, Reservation r) throws SQLException {
        List<String> identityFields = IdentityKinds.allFields();
        String sql = "insert into reservations (room_number, guest_name, guest_email, "
                + String.join(", ", identityFields)
                + ", check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
                + "amount_paid, refund) values (?, ?, ?, "
                + identityFields.stream().map(f -> "?").collect(Collectors.joining(", "))
                + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            st.setInt(i++, r.room().number());
            st.setString(i++, r.guestName());
            st.setString(i++, r.guestEmail());
            Map<String, String> fields = r.identity().fields();
            for (String f : identityFields) {
                st.setString(i++, fields.get(f));
            }
            st.setObject(i++, r.checkInDate());
            st.setObject(i++, r.checkOutDate());
            st.setInt(i++, r.guests());
            st.setString(i++, r.ratePlan().name());
            st.setInt(i++, r.quote().nights());
            st.setBigDecimal(i++, r.quote().subtotal());
            st.setBigDecimal(i++, r.quote().discount());
            st.setBigDecimal(i++, r.quote().total());
            st.setString(i++, r.status().name());
            st.setBigDecimal(i++, r.amountPaid());
            st.setBigDecimal(i, r.refund());
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                keys.next();
                r.assignId(keys.getLong("id"));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "update reservations set status = ?, amount_paid = ?, refund = ? where id = ?")) {
            st.setString(1, r.status().name());
            st.setBigDecimal(2, r.amountPaid());
            st.setBigDecimal(3, r.refund());
            st.setLong(4, r.id());
            st.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return query(c, "select * from reservations where id = ?", st -> st.setLong(1, id)).stream().findFirst();
    }

    public boolean isRoomTaken(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "select 1 from reservations where room_number = ? and status <> 'CANCELLED' "
                        + "and check_in < ? and check_out > ? limit 1")) {
            st.setInt(1, roomNumber);
            st.setObject(2, checkOut);
            st.setObject(3, checkIn);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Reservation> paidOccupying(Connection c, LocalDate night) throws SQLException {
        String statuses = ReservationStatus.paidOnes().stream()
                .map(s -> "'" + s.name() + "'").collect(Collectors.joining(", "));
        return query(c, "select * from reservations where status in (" + statuses + ") "
                + "and check_in <= ? and check_out > ? order by room_number, id", st -> {
            st.setObject(1, night);
            st.setObject(2, night);
        });
    }

    public List<Reservation> startingIn(Connection c, YearMonth month) throws SQLException {
        return query(c, "select * from reservations where check_in >= ? and check_in <= ? "
                + "order by check_in, id", st -> {
            st.setObject(1, month.atDay(1));
            st.setObject(2, month.atEndOfMonth());
        });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement st) throws SQLException;
    }

    private List<Reservation> query(Connection c, String sql, Binder binder) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql)) {
            binder.bind(st);
            try (ResultSet rs = st.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Map<String, String> given = new HashMap<>();
        for (String f : IdentityKinds.allFields()) {
            given.put(f, rs.getString(f));
        }
        Identity identity = IdentityKinds.from(given)
                .orElseThrow(() -> new IllegalStateException("Identificação inválida no banco"));
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"),
                Rooms.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"), rs.getString("guest_email"), identity,
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
