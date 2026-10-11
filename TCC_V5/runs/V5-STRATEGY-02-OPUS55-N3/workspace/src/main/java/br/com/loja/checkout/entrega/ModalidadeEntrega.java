package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

public interface ModalidadeEntrega extends Codificado {

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
