package marealta;

import java.math.BigDecimal;

public record Quote(BigDecimal subtotal, BigDecimal discount) {
    public BigDecimal total() { return subtotal.subtract(discount); }
}
