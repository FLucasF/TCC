package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    @Test
    void testStandardRatePlanHighSeason() {
        StandardRatePlan plan = new StandardRatePlan();
        PriceCalculation price = plan.calculatePrice(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2
        );

        assertEquals(new BigDecimal("599.76"), price.subtotal());
        assertEquals(BigDecimal.ZERO, price.discount());
        assertEquals(new BigDecimal("599.76"), price.total());
    }

    @Test
    void testCorporateTecnosulDeluxe() {
        CorporateRatePlan plan = new CorporateRatePlan();
        PriceCalculation price = plan.calculatePrice(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "TECNOSUL"
        );

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void testWeekendPackageDeluxe() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        assertDoesNotThrow(() -> {
            plan.validate(
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 14),
                2,
                null
            );
        });

        PriceCalculation price = plan.calculatePrice(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2
        );

        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(BigDecimal.ZERO, price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void testWeekendPackageRefund() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));

        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testLongStayStandard() {
        LongStayRatePlan plan = new LongStayRatePlan();

        assertDoesNotThrow(() -> {
            plan.validate(
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 8),
                7,
                null
            );
        });

        PriceCalculation price = plan.calculatePrice(
            RoomType.STANDARD,
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            7
        );

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void testCorporateInvalidCompanyCode() {
        CorporateRatePlan plan = new CorporateRatePlan();

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                "INVALID"
            );
        });
    }

    @Test
    void testWeekendPackageNotFriday() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(
                LocalDate.of(2027, 3, 13),
                LocalDate.of(2027, 3, 15),
                2,
                null
            );
        });
    }

    @Test
    void testLongStayTooShort() {
        LongStayRatePlan plan = new LongStayRatePlan();

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 7),
                6,
                null
            );
        });
    }

    @Test
    void testHighSeasonDetection() {
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));

        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }
}
