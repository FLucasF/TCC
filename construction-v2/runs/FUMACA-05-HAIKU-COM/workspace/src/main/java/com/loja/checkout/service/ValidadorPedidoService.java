package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinhoRequest;
import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.Entrega;
import com.loja.checkout.model.FormaPagamento;

public class ValidadorPedidoService {

    public static void validar(ResumoCheckoutRequest request) throws ValidacaoException {
        validarPedido(request);
        validarModalidadeEntrega(request);
        validarCupom(request);
        validarFormaPagamento(request);
    }

    private static void validarPedido(ResumoCheckoutRequest request) throws ValidacaoException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ValidacaoException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinhoRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                throw new ValidacaoException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new ValidacaoException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg() <= 0) {
                throw new ValidacaoException("PEDIDO_INVALIDO");
            }
        }
    }

    private static void validarModalidadeEntrega(ResumoCheckoutRequest request) throws ValidacaoException {
        String modalidade = request.getModalidadeEntrega();

        if (modalidade == null || modalidade.isEmpty()) {
            throw new ValidacaoException("MODALIDADE_INVALIDA");
        }

        Entrega entrega = Entrega.obter(modalidade);
        if (entrega == null) {
            throw new ValidacaoException("MODALIDADE_INVALIDA");
        }

        double pesoTotal = calcularPesoTotal(request.getItens());
        if (!entrega.isDisponivel(pesoTotal)) {
            throw new ValidacaoException("MODALIDADE_INDISPONIVEL");
        }
    }

    private static void validarCupom(ResumoCheckoutRequest request) throws ValidacaoException {
        String cupomCodigo = request.getCupom();

        if (cupomCodigo == null) {
            return;
        }

        if ("LEVE3PAGUE2".equals(cupomCodigo) || "FRETEGRATIS".equals(cupomCodigo)) {
            return;
        }

        Cupom cupom = Cupom.obter(cupomCodigo);
        if (cupom == null) {
            throw new ValidacaoException("CUPOM_INVALIDO");
        }

        double subtotal = calcularSubtotalProdutos(request.getItens(), cupomCodigo);
        if (!cupom.isAplicavel(subtotal)) {
            throw new ValidacaoException("CUPOM_NAO_APLICAVEL");
        }
    }

    private static void validarFormaPagamento(ResumoCheckoutRequest request) throws ValidacaoException {
        String formaPagamentoCodigo = request.getFormaPagamento();
        Integer parcelas = request.getParcelas();

        if (formaPagamentoCodigo == null || formaPagamentoCodigo.isEmpty()) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamento.obter(formaPagamentoCodigo);
        if (formaPagamento == null) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INVALIDA");
        }

        int numParcelas = parcelas != null ? parcelas : 1;

        if (!formaPagamento.getParcelasPermitidas().contains(numParcelas)) {
            throw new ValidacaoException("PARCELAMENTO_INVALIDO");
        }

        double totalPedido = calcularTotalPedido(request);
        if (!formaPagamento.isDisponivel(totalPedido, numParcelas)) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private static double calcularPesoTotal(java.util.List<ItemCarrinhoRequest> itens) {
        double pesoTotal = 0.0;
        for (ItemCarrinhoRequest item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private static double calcularSubtotalProdutos(java.util.List<ItemCarrinhoRequest> itens, String cupomCodigo) {
        double subtotal = 0.0;

        for (ItemCarrinhoRequest item : itens) {
            double precoItem = item.getPrecoUnitario() * item.getQuantidade();
            subtotal += precoItem;
        }

        return subtotal;
    }

    private static double calcularTotalPedido(ResumoCheckoutRequest request) {
        double subtotal = calcularSubtotalProdutos(request.getItens(), request.getCupom());
        double desconto = 0.0;

        if (request.getCupom() != null) {
            if ("LEVE3PAGUE2".equals(request.getCupom())) {
                for (ItemCarrinhoRequest item : request.getItens()) {
                    int unidadesGratis = item.getQuantidade() / 3;
                    desconto += unidadesGratis * item.getPrecoUnitario();
                }
            } else {
                Cupom cupom = Cupom.obter(request.getCupom());
                if (cupom != null && !"FRETEGRATIS".equals(request.getCupom())) {
                    desconto = cupom.calcularDesconto(subtotal);
                }
            }
        }

        double pesoTotal = calcularPesoTotal(request.getItens());
        Entrega entrega = Entrega.obter(request.getModalidadeEntrega());
        double frete = 0.0;
        if (entrega != null) {
            frete = entrega.calcularFrete(pesoTotal);
        }

        if ("FRETEGRATIS".equals(request.getCupom())) {
            desconto += frete;
            frete = 0.0;
        }

        return subtotal - desconto + frete;
    }

}
