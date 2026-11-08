package com.pousada.rateplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.pousada.model.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableTest {
  private final NonRefundableRatePlan plan = new NonRefundableRatePlan();

  @Test
  void testNonRefundableRefund() {
    BigDecimal refund = plan.calculateRefund(new BigDecimal("500.00"));
    assertThat(refund).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void testNonRefundable15PercentDiscount() throws RatePlanStrategy.InvalidReservationException {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 10);
    LocalDate checkOut = LocalDate.of(2027, 3, 12);

    plan.validate(room, checkIn, checkOut, 2, null);
    RatePlanStrategy.PriceCalculation pricing = plan.calculatePrice(room, checkIn, checkOut);

    assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("779.80"));
    assertThat(pricing.discount).isEqualByComparingTo(new BigDecimal("116.97"));
    assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("662.83"));
  }
}
