package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A vista, com a tarifa do banco somada ao total, e so ate R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        return ResultadoPagamento.aVista(totalPedido.add(TARIFA));
    }
}
