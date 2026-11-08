package com.pousada.rateplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pousada.model.Room;
import com.pousada.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WeekendPackageTest {
  private final WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

  @Test
  void testDeluxeWeekendPackage12To14March2027() throws RatePlanStrategy.InvalidReservationException {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
    LocalDate checkOut = LocalDate.of(2027, 3, 14); // Sunday

    plan.validate(room, checkIn, checkOut, 2, null);
    RatePlanStrategy.PriceCalculation pricing = plan.calculatePrice(room, checkIn, checkOut);

    assertThat(pricing.nights).isEqualTo(2);
    assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("820.00"));
    assertThat(pricing.discount).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("820.00"));
  }

  @Test
  void testWeekendPackageRefund() {
    BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
    assertThat(refund).isEqualByComparingTo(new BigDecimal("410.00"));
  }

  @Test
  void testNotFridayToSunday() {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 11); // Thursday
    LocalDate checkOut = LocalDate.of(2027, 3, 13); // Saturday

    assertThatThrownBy(() -> plan.validate(room, checkIn, checkOut, 2, null))
        .isInstanceOf(RatePlanStrategy.InvalidReservationException.class)
        .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
  }
}
