package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.ApiException;
import pousada.domain.ErrorCode;
import pousada.domain.NewReservation;
import pousada.domain.Quote;
import pousada.domain.RatePlan;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.domain.Season;
import pousada.service.ReceiptText;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainTest {
    private final RoomCatalog rooms = RoomCatalog.standard();

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private Reservation reservation(int room, String in, String out, RatePlan plan) {
        return Reservation.open(rooms.get(room), new NewReservation(room, "Carla Dias", "c@x.com",
                "111.222.333-44", LocalDate.parse(in), LocalDate.parse(out), 2, plan));
    }

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote quote = Quote.of(rooms.get(101), LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(d("599.76"), quote.subtotal());
        assertEquals(d("599.76"), quote.total());
        assertEquals(2, quote.nights());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(Season.LOW, Season.of(LocalDate.parse("2027-12-14")));
        assertEquals(Season.HIGH, Season.of(LocalDate.parse("2027-12-15")));
        assertEquals(Season.HIGH, Season.of(LocalDate.parse("2028-02-29")));
        assertEquals(Season.LOW, Season.of(LocalDate.parse("2028-03-01")));
        assertEquals(Season.HIGH, Season.of(LocalDate.parse("2027-07-31")));
        assertEquals(Season.LOW, Season.of(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscountIsRoundedHalfEven() {
        Quote quote = Quote.of(rooms.get(201), LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(d("779.80"), quote.subtotal());
        assertEquals(d("116.97"), quote.discount());
        assertEquals(d("662.83"), quote.total());
    }

    @Test
    void lifecycleFollowsTheAllowedOrder() {
        Reservation r = reservation(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertThrows(ApiException.class, r::registerCheckIn);
        r.confirm();
        assertEquals(r.total(), r.amountPaid());
        assertThrows(ApiException.class, r::confirm);
        assertThrows(ApiException.class, r::registerCheckOut);
        r.registerCheckIn();
        assertThrows(ApiException.class, r::cancel);
        r.registerCheckOut();
        assertEquals(ReservationStatus.CHECKED_OUT, r.status());
    }

    @Test
    void cancellationRefundDependsOnThePlan() {
        Reservation standard = reservation(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        standard.confirm();
        standard.cancel();
        assertEquals(standard.total(), standard.refund());
        assertEquals(0, standard.retained().signum());

        Reservation locked = reservation(201, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE);
        locked.confirm();
        locked.cancel();
        assertEquals(d("0.00"), locked.refund());
        assertEquals(locked.total(), locked.retained());

        Reservation pending = reservation(201, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE);
        pending.cancel();
        assertEquals(d("0.00"), pending.refund());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED,
                assertThrows(ApiException.class, pending::cancel).code());
    }

    @Test
    void receiptTextMatchesTheAgreedLayout() {
        Reservation r = reservation(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        Reservation withId = new Reservation(7L, 201, r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), 2, r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                ReservationStatus.CONFIRMED, r.total(), null);
        Room room = rooms.get(201);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", ReceiptText.format(Receipt.of(withId, room)));
    }

    @Test
    void receiptTextShowsDiscountAndSingularNight() {
        Reservation r = reservation(301, "2028-04-01", "2028-04-02", RatePlan.NON_REFUNDABLE);
        Reservation withId = new Reservation(1234L, 301, r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), 2, r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                ReservationStatus.CONFIRMED, r.total(), null);
        String text = ReceiptText.format(Receipt.of(withId, rooms.get(301)));
        assertEquals("""
                Comprovante de reserva nº 1234
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 301 (SUITE)
                Entrada: 01/04/2028 | Saída: 02/04/2028 | 1 diária
                Diárias: R$ 579,90
                Desconto: R$ 86,98
                Total: R$ 492,92""", text);
    }
}
