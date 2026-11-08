package com.pousada.rateplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pousada.model.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CorporateRatePlanTest {
  private final CorporateRatePlan plan = new CorporateRatePlan();

  @Test
  void testTecnosulDeluxe10To12March2027() throws RatePlanStrategy.InvalidReservationException {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 10);
    LocalDate checkOut = LocalDate.of(2027, 3, 12);

    plan.validate(room, checkIn, checkOut, 2, "TECNOSUL");
    RatePlanStrategy.PriceCalculation pricing = plan.calculatePrice(room, checkIn, checkOut, "TECNOSUL");

    assertThat(pricing.nights).isEqualTo(2);
    assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("779.80"));
    assertThat(pricing.discount).isEqualByComparingTo(new BigDecimal("93.58"));
    assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("686.22"));
  }

  @Test
  void testMissingCompanyCode() {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 10);
    LocalDate checkOut = LocalDate.of(2027, 3, 12);

    assertThatThrownBy(() -> plan.validate(room, checkIn, checkOut, 2, null))
        .isInstanceOf(RatePlanStrategy.InvalidReservationException.class)
        .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
  }

  @Test
  void testInvalidCompanyCode() {
    Room room = new Room(201, Room.Type.DELUXE, 3, new BigDecimal("389.90"));
    LocalDate checkIn = LocalDate.of(2027, 3, 10);
    LocalDate checkOut = LocalDate.of(2027, 3, 12);

    assertThatThrownBy(() -> plan.validate(room, checkIn, checkOut, 2, "INVALID"))
        .isInstanceOf(RatePlanStrategy.InvalidReservationException.class)
        .hasFieldOrPropertyWithValue("errorCode", "INVALID_REQUEST");
  }
}
