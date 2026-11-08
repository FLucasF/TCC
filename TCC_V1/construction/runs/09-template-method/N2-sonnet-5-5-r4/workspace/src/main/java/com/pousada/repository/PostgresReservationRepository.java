package com.pousada.repository;

import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PostgresReservationRepository implements ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database database;

    public PostgresReservationRepository(Database database) {
        this.database = database;
    }

    @Override
    public Reservation create(Reservation draft) {
        return database.inTransaction(c -> {
            try (var st = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                st.setInt(1, draft.getRoomNumber());
                st.execute();
            }
            try (var st = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                    + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
                st.setInt(1, draft.getRoomNumber());
                st.setObject(2, draft.getCheckOut());
                st.setObject(3, draft.getCheckIn());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                                "Quarto " + draft.getRoomNumber() + " já reservado em alguma das noites");
                    }
                }
            }
            try (var st = c.prepareStatement("INSERT INTO reservations (room_number, guest_name, guest_email, "
                    + "guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, "
                    + "status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
                st.setInt(1, draft.getRoomNumber());
                st.setString(2, draft.getGuestName());
                st.setString(3, draft.getGuestEmail());
                st.setString(4, draft.getGuestCpf());
                st.setObject(5, draft.getCheckIn());
                st.setObject(6, draft.getCheckOut());
                st.setInt(7, draft.getGuests());
                st.setString(8, draft.getRatePlan().name());
                st.setInt(9, draft.getNights());
                st.setBigDecimal(10, draft.getSubtotal());
                st.setBigDecimal(11, draft.getDiscount());
                st.setBigDecimal(12, draft.getTotal());
                st.setString(13, draft.getStatus().name());
                st.setBigDecimal(14, draft.getAmountPaid());
                st.setBigDecimal(15, draft.getRefund());
                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    return draft.withId(rs.getLong(1));
                }
            }
        });
    }

    @Override
    public Optional<Reservation> find(long id) {
        return database.inTransaction(c -> select(c, id, false));
    }

    @Override
    public Reservation update(long id, Consumer<Reservation> change) {
        return database.inTransaction(c -> {
            Reservation reservation = select(c, id, true).orElseThrow(() -> new DomainException(
                    ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
            change.accept(reservation);
            try (var st = c.prepareStatement("UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                st.setString(1, reservation.getStatus().name());
                st.setBigDecimal(2, reservation.getAmountPaid());
                st.setBigDecimal(3, reservation.getRefund());
                st.setLong(4, id);
                st.executeUpdate();
            }
            return reservation;
        });
    }

    @Override
    public List<Reservation> findAll() {
        return database.inTransaction(c -> {
            var result = new ArrayList<Reservation>();
            try (var st = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations ORDER BY id");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        });
    }

    private Optional<Reservation> select(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund);
    }
}
