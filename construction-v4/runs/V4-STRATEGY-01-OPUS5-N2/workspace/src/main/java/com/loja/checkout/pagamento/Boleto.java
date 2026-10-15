package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Tarifa de R$ 3,49 do banco, somada ao total. Nao aceito acima de R$ 1.000,00. */
@Component
public class Boleto implements PagamentoAVista {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public BigDecimal totalFinal(BigDecimal totalPedido) {
        return Dinheiro.arredonda(totalPedido.add(TARIFA));
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }
}
