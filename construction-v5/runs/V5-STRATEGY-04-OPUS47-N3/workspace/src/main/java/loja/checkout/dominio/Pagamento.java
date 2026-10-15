package loja.checkout.dominio;

import java.math.BigDecimal;

public record Pagamento(BigDecimal ajuste, BigDecimal valorParcela) {
}
