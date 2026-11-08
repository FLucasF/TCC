package com.pousada.rateplan;

import com.pousada.model.Reservation;

public class RatePlanFactory {
  public static RatePlanStrategy create(Reservation.RatePlan ratePlan) {
    return switch (ratePlan) {
      case STANDARD -> new StandardRatePlan();
      case NON_REFUNDABLE -> new NonRefundableRatePlan();
      case CORPORATE -> new CorporateRatePlan();
      case WEEKEND_PACKAGE -> new WeekendPackageRatePlan();
      case LONG_STAY -> new LongStayRatePlan();
    };
  }
}
