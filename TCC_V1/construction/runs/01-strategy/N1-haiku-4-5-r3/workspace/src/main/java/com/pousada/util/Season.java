package com.pousada.util;

import java.time.LocalDate;
import java.time.Month;

public class Season {
  public static boolean isHighSeason(LocalDate date) {
    Month month = date.getMonth();
    int day = date.getDayOfMonth();
    int monthValue = month.getValue();

    if (month == Month.JULY) {
      return true;
    }

    if (month == Month.DECEMBER && day >= 15) {
      return true;
    }

    if (month == Month.JANUARY || month == Month.FEBRUARY) {
      return true;
    }

    return false;
  }
}
