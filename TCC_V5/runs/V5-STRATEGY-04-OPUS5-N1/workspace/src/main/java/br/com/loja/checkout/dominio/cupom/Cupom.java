package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.MotivoRecusa;

/** Uma promoção do marketing. Cada cupom novo é uma implementação nova. */
public interface Cupom {

    Catalogo<Cupom> CATALOGO = Catalogo.de(
            MotivoRecusa.CUPOM_INVALIDO,
            Cupom::codigo,
            List.of(new BemVindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()));

    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    boolean aplicavel(ContextoCupom contexto);

    BigDecimal desconto(ContextoCupom contexto);
}
