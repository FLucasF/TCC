package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainTest {
    private final PriceCalculator calc = new PriceCalculator();

    private Reservation reservation(Room room, IdentityType type, Map<String, String> data, String in, String out,
                                    RatePlan plan, long id) {
        Reservation r = Reservation.pending(room, "Carla Dias", "c@x.com", new GuestIdentity(type, data),
                LocalDate.parse(in), LocalDate.parse(out), 2, plan,
                calc.calculate(room.type(), LocalDate.parse(in), LocalDate.parse(out), plan));
        r.assignId(id);
        return r;
    }

    @Test
    void highSeasonPrice() {
        Price p = calc.calculate(RoomType.STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), p.total());
        assertEquals(2, p.nights());
    }

    @Test
    void nonRefundableDiscount() {
        Price p = calc.calculate(RoomType.DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), p.subtotal());
        assertEquals(new BigDecimal("116.97"), p.discount());
        assertEquals(new BigDecimal("662.83"), p.total());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, PriceCalculator.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, PriceCalculator.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, PriceCalculator.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, PriceCalculator.isHighSeason(LocalDate.parse("2028-03-01")));
    }

    @Test
    void receipt() {
        Reservation r = reservation(new Room(201, RoomType.DELUXE), new CpfIdentity(),
                Map.of("guestCpf", "111.222.333-44"), "2027-03-10", "2027-03-12", RatePlan.STANDARD, 7);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.identity().format().render(r));
    }

    @Test
    void invoice() {
        Reservation r = reservation(new Room(201, RoomType.DELUXE), new CnpjIdentity(),
                Map.of("companyCnpj", "12.345.678/0001-90", "companyName", "Tecnosul Ltda"), "2027-03-10",
                "2027-03-12", RatePlan.STANDARD, 3);
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Carla Dias
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", r.identity().format().render(r));
    }

    @Test
    void bookingReceiptWithDiscount() {
        Reservation r = reservation(new Room(301, RoomType.SUITE), new PassportIdentity(),
                Map.of("guestPassport", "X1", "guestCountry", "United Kingdom"), "2027-03-10", "2027-03-11",
                RatePlan.NON_REFUNDABLE, 4);
        assertEquals("""
                Booking receipt no. 4
                Guest: Carla Dias (passport X1, United Kingdom)
                Room: 301 (SUITE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 579.90
                Discount: BRL 86.98
                Total: BRL 492.92""", r.identity().format().render(r));
    }

    @Test
    void lifecycleAndRefund() {
        Reservation r = reservation(new Room(101, RoomType.STANDARD), new CpfIdentity(), Map.of("guestCpf", "1"),
                "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE, 1);
        assertThrows(AppException.class, r::startStay);
        r.confirm();
        assertEquals(r.price().total(), r.amountPaid());
        assertThrows(AppException.class, r::confirm);
        r.cancel();
        assertEquals(Money.zero(), r.refund());
        assertEquals(r.price().total(), r.retained());
    }

    @Test
    void pendingCancelRefundsNothing() {
        Reservation r = reservation(new Room(101, RoomType.STANDARD), new CpfIdentity(), Map.of("guestCpf", "1"),
                "2027-03-10", "2027-03-12", RatePlan.STANDARD, 1);
        r.cancel();
        assertEquals(Money.zero(), r.refund());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.brazilian(new BigDecimal("1234.56")));
        assertEquals("BRL 1,234.56", Money.english(new BigDecimal("1234.56")));
    }
}
