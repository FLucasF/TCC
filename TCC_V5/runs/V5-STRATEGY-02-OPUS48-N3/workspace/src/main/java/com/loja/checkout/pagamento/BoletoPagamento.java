package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** À vista, com tarifa bancária de R$ 3,49, e só para total até R$ 1.000,00. */
@Component
public class BoletoPagamento implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new ResultadoPagamento(valorFinal, valorFinal);
    }
}
