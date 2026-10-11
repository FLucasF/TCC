package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Nível do cliente no clube da loja. Cada nível define o próprio conjunto de vantagens. */
public interface NivelClube extends Codificado {

    /** Frete que o cliente paga, a partir do frete da opção de entrega. */
    BigDecimal frete(BigDecimal freteEntrega);

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
