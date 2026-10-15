package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Opção de entrega. Para criar uma nova, basta registrar outro bean desta interface. */
public interface Entrega {

    String codigo();

    boolean atende(BigDecimal pesoKg);

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
