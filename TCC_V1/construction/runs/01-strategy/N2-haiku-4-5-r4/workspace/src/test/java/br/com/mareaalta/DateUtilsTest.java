package br.com.mareaalta;

import br.com.mareaalta.util.DateUtils;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class DateUtilsTest {
    @Test
    public void testHighSeason() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 2, 10)));

        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testNightsBetween() {
        assertEquals(2, DateUtils.nightsBetween(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12)));
        assertEquals(7, DateUtils.nightsBetween(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8)));
    }

    @Test
    public void testWeekendDates() {
        LocalDate friday = LocalDate.of(2027, 3, 12);
        assertTrue(DateUtils.isFriday(friday));

        LocalDate sunday = DateUtils.nextSunday(friday);
        assertTrue(DateUtils.isSunday(sunday));
        assertEquals(LocalDate.of(2027, 3, 14), sunday);
    }
}
