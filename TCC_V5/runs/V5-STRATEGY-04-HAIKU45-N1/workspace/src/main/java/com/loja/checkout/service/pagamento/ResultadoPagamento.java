package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
    BigDecimal ajuste,
    int parcelas,
    BigDecimal valorParcela
) {}
