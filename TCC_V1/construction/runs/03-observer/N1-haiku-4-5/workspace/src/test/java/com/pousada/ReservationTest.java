package com.pousada;

import static org.junit.Assert.*;

import com.pousada.database.RoomRepository;
import com.pousada.database.ReservationRepository;
import com.pousada.database.MessageRepository;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.service.PricingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;

public class ReservationTest {
  @Test
  public void testPricingCalculation() {
    LocalDate checkIn = LocalDate.of(2027, 7, 10);
    LocalDate checkOut = LocalDate.of(2027, 7, 12);

    BigDecimal nightlyRate = new BigDecimal("249.90");
    PricingService.PriceCalculation pricing =
        PricingService.calculate(checkIn, checkOut, nightlyRate, "STANDARD");

    assertEquals(2, pricing.nights);
    assertEquals(0, pricing.subtotal.compareTo(new BigDecimal("599.76")));
    assertEquals(0, pricing.discount.compareTo(new BigDecimal("0.00")));
    assertEquals(0, pricing.total.compareTo(new BigDecimal("599.76")));
  }

  @Test
  public void testPricingWithDiscount() {
    LocalDate checkIn = LocalDate.of(2027, 7, 10);
    LocalDate checkOut = LocalDate.of(2027, 7, 12);

    BigDecimal nightlyRate = new BigDecimal("249.90");
    PricingService.PriceCalculation pricing =
        PricingService.calculate(checkIn, checkOut, nightlyRate, "NON_REFUNDABLE");

    assertEquals(2, pricing.nights);
    assertEquals(0, pricing.subtotal.compareTo(new BigDecimal("599.76")));
    assertEquals(0, pricing.discount.compareTo(new BigDecimal("89.96")));
    assertEquals(0, pricing.total.compareTo(new BigDecimal("509.80")));
  }

  @Test
  public void testHighSeasonPricing() {
    LocalDate checkIn = LocalDate.of(2027, 12, 20);
    LocalDate checkOut = LocalDate.of(2027, 12, 22);

    BigDecimal nightlyRate = new BigDecimal("249.90");
    PricingService.PriceCalculation pricing =
        PricingService.calculate(checkIn, checkOut, nightlyRate, "STANDARD");

    assertEquals(2, pricing.nights);
    assertEquals(0, pricing.subtotal.compareTo(new BigDecimal("599.76")));
  }

  @Test
  public void testRoundingHalfEven() {
    BigDecimal value = new BigDecimal("123.455");
    BigDecimal rounded = PricingService.roundHalfEven(value);
    assertEquals(0, rounded.compareTo(new BigDecimal("123.46")));

    value = new BigDecimal("123.445");
    rounded = PricingService.roundHalfEven(value);
    assertEquals(0, rounded.compareTo(new BigDecimal("123.44")));
  }
}
