package com.pousada.rateplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.pousada.model.Room;
import com.pousada.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlanTest {
  private final StandardRatePlan plan = new StandardRatePlan();

  @Test
  void testStandardDeluxe10To12March2027() {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 10);
    LocalDate checkOut = LocalDate.of(2027, 3, 12);

    RatePlanStrategy.PriceCalculation pricing = plan.calculatePrice(room, checkIn, checkOut);

    assertThat(pricing.nights).isEqualTo(2);
    assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("779.80"));
    assertThat(pricing.discount).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("779.80"));
  }
}
