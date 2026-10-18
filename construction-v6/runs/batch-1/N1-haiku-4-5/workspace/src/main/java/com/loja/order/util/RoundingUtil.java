package com.loja.order.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RoundingUtil {
  public static BigDecimal roundHalfEven(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }

  public static int ceilDays(double hours) {
    return (int) Math.ceil(hours / 24.0);
  }
}
