package pousada.infra;

import pousada.domain.AppException;
import pousada.domain.Channel;
import pousada.domain.ErrorCode;
import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.RoomCatalog;
import pousada.plan.RatePlans;
import pousada.service.ReservationRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresReservationRepository implements ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final DataSource dataSource;

    public PostgresReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation insert(Reservation r) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                // serializa as reservas do mesmo quarto até o fim da transação
                try (PreparedStatement lock = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    lock.setLong(1, 1_000_000L + r.room().number());
                    lock.execute();
                }
                try (PreparedStatement overlap = c.prepareStatement(
                        "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                                + "AND check_in < ? AND check_out > ? LIMIT 1")) {
                    overlap.setInt(1, r.room().number());
                    overlap.setDate(2, Date.valueOf(r.checkOutDate()));
                    overlap.setDate(3, Date.valueOf(r.checkInDate()));
                    try (ResultSet rs = overlap.executeQuery()) {
                        if (rs.next()) {
                            throw new AppException(ErrorCode.ROOM_UNAVAILABLE,
                                    "O quarto já está reservado em alguma das noites");
                        }
                    }
                }
                long id;
                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, "
                                + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
                                + "amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
                    insert.setInt(1, r.room().number());
                    insert.setString(2, r.guestName());
                    insert.setString(3, r.guestEmail());
                    insert.setString(4, r.guestCpf());
                    insert.setDate(5, Date.valueOf(r.checkInDate()));
                    insert.setDate(6, Date.valueOf(r.checkOutDate()));
                    insert.setInt(7, r.guests());
                    insert.setString(8, r.ratePlan().code());
                    insert.setInt(9, r.nights());
                    insert.setBigDecimal(10, r.subtotal());
                    insert.setBigDecimal(11, r.discount());
                    insert.setBigDecimal(12, r.total());
                    insert.setString(13, r.status().name());
                    insert.setBigDecimal(14, r.amountPaid());
                    insert.setBigDecimal(15, r.refund());
                    try (ResultSet rs = insert.executeQuery()) {
                        rs.next();
                        id = rs.getLong(1);
                    }
                }
                c.commit();
                return r.withId(id);
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(long id) {
        List<Reservation> found = query("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    @Override
    public void update(Reservation r, ReservationStatus expected, List<Message> messages) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ? AND status = ?")) {
                    ps.setString(1, r.status().name());
                    ps.setBigDecimal(2, r.amountPaid());
                    ps.setBigDecimal(3, r.refund());
                    ps.setLong(4, r.id());
                    ps.setString(5, expected.name());
                    changed = ps.executeUpdate();
                }
                if (changed == 0) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                            "A situação da reserva mudou; operação não permitida");
                }
                for (Message m : messages) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
                        ps.setString(1, m.channel().name());
                        ps.setString(2, m.to());
                        ps.setString(3, m.subject());
                        ps.setString(4, m.body());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> messages() {
        List<Message> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Message(rs.getLong(1), Channel.valueOf(rs.getString(2)), rs.getString(3),
                        rs.getString(4), rs.getString(5)));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Reservation> paidOnNight(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND check_out > ?", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    @Override
    public List<Reservation> checkInBetween(LocalDate from, LocalDate to) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in BETWEEN ? AND ?", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"),
                RoomCatalog.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"), rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlans.byCode(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
