package hotel.reservations;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Keeps the reservations in the database. */
public final class ReservationStore {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, "
            + "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, "
            + "subtotal, discount, total, status, amount_paid, refund";
    /** PostgreSQL's code for a broken exclusion constraint: here, the room was already booked on one of the nights. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final DataSource dataSource;

    public ReservationStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Saves a new reservation; the database numbers it. */
    Reservation add(NewReservation request, Price price) {
        String insert = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, "
                + "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, "
                + "subtotal, discount, total, status, amount_paid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, request.roomNumber());
            statement.setString(2, request.guest().name());
            statement.setString(3, request.guest().email());
            statement.setString(4, request.guest().cpf());
            statement.setString(5, request.guest().companyCnpj());
            statement.setString(6, request.guest().companyName());
            statement.setString(7, request.guest().passport());
            statement.setString(8, request.guest().country());
            statement.setDate(9, Date.valueOf(request.checkIn()));
            statement.setDate(10, Date.valueOf(request.checkOut()));
            statement.setInt(11, request.guests());
            statement.setString(12, request.ratePlan().name());
            statement.setInt(13, price.nights());
            statement.setBigDecimal(14, price.subtotal());
            statement.setBigDecimal(15, price.discount());
            statement.setBigDecimal(16, price.total());
            statement.setString(17, ReservationStatus.PENDING.name());
            statement.setBigDecimal(18, PriceCalculator.cents(BigDecimal.ZERO));
            try (ResultSet generated = statement.executeQuery()) {
                generated.next();
                return new Reservation(generated.getLong("id"), request, price);
            }
        } catch (SQLException failure) {
            if (EXCLUSION_VIOLATION.equals(failure.getSQLState())) {
                throw new HotelException(ErrorCode.ROOM_UNAVAILABLE,
                        "room " + request.roomNumber() + " is taken on some of those nights");
            }
            throw new IllegalStateException("could not save the reservation", failure);
        }
    }

    /** Saves what changes in a reservation's life: the status, what was paid and what went back. */
    void update(Reservation reservation) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            statement.setString(1, reservation.status().name());
            statement.setBigDecimal(2, reservation.amountPaid());
            statement.setBigDecimal(3, reservation.refund());
            statement.setLong(4, reservation.id());
            statement.executeUpdate();
        } catch (SQLException failure) {
            throw new IllegalStateException("could not save reservation " + reservation.id(), failure);
        }
    }

    /** Whether a reservation that was not cancelled holds the room on any of the nights. */
    boolean isRoomTaken(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                                + "AND check_in < ? AND ? < check_out")) {
            select.setInt(1, roomNumber);
            select.setDate(2, Date.valueOf(checkOut));
            select.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rows = select.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not check room " + roomNumber, failure);
        }
    }

    public Optional<Reservation> find(long id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT " + COLUMNS + " FROM reservations WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rows = select.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read reservation " + id, failure);
        }
    }

    public List<Reservation> all() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT " + COLUMNS + " FROM reservations ORDER BY id");
                ResultSet rows = select.executeQuery()) {
            List<Reservation> reservations = new ArrayList<>();
            while (rows.next()) {
                reservations.add(read(rows));
            }
            return reservations;
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read the reservations", failure);
        }
    }

    private static Reservation read(ResultSet row) throws SQLException {
        String cpf = row.getString("guest_cpf");
        String companyCnpj = row.getString("company_cnpj");
        String passport = row.getString("guest_passport");

        Guest guest;
        if (cpf != null) {
            guest = Guest.withCpf(row.getString("guest_name"), row.getString("guest_email"), cpf);
        } else if (companyCnpj != null) {
            guest = Guest.withCompany(
                    row.getString("guest_name"), row.getString("guest_email"), companyCnpj,
                    row.getString("company_name"));
        } else {
            guest = Guest.withPassport(
                    row.getString("guest_name"), row.getString("guest_email"), passport,
                    row.getString("guest_country"));
        }

        NewReservation request = new NewReservation(
                row.getInt("room_number"),
                guest,
                row.getDate("check_in").toLocalDate(),
                row.getDate("check_out").toLocalDate(),
                row.getInt("guests"),
                RatePlan.valueOf(row.getString("rate_plan")));
        Price price = new Price(
                row.getInt("nights"), row.getBigDecimal("subtotal"), row.getBigDecimal("discount"),
                row.getBigDecimal("total"));
        return new Reservation(
                row.getLong("id"),
                request,
                price,
                ReservationStatus.valueOf(row.getString("status")),
                row.getBigDecimal("amount_paid"),
                row.getBigDecimal("refund"));
    }
}
