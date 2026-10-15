package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean aceita(BigDecimal totalPedido, int parcelas) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(DESCONTO)
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(DESCONTO)
            .setScale(2, RoundingMode.HALF_EVEN);
        return totalPedido.subtract(desconto);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularValorFinal(totalPedido, parcelas);
    }
}
