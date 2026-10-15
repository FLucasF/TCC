package com.loja.checkout.validator;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;

import java.math.BigDecimal;

public class CheckoutValidator {
    public String validar(CheckoutRequest request) {
        if (isCarrinhoInvalido(request)) {
            return "PEDIDO_INVALIDO";
        }

        if (isNivelClubeInvalido(request)) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (isRegiaoInvalida(request)) {
            return "REGIAO_INVALIDA";
        }

        if (isModalidadeInvalida(request)) {
            return "MODALIDADE_INVALIDA";
        }

        if (!modalidadeAtende(request)) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (request.getCupom() != null && !cupomExiste(request.getCupom())) {
            return "CUPOM_INVALIDO";
        }

        if (request.getCupom() != null && !cupomAplicavel(request)) {
            return "CUPOM_NAO_APLICAVEL";
        }

        if (isFormaPagamentoInvalida(request)) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        if (!parcelamentoValido(request)) {
            return "PARCELAMENTO_INVALIDO";
        }

        if (!formaPagamentoAtende(request)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private boolean isCarrinhoInvalido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return true;
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return true;
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return true;
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isNivelClubeInvalido(CheckoutRequest request) {
        if (request.getNivelClube() == null) {
            return true;
        }
        try {
            NivelClube.valueOf(request.getNivelClube());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean isRegiaoInvalida(CheckoutRequest request) {
        if (request.getRegiao() == null) {
            return true;
        }
        try {
            Regiao.valueOf(request.getRegiao());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean isModalidadeInvalida(CheckoutRequest request) {
        if (request.getModalidadeEntrega() == null) {
            return true;
        }
        try {
            ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean modalidadeAtende(CheckoutRequest request) {
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        BigDecimal pesoTotal = calcularPesoTotal(request);
        return modalidade.atende(pesoTotal);
    }

    private boolean cupomExiste(String cupom) {
        try {
            Cupom.valueOf(cupom);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean cupomAplicavel(CheckoutRequest request) {
        Cupom cupom = Cupom.valueOf(request.getCupom());
        BigDecimal subtotal = calcularSubtotalProdutos(request);
        return cupom.aplicavel(subtotal);
    }

    private boolean isFormaPagamentoInvalida(CheckoutRequest request) {
        if (request.getFormaPagamento() == null) {
            return true;
        }
        try {
            FormaPagamento.valueOf(request.getFormaPagamento());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean parcelamentoValido(CheckoutRequest request) {
        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        return forma.permiteParcelamento(parcelas);
    }

    private boolean formaPagamentoAtende(CheckoutRequest request) {
        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());
        BigDecimal total = calcularSubtotalProdutos(request);
        return forma.atende(total);
    }

    private BigDecimal calcularSubtotalProdutos(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : request.getItens()) {
            BigDecimal itemTotal = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (Item item : request.getItens()) {
            BigDecimal itemPeso = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(itemPeso);
        }
        return pesoTotal;
    }
}
