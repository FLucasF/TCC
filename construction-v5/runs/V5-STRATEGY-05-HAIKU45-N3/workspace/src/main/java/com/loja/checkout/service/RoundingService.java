package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class RoundingService {
  public double round(double value) {
    BigDecimal bd = new BigDecimal(value);
    return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
  }
}
