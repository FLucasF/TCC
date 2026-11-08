package br.com.marealta.persistence;

import br.com.marealta.app.RoomCatalog;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;
    private final RoomCatalog rooms;

    public ReservationRepository(Database db, RoomCatalog rooms) {
        this.db = db;
        this.rooms = rooms;
    }

    /** Grava uma reserva nova e dá a ela o próximo número. */
    public void insert(Reservation r) {
        long id = db.insert("""
                INSERT INTO reservation (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", ps -> {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setDate(5, Date.valueOf(r.checkInDate()));
            ps.setDate(6, Date.valueOf(r.checkOutDate()));
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setInt(9, r.nights());
            ps.setBigDecimal(10, r.subtotal());
            ps.setBigDecimal(11, r.discount());
            ps.setBigDecimal(12, r.total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            ps.setBigDecimal(15, r.refund());
        });
        r.assignId(id);
    }

    public void update(Reservation r) {
        db.update("UPDATE reservation SET status = ?, amount_paid = ?, refund = ? WHERE id = ?", ps -> {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
        });
    }

    /** Busca e trava a reserva até o fim da transação, para operações simultâneas não se atropelarem. */
    public Optional<Reservation> findForUpdate(long id) {
        return db.query("SELECT " + COLUMNS + " FROM reservation WHERE id = ? FOR UPDATE",
                ps -> ps.setLong(1, id), this::map).stream().findFirst();
    }

    public Optional<Reservation> find(long id) {
        return db.query("SELECT " + COLUMNS + " FROM reservation WHERE id = ?",
                ps -> ps.setLong(1, id), this::map).stream().findFirst();
    }

    /** Impede que duas reservas do mesmo quarto sejam criadas ao mesmo tempo. */
    public void lockRoom(int roomNumber) {
        db.query("SELECT pg_advisory_xact_lock(?)", ps -> ps.setLong(1, roomNumber), rs -> 0);
    }

    /** Há reserva não cancelada do quarto em alguma noite de [checkIn, checkOut)? */
    public boolean existsOverlapping(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return !db.query("""
                SELECT 1 FROM reservation
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""", ps -> {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
        }, rs -> 1).isEmpty();
    }

    /** Reservas pagas que ocupam um quarto na noite, em ordem de quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return db.query("SELECT " + COLUMNS + """
                 FROM reservation
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ?
                ORDER BY room_number, id""", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        }, this::map);
    }

    /** Reservas com entrada em [from, until), em ordem de entrada e número. */
    public List<Reservation> findCheckingInBetween(LocalDate from, LocalDate until) {
        return db.query("SELECT " + COLUMNS + """
                 FROM reservation
                WHERE check_in >= ? AND check_in < ?
                ORDER BY check_in, id""", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(until));
        }, this::map);
    }

    private Reservation map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Reservation.restore(
                rs.getLong("id"),
                rooms.require(rs.getInt("room_number")),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
