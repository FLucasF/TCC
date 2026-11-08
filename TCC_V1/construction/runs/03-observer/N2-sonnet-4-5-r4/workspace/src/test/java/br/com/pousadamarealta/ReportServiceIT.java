package br.com.pousadamarealta;

import br.com.pousadamarealta.events.EventPublisher;
import br.com.pousadamarealta.repository.Database;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.service.ReportService;
import br.com.pousadamarealta.service.ReservationService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceIT {

    private ReportService reportService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Database database = new Database(dataSource);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        EventPublisher eventPublisher = new EventPublisher();

        reservationService = new ReservationService(reservationRepository, roomRepository, eventPublisher);
        reportService = new ReportService(roomRepository, reservationRepository);
    }

    @Test
    void shouldGenerateOccupancyReport() {
        var reservation = reservationService.create(
            "101",
            "Guest",
            "guest@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );
        reservationService.confirm(reservation.getId());

        String csv = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 10));

        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("101,STANDARD," + reservation.getId()));
        assertTrue(csv.contains("2027-03-10,2027-03-12"));
        assertTrue(csv.contains("total,1/5"));
    }

    @Test
    void shouldGenerateRevenueReport() {
        var reservation = reservationService.create(
            "201",
            "Guest",
            "guest@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "STANDARD"
        );
        reservationService.confirm(reservation.getId());

        String csv = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains(reservation.getId() + ",201,2027-03-10,2027-03-12,CONFIRMED"));
        assertTrue(csv.contains("779.80"));
        assertTrue(csv.contains("total,779.80"));
    }

    @Test
    void shouldIncludeCancelledNonRefundableInRevenue() {
        var reservation = reservationService.create(
            "201",
            "Guest",
            "guest@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "NON_REFUNDABLE"
        );
        reservationService.confirm(reservation.getId());
        reservationService.cancel(reservation.getId());

        String csv = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(csv.contains(reservation.getId().toString()));
        assertTrue(csv.contains("CANCELLED"));
        assertTrue(csv.contains("662.83"));
    }
}
