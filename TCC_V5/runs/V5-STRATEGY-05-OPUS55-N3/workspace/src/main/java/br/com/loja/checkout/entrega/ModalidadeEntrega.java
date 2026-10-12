package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

import br.com.loja.checkout.Codificado;

public interface ModalidadeEntrega extends Codificado {

    boolean atende(BigDecimal pesoKg);

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
