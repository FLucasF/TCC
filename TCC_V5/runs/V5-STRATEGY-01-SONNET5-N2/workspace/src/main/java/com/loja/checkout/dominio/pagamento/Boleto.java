package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = BigDecimal.valueOf(3.49);
    private static final BigDecimal LIMITE_TOTAL = BigDecimal.valueOf(1000.00);

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido, int parcelas) {
        return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new ResultadoPagamento(TARIFA.setScale(2), totalFinal, totalFinal);
    }
}
