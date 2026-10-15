package com.loja.checkout.validacao;

import com.loja.checkout.domain.*;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;

import java.math.BigDecimal;
import java.util.Optional;

public class ValidadorPedido {

    public Optional<CodigoErro> validar(
        PedidoRequest request,
        ModalidadeEntrega modalidade,
        NivelClube nivel,
        Regiao regiao,
        Cupom cupom,
        FormaPagamento formaPagamento,
        double pesoTotal,
        BigDecimal subtotalProdutos,
        BigDecimal totalPedido,
        java.util.function.Supplier<com.loja.checkout.domain.cupom.ContextoCupom> contextoCupomSupplier
    ) {
        if (!validarCarrinho(request)) {
            return Optional.of(CodigoErro.PEDIDO_INVALIDO);
        }

        if (nivel == null) {
            return Optional.of(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        if (regiao == null) {
            return Optional.of(CodigoErro.REGIAO_INVALIDA);
        }

        if (modalidade == null) {
            return Optional.of(CodigoErro.MODALIDADE_INVALIDA);
        }

        if (!modalidade.aceitaPedido(pesoTotal)) {
            return Optional.of(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        if (request.cupom() != null && !request.cupom().isBlank() && cupom == null) {
            return Optional.of(CodigoErro.CUPOM_INVALIDO);
        }

        if (cupom != null && !cupom.aplicavel(contextoCupomSupplier.get())) {
            return Optional.of(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        if (formaPagamento == null) {
            return Optional.of(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.validarParcelas(parcelas)) {
            return Optional.of(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        if (!formaPagamento.aceitaTotal(totalPedido)) {
            return Optional.of(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        return Optional.empty();
    }

    private boolean validarCarrinho(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            return false;
        }

        for (var item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0) return false;
            if (item.quantidade() == null || item.quantidade() <= 0) return false;
            if (item.pesoKg() == null || item.pesoKg() <= 0) return false;
        }

        return true;
    }
}
