package com.loja.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.pagamento.Cartao;
import com.loja.pedidos.pagamento.Cobranca;

class CartaoPriceTest {

    @Test
    void semJurosAteTresParcelas() {
        Cobranca c = new Cartao().cobrar(Dinheiro.valor("100.00"), 3, false);
        assertEquals(Dinheiro.valor("100.00"), c.totalFinal());
        assertEquals(Dinheiro.valor("33.33"), c.valorParcela());
    }

    @Test
    void jurosPriceAPartirDeQuatroParcelas() {
        Cobranca c = new Cartao().cobrar(Dinheiro.valor("1000.00"), 12, false);
        assertEquals(Dinheiro.valor("94.50"), c.valorParcela());
        assertEquals(Dinheiro.valor("1134.00"), c.totalFinal());
    }
}
