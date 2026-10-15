package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements FormaPagamento {
    public String codigo() { return "PIX"; }

    public boolean aceitaParcelas(int parcelas) { return parcelas == 1; }

    public boolean disponivel(BigDecimal totalPedido) { return true; }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        BigDecimal total = totalPedido.subtract(desconto);
        return new Cobranca(total, total);
    }
}
