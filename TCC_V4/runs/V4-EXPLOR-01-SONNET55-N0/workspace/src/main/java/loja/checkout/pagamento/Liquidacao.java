package loja.checkout.pagamento;

import java.math.BigDecimal;

public record Liquidacao(BigDecimal totalFinal, BigDecimal valorParcela) {}
