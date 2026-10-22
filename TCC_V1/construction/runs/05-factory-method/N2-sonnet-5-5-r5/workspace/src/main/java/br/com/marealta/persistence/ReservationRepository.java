package br.com.marealta.persistence;

import br.com.marealta.domain.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private static final String COLUMNS = """
            id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, guest_passport,
            guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status,
            amount_paid, refund""";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public Reservation insert(Reservation r) {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                    company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                    subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id""";
        long id = db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                IdentityFields f = r.identity().fields();
                int i = 1;
                ps.setInt(i++, r.room().number());
                ps.setString(i++, r.guestName());
                ps.setString(i++, r.guestEmail());
                ps.setString(i++, f.guestCpf());
                ps.setString(i++, f.companyCnpj());
                ps.setString(i++, f.companyName());
                ps.setString(i++, f.guestPassport());
                ps.setString(i++, f.guestCountry());
                ps.setObject(i++, r.checkIn());
                ps.setObject(i++, r.checkOut());
                ps.setInt(i++, r.guests());
                ps.setString(i++, r.ratePlan().name());
                ps.setInt(i++, r.nights());
                ps.setBigDecimal(i++, r.subtotal());
                ps.setBigDecimal(i++, r.discount());
                ps.setBigDecimal(i++, r.total());
                ps.setString(i++, r.status().name());
                ps.setBigDecimal(i++, r.amountPaid());
                ps.setBigDecimal(i, r.refund());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
        r.assignId(id);
        return r;
    }

    public void save(Reservation r) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status=?, amount_paid=?, refund=? WHERE id=?")) {
                ps.setString(1, r.status().name());
                ps.setBigDecimal(2, r.amountPaid());
                ps.setBigDecimal(3, r.refund());
                ps.setLong(4, r.id());
                return ps.executeUpdate();
            }
        });
    }

    public Optional<Reservation> find(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id=?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    /** Alguma reserva não cancelada do quarto toca alguma das noites [checkIn, checkOut)? */
    public boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number=? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                    LIMIT 1""")) {
                ps.setInt(1, roomNumber);
                ps.setObject(2, checkOut);
                ps.setObject(3, checkIn);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Reservas que têm a noite entre a entrada (inclusive) e a saída (exclusive). */
    public List<Reservation> findCoveringNight(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in <= ? AND check_out > ? ORDER BY id",
                ps -> {
                    ps.setObject(1, night);
                    ps.setObject(2, night);
                });
    }

    /** Reservas com entrada em [from, to), por data de entrada e depois número. */
    public List<Reservation> findCheckingInBetween(LocalDate from, LocalDate to) {
        return query("SELECT " + COLUMNS
                + " FROM reservations WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id", ps -> {
            ps.setObject(1, from);
            ps.setObject(2, to);
        });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Reservation> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                    return list;
                }
            }
        });
    }

    private Reservation map(ResultSet rs) throws SQLException {
        IdentityFields fields = new IdentityFields(rs.getString("guest_cpf"), rs.getString("company_cnpj"),
                rs.getString("company_name"), rs.getString("guest_passport"), rs.getString("guest_country"));
        return new Reservation(
                rs.getLong("id"),
                RoomCatalog.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                IdentityKinds.from(fields),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
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
