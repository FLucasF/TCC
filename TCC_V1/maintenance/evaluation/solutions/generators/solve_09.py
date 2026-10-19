"""Scenario 09 (Template Method): the pattern solution (an abstract CsvReport with the fixed skeleton; four reports
fill in the steps) and the naive one (each report written whole, each with its own title line)."""

from kit import HOTEL, HTTP_API, OCCUPANCY, REPORTS_TEST, REVENUE, Solution

REPORTS = "src/main/java/hotel/reports/"


def shared_changes(solution: Solution) -> None:
    """The web routes, the wiring and the tests: the same for both solutions."""
    solution.replace(HTTP_API, "import hotel.reports.OccupancyReport;\nimport hotel.reports.RevenueReport;\n",
                     "import hotel.reports.OccupancyReport;\nimport hotel.reports.RatePlanReport;\n"
                     "import hotel.reports.RevenueReport;\nimport hotel.reports.TourismTaxReport;\n")
    solution.replace(HTTP_API, """    private final RevenueReport revenueReport;
""", """    private final RevenueReport revenueReport;
    private final TourismTaxReport tourismTaxReport;
    private final RatePlanReport ratePlanReport;
""")
    solution.replace(HTTP_API, """            RevenueReport revenueReport) {""", """            RevenueReport revenueReport,
            TourismTaxReport tourismTaxReport,
            RatePlanReport ratePlanReport) {""")
    solution.replace(HTTP_API, """        this.revenueReport = revenueReport;
""", """        this.revenueReport = revenueReport;
        this.tourismTaxReport = tourismTaxReport;
        this.ratePlanReport = ratePlanReport;
""")
    solution.replace(HTTP_API, """            ctx.contentType("text/csv; charset=utf-8").result(revenueReport.generate(month));
        });""", """            ctx.contentType("text/csv; charset=utf-8").result(revenueReport.generate(month));
        });
        server.get("/reports/tourism-tax", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(tourismTaxReport.generate(month));
        });
        server.get("/reports/rate-plans", ctx -> {
            YearMonth month = readMonth(ctx);
            ctx.contentType("text/csv; charset=utf-8").result(ratePlanReport.generate(month));
        });""")
    solution.replace(HOTEL, "import hotel.reports.OccupancyReport;\nimport hotel.reports.RevenueReport;\n",
                     "import hotel.reports.OccupancyReport;\nimport hotel.reports.RatePlanReport;\n"
                     "import hotel.reports.RevenueReport;\nimport hotel.reports.TourismTaxReport;\n")
    solution.replace(HOTEL, """                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store));""",
                     """                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store),
                new TourismTaxReport(store), new RatePlanReport(store));""")
    for date in ("1", "3"):
        solution.replace(REPORTS_TEST, f'''generate(LocalDate.of(2027, 4, {date}));

        assertEquals("""
                room,type''', f'''generate(LocalDate.of(2027, 4, {date}));

        assertEquals("""
                Pousada Maré Alta,occupancy,2027-04-0{date}
                room,type''')
    for first_row, month in (("1,201", "04"), ("6,101", "05"), ("total,0.00", "06")):
        solution.replace(REPORTS_TEST, f"""                reservation,room,check_in,check_out,status,amount
                {first_row}""", f"""                Pousada Maré Alta,revenue,2027-{month}
                reservation,room,check_in,check_out,status,amount
                {first_row}""")
    solution.replace(REPORTS_TEST, '''    @Test
    void anEmptyMonthHasOnlyTheTotal() {''', '''    @Test
    void tourismTaxIsChargedPerGuestPerNight() {
        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 4));

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-04
                reservation,room,check_in,check_out,guests,nights,tax
                1,201,2027-04-01,2027-04-03,1,2,9.00
                3,301,2027-04-02,2027-04-04,1,2,9.00
                total,18.00
                """, csv);
    }

    @Test
    void ratePlansSplitTheRevenue() {
        String csv = new RatePlanReport(store).generate(YearMonth.of(2027, 4));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-04
                rate_plan,reservations,amount
                STANDARD,2,1939.60
                NON_REFUNDABLE,0,0.00
                total,2,1939.60
                """, csv);
    }

    @Test
    void anEmptyMonthHasOnlyTheTotal() {''')


