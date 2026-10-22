package pousada;

import pousada.Domain.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Regras de negócio e persistência (JDBC). */
public final class Service {
    private static final String RES_COLS = """
            id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, guest_passport,
            guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
            status, amount_paid, refund""";

    private final Db db;

    public Service(Db db) {
        this.db = db;
    }

    public record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public record Document(Reservation reservation, Room room, Identification.Receipt receipt, String text) {}

    private <T> T tx(Db.TxWork<T> work) {
        try {
            return db.tx(work);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- consultas ----

    public List<Room> rooms() {
        return tx(c -> {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
                List<Room> out = new ArrayList<>();
                while (rs.next()) out.add(room(rs));
                return out;
            }
        });
    }

    public Reservation reservation(long id) {
        return tx(c -> load(c, id, false));
    }

    public List<Message> messages() {
        return tx(c -> {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
                List<Message> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
                return out;
            }
        });
    }

    // ---- comandos ----

    public Reservation create(NewReservation n) {
        return tx(c -> {
            Room room;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
                ps.setInt(1, n.roomNumber());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + n.roomNumber());
                    room = room(rs);
                }
            }
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1""")) {
                ps.setInt(1, room.number());
                ps.setDate(2, Date.valueOf(n.checkOut()));
                ps.setDate(3, Date.valueOf(n.checkIn()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites pedidas");
                    }
                }
            }
            Pricing.Quote q = Pricing.quote(room.nightlyRate(), n.checkIn(), n.checkOut(), n.ratePlan());
            Identification i = n.identification();
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                      guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, discount,
                      total, status, amount_paid)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id""")) {
                int k = 1;
                ps.setInt(k++, room.number());
                ps.setString(k++, n.guestName());
                ps.setString(k++, n.guestEmail());
                ps.setString(k++, i.cpf());
                ps.setString(k++, i.cnpj());
                ps.setString(k++, i.companyName());
                ps.setString(k++, i.passport());
                ps.setString(k++, i.country());
                ps.setDate(k++, Date.valueOf(n.checkIn()));
                ps.setDate(k++, Date.valueOf(n.checkOut()));
                ps.setInt(k++, n.guests());
                ps.setString(k++, n.ratePlan().name());
                ps.setInt(k++, q.nights());
                ps.setBigDecimal(k++, q.subtotal());
                ps.setBigDecimal(k++, q.discount());
                ps.setBigDecimal(k++, q.total());
                ps.setString(k++, Status.PENDING.name());
                ps.setBigDecimal(k, Money.ZERO);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return load(c, rs.getLong(1), false);
                }
            }
        });
    }

    public Reservation confirm(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == Status.PENDING, "Só é possível confirmar uma reserva pendente");
            update(c, id, "status = 'CONFIRMED', amount_paid = total");
            Reservation confirmed = load(c, id, false);
            Document doc = document(c, confirmed);
            String body = "Olá, " + confirmed.guestName() + "!\n\n"
                    + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                    + doc.text() + "\n\n"
                    + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                    + "Até breve!";
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES ('EMAIL', ?, ?, ?)")) {
                ps.setString(1, confirmed.guestEmail());
                ps.setString(2, "Reserva confirmada - Pousada Maré Alta");
                ps.setString(3, body);
                ps.executeUpdate();
            }
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == Status.PENDING || r.status() == Status.CONFIRMED,
                    "Só é possível cancelar uma reserva pendente ou confirmada");
            // pendente não tem nada pago; no plano não reembolsável nada é devolvido
            BigDecimal refund = r.ratePlan() == RatePlan.STANDARD ? r.amountPaid() : Money.ZERO;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?")) {
                ps.setBigDecimal(1, refund);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == Status.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
            update(c, id, "status = 'CHECKED_IN'");
            return load(c, id, false);
        });
    }

    public Document checkOut(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == Status.CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
            update(c, id, "status = 'CHECKED_OUT'");
            return document(c, load(c, id, false));
        });
    }

    public Document document(long id) {
        return tx(c -> {
            Reservation r = load(c, id, false);
            require(r.isPaid(), "Só existe documento para reserva paga");
            return document(c, r);
        });
    }

    // ---- relatórios ----

    public String occupancyCsv(LocalDate date) {
        return tx(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.room_number, m.type, r.id, r.check_in, r.check_out
                    FROM reservations r JOIN rooms m ON m.number = r.room_number
                    WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ?
                    ORDER BY r.room_number, r.id""")) {
                ps.setDate(1, Date.valueOf(date));
                ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupied++;
                        sb.append(rs.getInt(1)).append(',').append(rs.getString(2)).append(',').append(rs.getLong(3))
                          .append(',').append(rs.getDate(4).toLocalDate()).append(',').append(rs.getDate(5).toLocalDate())
                          .append('\n');
                    }
                }
            }
            int total;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM rooms")) {
                rs.next();
                total = rs.getInt(1);
            }
            return sb.append("total,").append(occupied).append('/').append(total).append('\n').toString();
        });
    }

    public String revenueCsv(YearMonth month) {
        return tx(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0)
                    FROM reservations
                    WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0
                    ORDER BY check_in, id""")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = Money.round(rs.getBigDecimal(6));
                        sum = sum.add(amount);
                        sb.append(rs.getLong(1)).append(',').append(rs.getInt(2)).append(',')
                          .append(rs.getDate(3).toLocalDate()).append(',').append(rs.getDate(4).toLocalDate()).append(',')
                          .append(rs.getString(5)).append(',').append(Money.plain(amount)).append('\n');
                    }
                }
            }
            return sb.append("total,").append(Money.plain(sum)).append('\n').toString();
        });
    }

    // ---- internos ----

    private static void require(boolean ok, String message) {
        if (!ok) throw ApiException.notAllowed(message);
    }

    private static void update(Connection c, long id, String set) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET " + set + " WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Document document(Connection c, Reservation r) throws SQLException {
        Room room;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, r.roomNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                room = room(rs);
            }
        }
        Identification.Receipt receipt = new Identification.Receipt(r.id(), r.guestName(), room.number(), room.type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
        return new Document(r, room, receipt, r.identification().render(receipt));
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), Money.round(rs.getBigDecimal(4)));
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id);
                BigDecimal refund = rs.getBigDecimal(20);
                return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        Identification.from(rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9)),
                        rs.getDate(10).toLocalDate(), rs.getDate(11).toLocalDate(), rs.getInt(12),
                        RatePlan.valueOf(rs.getString(13)), rs.getInt(14),
                        Money.round(rs.getBigDecimal(15)), Money.round(rs.getBigDecimal(16)), Money.round(rs.getBigDecimal(17)),
                        Status.valueOf(rs.getString(18)), Money.round(rs.getBigDecimal(19)),
                        refund == null ? null : Money.round(refund));
            }
        }
    }
}
