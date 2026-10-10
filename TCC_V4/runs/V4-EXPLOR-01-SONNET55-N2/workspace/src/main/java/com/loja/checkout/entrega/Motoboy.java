package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    public String codigo() { return "MOTOBOY"; }

    public int prazoDias() { return 0; }

    public boolean atende(Carrinho carrinho) { return carrinho.pesoTotal().compareTo(PESO_MAXIMO) <= 0; }

    public BigDecimal frete(Carrinho carrinho) { return Dinheiro.valor("18.00"); }
}
