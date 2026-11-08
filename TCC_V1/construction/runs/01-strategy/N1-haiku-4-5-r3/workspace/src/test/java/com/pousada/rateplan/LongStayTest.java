package com.pousada.rateplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pousada.model.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayTest {
  private final LongStayRatePlan plan = new LongStayRatePlan();

  @Test
  void testStandardLongStay01To08March2027() throws RatePlanStrategy.InvalidReservationException {
    Room room = new Room(101, Room.Type.STANDARD, 2, new BigDecimal("249.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 1);
    LocalDate checkOut = LocalDate.of(2027, 3, 8);

    plan.validate(room, checkIn, checkOut, 1, null);
    RatePlanStrategy.PriceCalculation pricing = plan.calculatePrice(room, checkIn, checkOut);

    assertThat(pricing.nights).isEqualTo(7);
    assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("1749.30"));
    assertThat(pricing.discount).isEqualByComparingTo(new BigDecimal("174.93"));
    assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("1574.37"));
  }

  @Test
  void testMinimum7Nights() {
    Room room = new Room(101, Room.Type.STANDARD, 2, new BigDecimal("249.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 1);
    LocalDate checkOut = LocalDate.of(2027, 3, 7); // Only 6 nights

    assertThatThrownBy(() -> plan.validate(room, checkIn, checkOut, 1, null))
        .isInstanceOf(RatePlanStrategy.InvalidReservationException.class)
        .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
  }

  @Test
  void testLongStayRefund() {
    BigDecimal refund = plan.calculateRefund(new BigDecimal("1574.37"));
    assertThat(refund).isEqualByComparingTo(new BigDecimal("1574.37"));
  }
}
