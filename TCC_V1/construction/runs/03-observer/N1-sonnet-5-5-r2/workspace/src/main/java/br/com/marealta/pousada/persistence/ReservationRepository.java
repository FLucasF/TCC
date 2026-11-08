package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    /** Impede que duas transações reservem o mesmo quarto ao mesmo tempo. */
    public void lockRoom(int roomNumber) {
        try (PreparedStatement st = db.connection().prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            st.setLong(1, roomNumber);
            st.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isRoomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (PreparedStatement st = db.connection().prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ?")) {
            st.setInt(1, roomNumber);
            st.setObject(2, checkOut);
            st.setObject(3, checkIn);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Reservation insert(Reservation r) {
        try (PreparedStatement st = db.connection().prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            st.setInt(1, r.room().number());
            st.setString(2, r.guestName());
            st.setString(3, r.guestEmail());
            st.setString(4, r.guestCpf());
            st.setObject(5, r.checkIn());
            st.setObject(6, r.checkOut());
            st.setInt(7, r.guests());
            st.setString(8, r.ratePlan().name());
            st.setInt(9, r.quote().nights());
            st.setBigDecimal(10, r.quote().subtotal());
            st.setBigDecimal(11, r.quote().discount());
            st.setBigDecimal(12, r.quote().total());
            st.setString(13, r.status().name());
            st.setBigDecimal(14, r.amountPaid());
            st.setBigDecimal(15, r.refund());
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(Reservation r) {
        try (PreparedStatement st = db.connection().prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            st.setString(1, r.status().name());
            st.setBigDecimal(2, r.amountPaid());
            st.setBigDecimal(3, r.refund());
            st.setLong(4, r.id());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Busca e trava a linha até o fim da transação. */
    public Optional<Reservation> findById(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id).stream().findFirst();
    }

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período e deixaram dinheiro com a pousada. */
    public List<Reservation> findRetainingMoney(LocalDate from, LocalDate toExclusive) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, toExclusive);
    }

    private List<Reservation> query(String sql, Object... params) {
        try (PreparedStatement st = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                st.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = st.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rs.next()) {
                    found.add(read(rs));
                }
                return found;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Room room = Room.find(rs.getInt("room_number")).orElseThrow();
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), room, rs.getString("guest_name"), rs.getString("guest_email"),
                rs.getString("guest_cpf"), rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
