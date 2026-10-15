package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com tarifa de R$ 3,49 somada ao total; não aceita acima de R$ 1.000. */
@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

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
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new PagamentoResultado(totalFinal, totalFinal);
    }
}
