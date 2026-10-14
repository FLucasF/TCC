package br.com.loja.pedidos.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;

@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.porcentagem(subtotalProdutos, new BigDecimal("2"));
    }
}
