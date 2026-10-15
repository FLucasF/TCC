package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

public interface Cupom extends Codificado {

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal desconto(ContextoCupom contexto);
}
