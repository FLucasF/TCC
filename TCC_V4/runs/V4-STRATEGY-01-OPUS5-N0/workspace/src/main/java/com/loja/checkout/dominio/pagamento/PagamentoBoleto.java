package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Boleto: sempre a vista, com tarifa bancaria de R$ 3,49 e limite de R$ 1.000,00. */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

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
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Moeda.emCentavos(totalPedido.add(TARIFA));
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
