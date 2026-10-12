package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Frete cobrado como valor fixo + valor por kg do pedido. */
public abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected FretePorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return valorFixo.add(valorPorKg.multiply(pedido.pesoKg()));
    }
}
