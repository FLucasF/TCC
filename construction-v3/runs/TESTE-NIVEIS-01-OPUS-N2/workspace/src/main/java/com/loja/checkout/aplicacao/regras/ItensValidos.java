package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.dominio.CodigoErro;
import java.math.BigDecimal;
import java.util.Optional;

/** Carrinho vazio ou item com preco, quantidade ou peso zerado, negativo ou ausente. */
public class ItensValidos implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        boolean valido = !rascunho.itensBrutos().isEmpty()
                && rascunho.itensBrutos().stream().allMatch(this::itemValido);
        return valido ? Optional.empty() : Optional.of(CodigoErro.PEDIDO_INVALIDO);
    }

    private boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
