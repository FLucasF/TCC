package com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoFormaPagamento implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal VALOR_MAXIMO_PEDIDO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(VALOR_MAXIMO_PEDIDO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(valorFinal, valorFinal);
    }
}
