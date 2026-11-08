package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.exception.InvalidRequestException;
import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Room;
import br.com.pousadamarealta.domain.model.RoomType;
import br.com.pousadamarealta.domain.rateplan.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RatePlanTest {

    @Test
    void testStandardRatePlan() {
        Room room = new Room("201", RoomType.DELUXE);
        RatePlan.ReservationRequest request = new RatePlan.ReservationRequest(
            "201", "Guest", "guest@email.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, null
        );

        StandardRatePlan plan = new StandardRatePlan();
        PriceCalculation price = plan.calculatePrice(request, room);

        assertThat(price.getSubtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(price.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(price.getTotal()).isEqualByComparingTo(new BigDecimal("779.80"));
    }

    @Test
    void testCorporateRatePlan() {
        Room room = new Room("201", RoomType.DELUXE);
        RatePlan.ReservationRequest request = new RatePlan.ReservationRequest(
            "201", "Guest", "guest@email.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, "TECNOSUL"
        );

        CorporateRatePlan plan = new CorporateRatePlan();
        PriceCalculation price = plan.calculatePrice(request, room);

        assertThat(price.getSubtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(price.getDiscount()).isEqualByComparingTo(new BigDecimal("93.58"));
        assertThat(price.getTotal()).isEqualByComparingTo(new BigDecimal("686.22"));
    }

    @Test
    void testCorporateRatePlanMissingCode() {
        Room room = new Room("201", RoomType.DELUXE);
        RatePlan.ReservationRequest request = new RatePlan.ReservationRequest(
            "201", "Guest", "guest@email.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, null
        );

        CorporateRatePlan plan = new CorporateRatePlan();

        assertThatThrownBy(() -> plan.validate(request, room))
            .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void testWeekendPackageRatePlan() {
        Room room = new Room("201", RoomType.DELUXE);
        RatePlan.ReservationRequest request = new RatePlan.ReservationRequest(
            "201", "Guest", "guest@email.com", "12345678901",
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2, null
        );

        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        PriceCalculation price = plan.calculatePrice(request, room);

        assertThat(price.getSubtotal()).isEqualByComparingTo(new BigDecimal("820.00"));
        assertThat(price.getTotal()).isEqualByComparingTo(new BigDecimal("820.00"));
    }

    @Test
    void testLongStayRatePlan() {
        Room room = new Room("101", RoomType.STANDARD);
        RatePlan.ReservationRequest request = new RatePlan.ReservationRequest(
            "101", "Guest", "guest@email.com", "12345678901",
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            1, null
        );

        LongStayRatePlan plan = new LongStayRatePlan();
        PriceCalculation price = plan.calculatePrice(request, room);

        assertThat(price.getSubtotal()).isEqualByComparingTo(new BigDecimal("1749.30"));
        assertThat(price.getDiscount()).isEqualByComparingTo(new BigDecimal("174.93"));
        assertThat(price.getTotal()).isEqualByComparingTo(new BigDecimal("1574.37"));
    }
}