# ---------------------------------------------------------------- the pattern: the skeleton in one place
pattern = Solution("09-pattern")
shared_changes(pattern)
pattern.write(REPORTS + "CsvReport.java", """package hotel.reports;

import hotel.reservations.Reservation;
import java.util.List;

/**
 * Every report has the same shape: the title line (the inn, the report's name, the period), the header, one line
 * per row, and the total line. Each report says only what changes: its name, its columns, which reservations
 * count, its rows and its total.
 */
public abstract class CsvReport<P> {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    public final String generate(P period) {
        List<Reservation> selected = select(period);
        StringBuilder csv = new StringBuilder();
        csv.append(HOTEL_NAME).append(',').append(name()).append(',').append(period).append('\\n');
        csv.append(header()).append('\\n');
        for (String row : rows(selected)) {
            csv.append(row).append('\\n');
        }
        csv.append("total,").append(total(selected)).append('\\n');
        return csv.toString();
    }

    protected abstract String name();

    protected abstract String header();

    /** The reservations the report is about, in the report's order. */
    protected abstract List<Reservation> select(P period);

    protected abstract List<String> rows(List<Reservation> selected);

    /** What comes after "total," on the last line. */
    protected abstract String total(List<Reservation> selected);
}
""")
pattern.write(OCCUPANCY, """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Which rooms are occupied on a given night, for the front desk (CSV). */
public final class OccupancyReport extends CsvReport<LocalDate> {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    @Override
    protected String name() {
        return "occupancy";
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<Reservation> select(LocalDate night) {
        return store.all().stream()
                .filter(Reservation::isPaid)
                .filter(reservation -> !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
    }

    @Override
    protected List<String> rows(List<Reservation> selected) {
        return selected.stream().map(reservation -> {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            return room.number() + "," + room.type() + "," + reservation.id() + ","
                    + reservation.checkIn() + "," + reservation.checkOut();
        }).toList();
    }

    @Override
    protected String total(List<Reservation> selected) {
        return selected.size() + "/" + rooms.all().size();
    }
}
""")
pattern.write(REVENUE, """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * The money the inn kept from the reservations that start in a given month, for the accountant (CSV).
 * A cancelled non-refundable reservation counts: the guest paid and nothing went back.
 */
public final class RevenueReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    /** The reservations that start in the month and left money with the inn, by check-in. */
    static List<Reservation> keptMoneyIn(ReservationStore store, YearMonth month) {
        return store.all().stream()
                .filter(reservation -> reservation.amountKept().signum() > 0)
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    @Override
    protected String name() {
        return "revenue";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<Reservation> select(YearMonth month) {
        return keptMoneyIn(store, month);
    }

    @Override
    protected List<String> rows(List<Reservation> selected) {
        return selected.stream().map(reservation -> reservation.id() + "," + reservation.roomNumber() + ","
                + reservation.checkIn() + "," + reservation.checkOut() + "," + reservation.status() + ","
                + reservation.amountKept().toPlainString()).toList();
    }

    @Override
    protected String total(List<Reservation> selected) {
        return selected.stream().map(Reservation::amountKept)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add).toPlainString();
    }
}
""")
pattern.write(REPORTS + "TourismTaxReport.java", """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges: R$ 4,50 per guest per night, for the paid stays that start in the month. */
public final class TourismTaxReport extends CsvReport<YearMonth> {

    static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    private static BigDecimal taxOf(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT.multiply(BigDecimal.valueOf((long) reservation.guests()
                * reservation.price().nights()));
    }

    @Override
    protected String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<Reservation> select(YearMonth month) {
        return store.all().stream()
                .filter(Reservation::isPaid)
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    @Override
    protected List<String> rows(List<Reservation> selected) {
        return selected.stream().map(reservation -> reservation.id() + "," + reservation.roomNumber() + ","
                + reservation.checkIn() + "," + reservation.checkOut() + "," + reservation.guests() + ","
                + reservation.price().nights() + "," + taxOf(reservation).toPlainString()).toList();
    }

    @Override
    protected String total(List<Reservation> selected) {
        return selected.stream().map(TourismTaxReport::taxOf)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add).toPlainString();
    }
}
""")
pattern.write(REPORTS + "RatePlanReport.java", """package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

/** The revenue of the month split by rate plan, for the management (every plan has a line, even with zero). */
public final class RatePlanReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::amountKept).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    @Override
    protected String name() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<Reservation> select(YearMonth month) {
        return RevenueReport.keptMoneyIn(store, month);
    }

    @Override
    protected List<String> rows(List<Reservation> selected) {
        return Arrays.stream(RatePlan.values()).map(plan -> {
            List<Reservation> ofPlan = selected.stream().filter(reservation -> reservation.ratePlan() == plan).toList();
            return plan + "," + ofPlan.size() + "," + sum(ofPlan).toPlainString();
        }).toList();
    }

    @Override
    protected String total(List<Reservation> selected) {
        return selected.size() + "," + sum(selected).toPlainString();
    }
}
""")
pattern.build_and_save()

# ---------------------------------------------------------------- naive: four whole reports, four title lines
naive = Solution("09-naive")
shared_changes(naive)
naive.replace(OCCUPANCY, """        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\\n");""", """        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(night).append('\\n');
        csv.append("room,type,reservation,check_in,check_out\\n");""")
naive.replace(REVENUE, """        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\\n");""", """        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month).append('\\n');
        csv.append("reservation,room,check_in,check_out,status,amount\\n");""")
naive.write(REPORTS + "TourismTaxReport.java", """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges: R$ 4,50 per guest per night, for the paid stays that start in the month. */
public final class TourismTaxReport {

    static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> paidInMonth = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidInMonth.add(reservation);
            }
        }
        paidInMonth.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append('\\n');
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\\n");
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidInMonth) {
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT.multiply(
                    BigDecimal.valueOf((long) reservation.guests() * reservation.price().nights()));
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(reservation.price().nights()).append(',')
                    .append(tax.toPlainString()).append('\\n');
            totalTax = totalTax.add(tax);
        }
        csv.append("total,").append(totalTax.toPlainString()).append('\\n');
        return csv.toString();
    }
}
""")
naive.write(REPORTS + "RatePlanReport.java", """package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The revenue of the month split by rate plan, for the management (every plan has a line, even with zero). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\\n');
        csv.append("rate_plan,reservations,amount\\n");
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.append(plan).append(',').append(count).append(',').append(amount.toPlainString()).append('\\n');
            revenue = revenue.add(amount);
        }
        csv.append("total,").append(withRevenue.size()).append(',').append(revenue.toPlainString()).append('\\n');
        return csv.toString();
    }
}
""")
naive.build_and_save()
