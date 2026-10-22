package com.posadamareaita.util;

import com.posadamareaita.domain.RatePlan;
import com.posadamareaita.domain.Room;
import com.posadamareaita.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PriceCalculatorTest {
    @Test
    public void testStandardRateNormalSeason() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculatePrice(room, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("499.80"), result.total);
    }

    @Test
    public void testHighSeasonMultiplier() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculatePrice(room, checkIn, checkOut, RatePlan.STANDARD);

        BigDecimal nightPrice = new BigDecimal("249.90").multiply(new BigDecimal("1.20"));
        nightPrice = MoneyUtil.round(nightPrice);
        BigDecimal expectedSubtotal = nightPrice.multiply(new BigDecimal("2"));

        assertEquals(expectedSubtotal, result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
    }

    @Test
    public void testNonRefundableDiscount() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculatePrice(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        BigDecimal expectedDiscount = new BigDecimal("499.80").multiply(new BigDecimal("0.15"));
        expectedDiscount = MoneyUtil.round(expectedDiscount);

        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(expectedDiscount, result.discount);
        assertEquals(result.subtotal.subtract(expectedDiscount), result.total);
    }
}
