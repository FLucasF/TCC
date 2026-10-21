"""Calibration examples for the database column of the quality reviewer (TCC_V1/evaluation/quality): two reference
solutions with their database spoiled on purpose, both expected `ruim`. The behavior stays the same.

- database-types: 01-pattern with a careless schema in the migration: money in double precision, the dates as
  text, no foreign key, no CHECK, no exclusion constraint, and the messages without a primary key. The code reads
  the money back with setScale(2), so it still answers the same.
- database-code: 05-pattern with no migrations: the schema is created by the code at startup (CREATE TABLE IF NOT
  EXISTS, no foreign key, no NOT NULL), and a new reservation is saved with the SQL put together by concatenation,
  the guest's name and e-mail inside quotes (SQL injection, and a name with an apostrophe breaks it).
"""

from kit import MIGRATIONS, STORE, Solution

ROOMS = "src/main/java/hotel/rooms/Rooms.java"
SENT_MESSAGES = "src/main/java/hotel/messages/SentMessages.java"
DATABASE = "src/main/java/hotel/Database.java"

CARELESS_SCHEMA = """CREATE TABLE rooms (
    number       integer PRIMARY KEY,
    type         text,
    capacity     integer,
    nightly_rate double precision
);

INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
    (101, 'STANDARD', 2, 249.90),
    (102, 'STANDARD', 2, 249.90),
    (201, 'DELUXE', 3, 389.90),
    (202, 'DELUXE', 3, 389.90),
    (301, 'SUITE', 4, 579.90);

CREATE TABLE reservations (
    id          bigserial PRIMARY KEY,
    room_number integer,
    guest_name  text,
    guest_email text,
    guest_cpf   text,
    check_in    text,
    check_out   text,
    guests      integer,
    rate_plan   text,
    nights      integer,
    subtotal    double precision,
    discount    double precision,
    total       double precision,
    status      text,
    amount_paid double precision,
    refund      double precision
);

CREATE TABLE messages (
    id        bigserial,
    channel   text,
    recipient text,
    subject   text,
    body      text
);
"""

types = Solution("database-types")
types.apply("01-pattern")
types.write(MIGRATIONS + "V1__create_tables.sql", CARELESS_SCHEMA)
types.replace(STORE, "            statement.setDate(5, Date.valueOf(request.checkIn()));\n"
                     "            statement.setDate(6, Date.valueOf(request.checkOut()));\n",
              "            statement.setString(5, request.checkIn().toString());\n"
              "            statement.setString(6, request.checkOut().toString());\n")
types.replace(STORE, "            select.setDate(2, Date.valueOf(checkOut));\n"
                     "            select.setDate(3, Date.valueOf(checkIn));\n",
              "            select.setString(2, checkOut.toString());\n"
              "            select.setString(3, checkIn.toString());\n")
types.replace(STORE, """                row.getDate("check_in").toLocalDate(),
                row.getDate("check_out").toLocalDate(),""", """                LocalDate.parse(row.getString("check_in")),
                LocalDate.parse(row.getString("check_out")),""")
types.replace(STORE, """                row.getInt("nights"), row.getBigDecimal("subtotal"), row.getBigDecimal("discount"),
                row.getBigDecimal("total"));""", """                row.getInt("nights"), money(row, "subtotal"), money(row, "discount"), money(row, "total"));""")
types.replace(STORE, """                row.getBigDecimal("amount_paid"),
                row.getBigDecimal("refund"));""", """                money(row, "amount_paid"),
                money(row, "refund"));""")
