package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return pedido.pesoTotal().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.arredondar(TAXA_FIXA);
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
