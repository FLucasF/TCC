package com.pousada;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.service.ReservationService;
import com.pousada.util.Money;
import com.pousada.util.DateUtil;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {

    @Test
    public void testMoneyRounding() {
        BigDecimal value = new BigDecimal("249.9");
        BigDecimal rounded = Money.round(value);
        assertEquals("249.90", rounded.toPlainString());
    }

    @Test
    public void testHighSeasonDetection() {
        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        assertTrue("July should be high season", DateUtil.isHighSeason(julyDate));

        LocalDate decemberHighDate = LocalDate.of(2027, 12, 15);
        assertTrue("Dec 15+ should be high season", DateUtil.isHighSeason(decemberHighDate));

        LocalDate decemberLowDate = LocalDate.of(2027, 12, 14);
        assertFalse("Dec 1-14 should not be high season", DateUtil.isHighSeason(decemberLowDate));

        LocalDate januaryDate = LocalDate.of(2027, 1, 10);
        assertTrue("January should be high season", DateUtil.isHighSeason(januaryDate));

        LocalDate aprilDate = LocalDate.of(2027, 4, 10);
        assertFalse("April should not be high season", DateUtil.isHighSeason(aprilDate));
    }

    @Test
    public void testNightsCounting() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = DateUtil.countNights(checkIn, checkOut);
        assertEquals("Should be 2 nights", 2, nights);
    }

    @Test
    public void testMoneyPercentageCalculation() {
        BigDecimal value = new BigDecimal("1000.00");
        BigDecimal discount = Money.percentage(value, 15);
        assertEquals("15% of 1000 should be 150", "150.00", discount.toPlainString());
    }

    @Test
    public void testMoneyAddition() {
        BigDecimal a = new BigDecimal("299.88");
        BigDecimal b = new BigDecimal("299.88");
        BigDecimal result = Money.add(a, b);
        assertEquals("299.88 + 299.88 should be 599.76", "599.76", result.toPlainString());
    }

    @Test
    public void testDateFormatting() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        String formatted = DateUtil.formatDate(date);
        assertEquals("10/03/2027", formatted);
    }

    @Test
    public void testDateFormattingForCsv() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        String formatted = DateUtil.formatDateForCsv(date);
        assertEquals("2027-03-10", formatted);
    }
}
