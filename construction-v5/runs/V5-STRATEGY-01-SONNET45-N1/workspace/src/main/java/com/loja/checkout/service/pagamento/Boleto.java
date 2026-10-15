package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceita(BigDecimal totalPedido, int parcelas) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularValorFinal(totalPedido, parcelas);
    }
}
