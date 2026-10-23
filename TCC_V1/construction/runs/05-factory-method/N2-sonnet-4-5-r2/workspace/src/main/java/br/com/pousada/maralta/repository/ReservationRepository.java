package br.com.pousada.maralta.repository;

import br.com.pousada.maralta.database.Database;
import br.com.pousada.maralta.domain.RatePlan;
import br.com.pousada.maralta.domain.Reservation;
import br.com.pousada.maralta.domain.ReservationStatus;
import br.com.pousada.maralta.guesttype.GuestType;
import br.com.pousada.maralta.guesttype.GuestTypeFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) {
        GuestType guestType = reservation.getGuestType();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("""
                 INSERT INTO reservations (
                     room_number, guest_name, guest_email,
                     guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                     check_in, check_out, guests, rate_plan,
                     nights, subtotal, discount, total, status, amount_paid, refund
                 ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 RETURNING id
             """)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, guestType.getGuestCpf());
            stmt.setString(5, guestType.getCompanyCnpj());
            stmt.setString(6, guestType.getCompanyName());
            stmt.setString(7, guestType.getGuestPassport());
            stmt.setString(8, guestType.getGuestCountry());
            stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(11, reservation.getGuests());
            stmt.setString(12, reservation.getRatePlan().name());
            stmt.setInt(13, reservation.getNights());
            stmt.setBigDecimal(14, reservation.getSubtotal());
            stmt.setBigDecimal(15, reservation.getDiscount());
            stmt.setBigDecimal(16, reservation.getTotal());
            stmt.setString(17, reservation.getStatus().name());
            stmt.setBigDecimal(18, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(19, reservation.getRefund());
            } else {
                stmt.setNull(19, java.sql.Types.DECIMAL);
            }

            var rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                return new Reservation(
                    id,
                    reservation.getRoomNumber(),
                    reservation.getGuestName(),
                    reservation.getGuestEmail(),
                    guestType,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getRatePlan(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal(),
                    reservation.getStatus(),
                    reservation.getAmountPaid(),
                    reservation.getRefund()
                );
            }

            throw new RuntimeException("Failed to get generated key");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    public void update(Reservation reservation) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("""
                 UPDATE reservations SET
                     status = ?, amount_paid = ?, refund = ?
                 WHERE id = ?
             """)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, java.sql.Types.DECIMAL);
            }
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Reservation findById(long id) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("""
                 SELECT id, room_number, guest_name, guest_email,
                        guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                        check_in, check_out, guests, rate_plan,
                        nights, subtotal, discount, total, status, amount_paid, refund
                 FROM reservations WHERE id = ?
             """)) {

            stmt.setLong(1, id);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return mapReservation(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return null;
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT id, room_number, guest_name, guest_email,
                   guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                   check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
        """ + (excludeId != null ? " AND id != ?" : "");

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeId != null) {
                stmt.setLong(4, excludeId);
            }

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch conflicting reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("""
                 SELECT id, room_number, guest_name, guest_email,
                        guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                        check_in, check_out, guests, rate_plan,
                        nights, subtotal, discount, total, status, amount_paid, refund
                 FROM reservations
                 WHERE check_in <= ? AND check_out > ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                 ORDER BY room_number
             """)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by date", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("""
                 SELECT id, room_number, guest_name, guest_email,
                        guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                        check_in, check_out, guests, rate_plan,
                        nights, subtotal, discount, total, status, amount_paid, refund
                 FROM reservations
                 WHERE check_in >= ? AND check_in < ?
                 ORDER BY check_in, id
             """)) {

            stmt.setDate(1, Date.valueOf(monthStart));
            stmt.setDate(2, Date.valueOf(monthEnd));

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapReservation(java.sql.ResultSet rs) throws SQLException {
        GuestType guestType = GuestTypeFactory.createFromDatabase(
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country")
        );

        BigDecimal refund = rs.getBigDecimal("refund");

        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            guestType,
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
            rs.wasNull() ? null : refund
        );
    }
}
