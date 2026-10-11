package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {
    public String codigo() { return "LEVE3PAGUE2"; }
    public boolean aplicavel(Pedido p) { return true; }
    public BigDecimal desconto(Pedido p, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido.Item item : p.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Dinheiro.emCentavos(total);
    }
}
