package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    public String codigo() { return "BOLETO"; }
    public boolean parcelamentoValido(int parcelas) { return parcelas == 1; }
    public boolean atende(BigDecimal totalPedido) { return totalPedido.compareTo(LIMITE) <= 0; }

    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = Dinheiro.emCentavos(TARIFA);
        BigDecimal totalFinal = Dinheiro.emCentavos(totalPedido.add(ajuste));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal, 1);
    }
}
