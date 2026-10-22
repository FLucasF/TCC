package br.com.marealta;

import br.com.marealta.domain.*;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.MessageService;
import br.com.marealta.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationTest {

    private ReservationService reservationService;
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = createTestDataSource();

        roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initSchema();
        roomRepository.initData();
        reservationRepository.initSchema();
        messageRepository.initSchema();

        MessageService messageService = new MessageService(messageRepository);
        reservationService = new ReservationService(reservationRepository, roomRepository, messageService);
    }

    @Test
    void shouldCreateAndConfirmReservation() {
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );

        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            dateRange, 2, "STANDARD", null
        );

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.calculateTotal().amount()).isEqualByComparingTo("599.76");

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualTo(confirmed.calculateTotal());
    }

    @Test
    void shouldPreventDoubleBooking() {
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );

        reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            dateRange, 2, "STANDARD", null
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            "101", "Maria Santos", "maria@example.com", "98765432109",
            dateRange, 1, "STANDARD", null
        )).isInstanceOf(ReservationService.RoomUnavailableException.class);
    }

    @Test
    void shouldRejectExcessGuests() {
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            dateRange, 5, "STANDARD", null
        )).isInstanceOf(Reservation.CapacityValidationException.class);
    }

    @Test
    void shouldCalculateCorporateRateCorrectly() {
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        Reservation reservation = reservationService.createReservation(
            "201", "João Silva", "joao@example.com", "12345678901",
            dateRange, 2, "CORPORATE", "TECNOSUL"
        );

        assertThat(reservation.calculateSubtotal().amount()).isEqualByComparingTo("779.80");
        assertThat(reservation.calculateDiscount().amount()).isEqualByComparingTo("93.58");
        assertThat(reservation.calculateTotal().amount()).isEqualByComparingTo("686.22");
    }

    @Test
    void shouldValidateWeekendPackage() {
        DateRange validDateRange = new DateRange(
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14)
        );

        Reservation reservation = reservationService.createReservation(
            "201", "João Silva", "joao@example.com", "12345678901",
            validDateRange, 2, "WEEKEND_PACKAGE", null
        );

        assertThat(reservation.calculateTotal().amount()).isEqualByComparingTo("820.00");

        DateRange invalidDateRange = new DateRange(
            LocalDate.of(2027, 3, 13),
            LocalDate.of(2027, 3, 15)
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            "202", "Maria Santos", "maria@example.com", "98765432109",
            invalidDateRange, 2, "WEEKEND_PACKAGE", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCalculateLongStayCorrectly() {
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8)
        );

        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@example.com", "12345678901",
            dateRange, 1, "LONG_STAY", null
        );

        assertThat(reservation.calculateSubtotal().amount()).isEqualByComparingTo("1749.30");
        assertThat(reservation.calculateDiscount().amount()).isEqualByComparingTo("174.93");
        assertThat(reservation.calculateTotal().amount()).isEqualByComparingTo("1574.37");
    }

    private DataSource createTestDataSource() {
        String dbName = "test_" + UUID.randomUUID().toString().replace("-", "");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
