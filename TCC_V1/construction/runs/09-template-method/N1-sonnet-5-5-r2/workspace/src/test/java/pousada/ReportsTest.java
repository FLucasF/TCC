package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.NewReservation;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;
import pousada.report.CsvWriter;
import pousada.report.OccupancyReport;
import pousada.report.Period;
import pousada.report.PeriodKind;
import pousada.report.RatePlanReport;
import pousada.report.Report;
import pousada.report.ReportSource;
import pousada.report.RevenueReport;
import pousada.report.TourismTaxReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportsTest {
    private final RoomCatalog rooms = RoomCatalog.standard();
    private final List<Reservation> all = new ArrayList<>();
    private long nextId = 1;

    private final ReportSource source = new ReportSource() {
        @Override
        public List<Reservation> occupying(LocalDate night) {
            return all.stream().filter(r -> r.occupies(night))
                    .sorted(Comparator.comparingInt(Reservation::roomNumber)).toList();
        }

        @Override
        public List<Reservation> startingIn(Period p) {
            return all.stream().filter(r -> !r.checkIn().isBefore(p.from()) && r.checkIn().isBefore(p.toExclusive()))
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id)).toList();
        }
    };

    private Reservation add(int room, String in, String out, int guests, RatePlan plan) {
        Reservation open = Reservation.open(rooms.get(room), new NewReservation(room, "H", "h@x.com", "1",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan));
        Reservation r = new Reservation(nextId++, room, "H", "h@x.com", "1", open.checkIn(), open.checkOut(),
                guests, plan, open.nights(), open.subtotal(), open.discount(), open.total(),
                open.status(), open.amountPaid(), null);
        all.add(r);
        return r;
    }

    private String render(Report report, String period) {
        Period p = report.periodKind().read(period);
        return CsvWriter.write(report, p, report.build(p, source));
    }

    @Test
    void occupancyListsPaidReservationsForTheNight() {
        add(202, "2027-07-09", "2027-07-11", 2, RatePlan.STANDARD).confirm();
        add(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD).confirm();
        add(102, "2027-07-08", "2027-07-10", 2, RatePlan.STANDARD).confirm(); // sai no dia
        add(301, "2027-07-10", "2027-07-11", 2, RatePlan.STANDARD);           // pendente
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-10
                room,type,reservation,check_in,check_out
                101,STANDARD,2,2027-07-10,2027-07-12
                202,DELUXE,1,2027-07-09,2027-07-11
                total,2/5
                """, render(new OccupancyReport(rooms), "2027-07-10"));
    }

    @Test
    void revenueKeepsNonRefundableCancellationsButNotRefundedOnes() {
        add(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD).confirm();
        Reservation locked = add(102, "2027-07-05", "2027-07-06", 2, RatePlan.NON_REFUNDABLE);
        locked.confirm();
        locked.cancel();
        Reservation refunded = add(201, "2027-07-06", "2027-07-07", 2, RatePlan.STANDARD);
        refunded.confirm();
        refunded.cancel();
        add(202, "2027-07-07", "2027-07-08", 2, RatePlan.STANDARD); // pendente
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                2,102,2027-07-05,2027-07-06,CANCELLED,254.90
                1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
                total,854.66
                """, render(new RevenueReport(), "2027-07"));
    }

    @Test
    void tourismTaxIsPerGuestPerNight() {
        add(201, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD).confirm();
        Reservation cancelled = add(202, "2027-03-11", "2027-03-12", 1, RatePlan.STANDARD);
        cancelled.confirm();
        cancelled.cancel();
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-03
                reservation,room,check_in,check_out,guests,nights,tax
                1,201,2027-03-10,2027-03-13,2,3,27.00
                total,27.00
                """, render(new TourismTaxReport(new BigDecimal("4.50")), "2027-03"));
    }

    @Test
    void ratePlansAlwaysListEveryPlan() {
        add(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD).confirm();
        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-03
                rate_plan,reservations,amount
                STANDARD,1,499.80
                NON_REFUNDABLE,0,0.00
                total,1,499.80
                """, render(new RatePlanReport(), "2027-03"));
    }

    @Test
    void periodsAreValidated() {
        assertEquals("2027-07-10", PeriodKind.DAY.read("2027-07-10").label());
        for (String bad : new String[]{null, "", "2027-7", "2027-07"}) {
            org.junit.jupiter.api.Assertions.assertThrows(pousada.domain.ApiException.class,
                    () -> PeriodKind.DAY.read(bad));
        }
        org.junit.jupiter.api.Assertions.assertThrows(pousada.domain.ApiException.class,
                () -> PeriodKind.MONTH.read("2027-07-10"));
    }
}
