package br.com.pousada.marealta.repository;

import br.com.pousada.marealta.model.GuestIdentification;
import br.com.pousada.marealta.model.RatePlan;
import br.com.pousada.marealta.model.Reservation;
import br.com.pousada.marealta.model.ReservationStatus;
import br.com.pousada.marealta.util.DatabaseInitializer;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DatabaseInitializer db;

    public ReservationRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public Reservation create(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());

            setIdentificationFields(stmt, reservation.getIdentification());

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
                stmt.setNull(19, Types.DECIMAL);
            }

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                reservation.setId(rs.getLong(1));
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar reserva", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reserva", e);
        }
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                company_cnpj = ?, company_name = ?, guest_passport = ?, guest_country = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?, nights = ?, subtotal = ?,
                discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());

            setIdentificationFields(stmt, reservation.getIdentification());

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
                stmt.setNull(19, Types.DECIMAL);
            }

            stmt.setLong(20, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar reserva", e);
        }
    }

    public boolean hasConflictingReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
                AND check_in < ? AND check_out > ?
            """ + (excludeId != null ? " AND id != ?" : "");

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            if (excludeId != null) {
                stmt.setLong(4, excludeId);
            }

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar conflito de reservas", e);
        }
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();

            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }

            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas por mês", e);
        }
    }

    public List<Reservation> findOccupyingNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            ResultSet rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();

            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }

            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas da noite", e);
        }
    }

    private void setIdentificationFields(PreparedStatement stmt, GuestIdentification identification) throws SQLException {
        if (identification instanceof GuestIdentification.Individual individual) {
            stmt.setString(4, individual.cpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (identification instanceof GuestIdentification.Company company) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, company.cnpj());
            stmt.setString(6, company.companyName());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (identification instanceof GuestIdentification.Foreign foreign) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setString(7, foreign.passport());
            stmt.setString(8, foreign.country());
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));

        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        String companyName = rs.getString("company_name");
        String passport = rs.getString("guest_passport");
        String country = rs.getString("guest_country");

        if (cpf != null) {
            reservation.setIdentification(new GuestIdentification.Individual(cpf));
        } else if (cnpj != null) {
            reservation.setIdentification(new GuestIdentification.Company(cnpj, companyName));
        } else if (passport != null) {
            reservation.setIdentification(new GuestIdentification.Foreign(passport, country));
        }

        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setNights(rs.getInt("nights"));
        reservation.setSubtotal(rs.getBigDecimal("subtotal"));
        reservation.setDiscount(rs.getBigDecimal("discount"));
        reservation.setTotal(rs.getBigDecimal("total"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
        reservation.setRefund(rs.getBigDecimal("refund"));

        return reservation;
    }
}
