package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ValidacaoService {

    public String validar(CheckoutRequest request) {
        if (erroCarrinho(request)) {
            return "PEDIDO_INVALIDO";
        }

        if (erroNivelClube(request)) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (erroRegiao(request)) {
            return "REGIAO_INVALIDA";
        }

        if (erroModalidadeExiste(request)) {
            return "MODALIDADE_INVALIDA";
        }

        if (erroModalidadeIndisponivel(request)) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (erroCupomExiste(request)) {
            return "CUPOM_INVALIDO";
        }

        BigDecimal subtotal = calcularSubtotal(request.getItens());
        if (erroCupomAplicavel(request, subtotal)) {
            return "CUPOM_NAO_APLICAVEL";
        }

        if (erroFormaPagamento(request)) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        if (erroParcelamento(request)) {
            return "PARCELAMENTO_INVALIDO";
        }

        if (erroFormaPagamentoIndisponivel(request, subtotal)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private boolean erroCarrinho(CheckoutRequest request) {
        List<Item> itens = request.getItens();
        if (itens == null || itens.isEmpty()) {
            return true;
        }

        for (Item item : itens) {
            if (item.getNome() == null || item.getNome().isEmpty() ||
                item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                return true;
            }
        }

        return false;
    }

    private boolean erroNivelClube(CheckoutRequest request) {
        return NivelClube.fromString(request.getNivelClube()) == null;
    }

    private boolean erroRegiao(CheckoutRequest request) {
        return Regiao.fromString(request.getRegiao()) == null;
    }

    private boolean erroModalidadeExiste(CheckoutRequest request) {
        return ModalidadeEntrega.fromString(request.getModalidadeEntrega()) == null;
    }

    private boolean erroModalidadeIndisponivel(CheckoutRequest request) {
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(request.getModalidadeEntrega());
        if (modalidade == null) {
            return false;
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        if (pesoTotal.compareTo(BigDecimal.valueOf(modalidade.getPesoMaximoKg())) > 0) {
            return true;
        }

        return false;
    }

    private boolean erroCupomExiste(CheckoutRequest request) {
        if (request.getCupom() == null) {
            return false;
        }
        return Cupom.fromString(request.getCupom()) == null;
    }

    private boolean erroCupomAplicavel(CheckoutRequest request, BigDecimal subtotal) {
        if (request.getCupom() == null) {
            return false;
        }
        Cupom cupom = Cupom.fromString(request.getCupom());
        if (cupom == null) {
            return false;
        }
        return !cupom.validar(subtotal, request.getItens());
    }

    private boolean erroFormaPagamento(CheckoutRequest request) {
        return FormaPagamento.fromString(request.getFormaPagamento()) == null;
    }

    private boolean erroParcelamento(CheckoutRequest request) {
        FormaPagamento forma = FormaPagamento.fromString(request.getFormaPagamento());
        if (forma == null) {
            return false;
        }
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        return !forma.isParcelavelado(parcelas);
    }

    private boolean erroFormaPagamentoIndisponivel(CheckoutRequest request, BigDecimal subtotal) {
        FormaPagamento forma = FormaPagamento.fromString(request.getFormaPagamento());
        if (forma != FormaPagamento.BOLETO) {
            return false;
        }

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(request.getModalidadeEntrega());
        NivelClube clube = NivelClube.fromString(request.getNivelClube());
        Regiao regiao = Regiao.fromString(request.getRegiao());

        BigDecimal frete = modalidade.calcularFrete(calcularPesoTotal(request.getItens()));
        if (clube.isDeFrete()) {
            frete = BigDecimal.ZERO;
        }

        BigDecimal seguro = subtotal.multiply(regiao.getPercentualSeguro());

        BigDecimal total = subtotal.add(frete).add(seguro);
        if (request.getCupom() != null) {
            Cupom cupom = Cupom.fromString(request.getCupom());
            BigDecimal desconto = cupom.calcularDesconto(subtotal, request.getItens(), frete);
            total = total.subtract(desconto);
        }

        return total.compareTo(BigDecimal.valueOf(1000)) > 0;
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
