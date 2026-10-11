package com.loja.modalidade;

import com.loja.domain.Pedido;
import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {

    @Override
    public boolean aceita(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal taxa = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        BigDecimal frete = taxa.add(porKg.multiply(pedido.pesoTotal()));
        return Arredondador.arredondar(frete);
    }

    @Override
    public int prazoEmDias() {
        return 2;
    }
}