types.replace(STORE, """    private static Reservation read(ResultSet row) throws SQLException {""",
              """    private static BigDecimal money(ResultSet row, String column) throws SQLException {
        BigDecimal value = row.getBigDecimal(column);
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private static Reservation read(ResultSet row) throws SQLException {""")
types.replace(STORE, """            if (EXCLUSION_VIOLATION.equals(failure.getSQLState())) {
                throw new HotelException(ErrorCode.ROOM_UNAVAILABLE,
                        "room " + request.roomNumber() + " is taken on some of those nights");
            }
""", "")
types.replace(ROOMS, """                row.getBigDecimal("nightly_rate"));""",
              """                row.getBigDecimal("nightly_rate").setScale(2, java.math.RoundingMode.HALF_EVEN));""")
types.build_and_save()

SCHEMA_IN_CODE = '''package hotel;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;

/**
 * The connections to the inn's PostgreSQL: the address comes in DB_URL (jdbc:postgresql://host:port/database), the
 * user in DB_USER and the password in DB_PASSWORD. Opening it creates the tables that are missing.
 */
public final class Database implements AutoCloseable {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS rooms (
                number integer PRIMARY KEY, type text, capacity integer, nightly_rate numeric(10, 2));
            INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90), (301, 'SUITE', 4, 579.90)
                ON CONFLICT DO NOTHING;
            CREATE TABLE IF NOT EXISTS reservations (
                id bigserial PRIMARY KEY, room_number integer, guest_name text, guest_email text, guest_cpf text,
                company_cnpj text, company_name text, guest_passport text, guest_country text, check_in date,
                check_out date, guests integer, rate_plan text, nights integer, subtotal numeric(10, 2),
                discount numeric(10, 2), total numeric(10, 2), status text, amount_paid numeric(10, 2),
                refund numeric(10, 2));
            CREATE TABLE IF NOT EXISTS messages (
                id bigserial PRIMARY KEY, channel text, recipient text, subject text, body text);
            """;

    private final HikariDataSource dataSource;

    public Database(Map<String, String> environment) {
        String url = environment.get("DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL is not set: it must hold the PostgreSQL address");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(environment.get("DB_USER"));
        config.setPassword(environment.get("DB_PASSWORD"));
        this.dataSource = new HikariDataSource(config);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(SCHEMA);
        } catch (SQLException failure) {
            throw new IllegalStateException("could not create the tables", failure);
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
'''

CONCATENATED_ADD = '''    /** Saves a new reservation; the database numbers it. */
    Reservation add(NewReservation request, Price price) {
        Guest guest = request.guest();
        String insert = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, "
                + "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, "
                + "subtotal, discount, total, status, amount_paid) VALUES (" + request.roomNumber() + ", '"
                + guest.name() + "', '" + guest.email() + "', " + text(guest.cpf()) + ", "
                + text(guest.companyCnpj()) + ", " + text(guest.companyName()) + ", " + text(guest.passport())
                + ", " + text(guest.country()) + ", '" + request.checkIn() + "', '" + request.checkOut() + "', "
                + request.guests() + ", '" + request.ratePlan() + "', " + price.nights() + ", " + price.subtotal()
                + ", " + price.discount() + ", " + price.total() + ", 'PENDING', 0.00) RETURNING id";
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet generated = statement.executeQuery(insert)) {
            generated.next();
            return new Reservation(generated.getLong("id"), request, price);
        } catch (SQLException failure) {
            throw new IllegalStateException("could not save the reservation", failure);
        }
    }

    private static String text(String value) {
        return value == null ? "NULL" : "'" + value + "'";
    }

'''

code = Solution("database-code")
code.apply("05-pattern")
code.write(DATABASE, SCHEMA_IN_CODE)
code.remove(MIGRATIONS + "V1__create_tables.sql")
code.remove(MIGRATIONS + "V2__guest_identification.sql")
store = code.read(STORE)
start = store.index("    /** Saves a new reservation; the database numbers it. */")
end = store.index("    /** Saves what changes in a reservation's life")
code.write(STORE, store[:start] + CONCATENATED_ADD + store[end:])
code.replace(STORE, "import java.sql.SQLException;\n", "import java.sql.SQLException;\nimport java.sql.Statement;\n")
code.build_and_save()
