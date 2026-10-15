package br.loja.checkout.entrega;

import br.loja.checkout.Dinheiro;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;

/** Base das opções cobradas como valor fixo + valor por kg. */
abstract class PorPeso implements Entrega {
    private final String codigo;
    private final BigDecimal base;
    private final BigDecimal porKg;
    private final int prazo;

    PorPeso(String codigo, String base, String porKg, int prazo) {
        this.codigo = codigo;
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
        this.prazo = prazo;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return prazo;
    }
}
