package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final String codigo;
    private final int prazoDias;
    private final BigDecimal base;
    private final BigDecimal porKg;

    EntregaPorPeso(String codigo, int prazoDias, String base, String porKg) {
        this.codigo = codigo;
        this.prazoDias = prazoDias;
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int prazoDias() {
        return prazoDias;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pedido.pesoKg())));
    }
}
