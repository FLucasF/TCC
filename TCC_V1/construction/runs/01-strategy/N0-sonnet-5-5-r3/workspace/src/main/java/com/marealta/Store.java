package com.marealta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso SQL; todos os métodos usam a conexão (transação) recebida. */
public final class Store {
    private Store() {}

    private static final String RES_COLS = "id,room_number,guest_name,guest_email,guest_cpf,check_in,check_out,guests,"
            + "rate_plan,company_code,nights,subtotal,discount,total,status,amount_paid,refund";

    private static Room room(ResultSet r) throws SQLException {
        return new Room(r.getInt(1), RoomType.valueOf(r.getString(2)), r.getInt(3), r.getBigDecimal(4));
    }

    public static List<Room> rooms(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number,type,capacity,nightly_rate FROM rooms ORDER BY number");
             ResultSet r = ps.executeQuery()) {
            List<Room> out = new ArrayList<>();
            while (r.next()) out.add(room(r));
            return out;
        }
    }

    /** Busca o quarto travando a linha, o que serializa reservas concorrentes do mesmo quarto. */
    public static Optional<Room> lockRoom(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number,type,capacity,nightly_rate FROM rooms WHERE number=? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? Optional.of(room(r)) : Optional.empty();
            }
        }
    }

    public static boolean hasConflict(Connection c, int roomNumber, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number=? AND status<>'CANCELLED' AND check_in<? AND check_out>? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(out));
            ps.setDate(3, Date.valueOf(in));
            try (ResultSet r = ps.executeQuery()) {
                return r.next();
            }
        }
    }

    public static Reservation insert(Connection c, Reservation v) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations(room_number,guest_name,guest_email,guest_cpf,check_in,check_out,guests,rate_plan,"
                        + "company_code,nights,subtotal,discount,total,status,amount_paid,refund) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
            ps.setInt(1, v.roomNumber());
            ps.setString(2, v.guestName());
            ps.setString(3, v.guestEmail());
            ps.setString(4, v.guestCpf());
            ps.setDate(5, Date.valueOf(v.checkIn()));
            ps.setDate(6, Date.valueOf(v.checkOut()));
            ps.setInt(7, v.guests());
            ps.setString(8, v.ratePlan());
            ps.setString(9, v.companyCode());
            ps.setInt(10, v.nights());
            ps.setBigDecimal(11, v.subtotal());
            ps.setBigDecimal(12, v.discount());
            ps.setBigDecimal(13, v.total());
            ps.setString(14, v.status().name());
            ps.setBigDecimal(15, v.amountPaid());
            ps.setBigDecimal(16, v.refund());
            try (ResultSet r = ps.executeQuery()) {
                r.next();
                return find(c, r.getLong(1), false).orElseThrow();
            }
        }
    }

    private static Reservation reservation(ResultSet r) throws SQLException {
        return new Reservation(r.getLong(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5),
                r.getDate(6).toLocalDate(), r.getDate(7).toLocalDate(), r.getInt(8), r.getString(9), r.getString(10),
                r.getInt(11), r.getBigDecimal(12), r.getBigDecimal(13), r.getBigDecimal(14),
                ReservationStatus.valueOf(r.getString(15)), r.getBigDecimal(16), r.getBigDecimal(17));
    }

    public static Optional<Reservation> find(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? Optional.of(reservation(r)) : Optional.empty();
            }
        }
    }

    public static void updateState(Connection c, Reservation v) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status=?, amount_paid=?, refund=? WHERE id=?")) {
            ps.setString(1, v.status().name());
            ps.setBigDecimal(2, v.amountPaid());
            ps.setBigDecimal(3, v.refund());
            ps.setLong(4, v.id());
            ps.executeUpdate();
        }
    }

    /** Reservas com entrada em [from, to), por data de entrada e número. */
    public static List<Reservation> startingBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "check_in>=? AND check_in<?", "check_in, id", from, to);
    }

    /** Reservas cuja estadia cobre a noite dada (entrada <= noite < saída). */
    public static List<Reservation> coveringNight(Connection c, LocalDate night) throws SQLException {
        return query(c, "check_in<=? AND check_out>?", "room_number, id", night, night);
    }

    private static List<Reservation> query(Connection c, String where, String order, LocalDate a, LocalDate b)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE " + where + " ORDER BY " + order)) {
            ps.setDate(1, Date.valueOf(a));
            ps.setDate(2, Date.valueOf(b));
            try (ResultSet r = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (r.next()) out.add(reservation(r));
                return out;
            }
        }
    }

    public static void insertMessage(Connection c, Message m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages(channel,recipient,subject,body) VALUES (?,?,?,?)")) {
            ps.setString(1, m.channel());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public static List<Message> messages(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id,channel,recipient,subject,body FROM messages ORDER BY id");
             ResultSet r = ps.executeQuery()) {
            List<Message> out = new ArrayList<>();
            while (r.next()) out.add(new Message(r.getLong(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5)));
            return out;
        }
    }
}
