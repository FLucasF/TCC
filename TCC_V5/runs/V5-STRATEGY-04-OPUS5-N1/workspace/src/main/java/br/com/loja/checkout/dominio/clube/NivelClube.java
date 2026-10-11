package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.MotivoRecusa;

/** Um nível do clube da loja, com o seu conjunto de vantagens. */
public interface NivelClube {

    Catalogo<NivelClube> CATALOGO = Catalogo.de(
            MotivoRecusa.NIVEL_CLUBE_INVALIDO,
            NivelClube::codigo,
            List.of(new Bronze(), new Prata(), new Ouro()));

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
