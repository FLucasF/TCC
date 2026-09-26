package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Cobranca de valor fixo mais um valor por quilo do pedido. */
abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal fixo;
    private final BigDecimal porKg;

    FretePorPeso(String fixo, String porKg) {
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pedido.pesoKg())));
    }
}
