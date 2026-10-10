package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {
    public String codigo() { return "ECONOMICA"; }

    public int prazoDias() { return 7; }

    public boolean atende(Carrinho carrinho) { return true; }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoTotal())));
    }
}
