package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Expressa implements ModalidadeEntrega {
    public String codigo() { return "EXPRESSA"; }

    public int prazoDias() { return 2; }

    public boolean atende(Carrinho carrinho) { return true; }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(carrinho.pesoTotal())));
    }
}
