package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Tarifa fixa do banco, sempre a vista e so para pedidos de ate R$ 1.000,00. */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean disponivel(ContextoPagamento contexto) {
        return contexto.totalSemImposto().compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(ContextoPagamento contexto) {
        BigDecimal totalFinal = Dinheiro.centavos(contexto.totalPedido().add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
