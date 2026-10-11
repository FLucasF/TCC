package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {
    public String codigo() { return "PIX"; }
    public boolean parcelamentoValido(int parcelas) { return parcelas == 1; }
    public boolean atende(BigDecimal totalPedido) { return true; }

    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = Dinheiro.emCentavos(totalPedido.multiply(new BigDecimal("-0.05")));
        BigDecimal totalFinal = Dinheiro.emCentavos(totalPedido.add(ajuste));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal, 1);
    }
}
