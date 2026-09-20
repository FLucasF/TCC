package br.tcc.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
	public static double round(double value) {
		BigDecimal bd = BigDecimal.valueOf(value);
		return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
	}
}
