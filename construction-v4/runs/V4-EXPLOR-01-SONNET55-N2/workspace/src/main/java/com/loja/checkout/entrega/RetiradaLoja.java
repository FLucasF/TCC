package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {
    public String codigo() { return "RETIRADA_LOJA"; }

    public int prazoDias() { return 1; }

    public boolean atende(Carrinho carrinho) { return true; }

    public BigDecimal frete(Carrinho carrinho) { return Dinheiro.arredondar(BigDecimal.ZERO); }
}
