package br.com.marealta;

import br.com.marealta.database.Database;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.pricing.PricingService;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.MessageService;
import br.com.marealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationTest {
    private ReservationService reservationService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada_test");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        Database database = new Database(dbUrl, dbUser, dbPassword);
        try {
            database.initialize();
        } catch (Exception e) {
            System.out.println("Note: Integration test skipped - database not available");
            org.junit.jupiter.api.Assumptions.assumeTrue(false);
        }

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        messageService = new MessageService(messageRepository);
        reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, messageService
        );
    }

    @Test
    void shouldCompleteFullReservationFlow() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("João Silva");
        reservation.setGuestEmail("joao@example.com");
        reservation.setGuestCpf("12345678900");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(created.getNights()).isEqualTo(2);
        assertThat(created.getTotal()).isEqualByComparingTo("499.80");

        Reservation confirmed = reservationService.confirmReservation(created.getId());
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualByComparingTo(confirmed.getTotal());

        Reservation checkedIn = reservationService.checkIn(confirmed.getId());
        assertThat(checkedIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);

        Reservation checkedOut = reservationService.checkOut(checkedIn.getId());
        assertThat(checkedOut.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void shouldCalculateCorrectPriceForHighSeason() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setGuestName("Maria Santos");
        reservation.setGuestEmail("maria@example.com");
        reservation.setGuestCpf("98765432100");
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);

        assertThat(created.getSubtotal()).isEqualByComparingTo("599.76");
        assertThat(created.getTotal()).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setGuestName("Carlos Oliveira");
        reservation.setGuestEmail("carlos@example.com");
        reservation.setGuestCpf("11122233344");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Reservation created = reservationService.createReservation(reservation);

        assertThat(created.getSubtotal()).isEqualByComparingTo("779.80");
        assertThat(created.getDiscount()).isEqualByComparingTo("116.97");
        assertThat(created.getTotal()).isEqualByComparingTo("662.83");
    }

    @Test
    void shouldRefundOnCancellationForStandardPlan() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("102");
        reservation.setGuestName("Ana Costa");
        reservation.setGuestEmail("ana@example.com");
        reservation.setGuestCpf("55566677788");
        reservation.setCheckIn(LocalDate.of(2027, 4, 1));
        reservation.setCheckOut(LocalDate.of(2027, 4, 3));
        reservation.setGuests(1);
        reservation.setRatePlan(RatePlan.STANDARD);

        Reservation created = reservationService.createReservation(reservation);
        Reservation confirmed = reservationService.confirmReservation(created.getId());
        Reservation cancelled = reservationService.cancelReservation(confirmed.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(cancelled.getAmountPaid());
    }

    @Test
    void shouldNotRefundOnCancellationForNonRefundablePlan() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("202");
        reservation.setGuestName("Pedro Lima");
        reservation.setGuestEmail("pedro@example.com");
        reservation.setGuestCpf("99988877766");
        reservation.setCheckIn(LocalDate.of(2027, 4, 5));
        reservation.setCheckOut(LocalDate.of(2027, 4, 7));
        reservation.setGuests(2);
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Reservation created = reservationService.createReservation(reservation);
        Reservation confirmed = reservationService.confirmReservation(created.getId());
        Reservation cancelled = reservationService.cancelReservation(confirmed.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
