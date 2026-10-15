package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.ItemRequest;
import com.loja.model.FormaPagamento;
import com.loja.model.ItemCarrinho;
import com.loja.model.ModalidadeEntrega;
import com.loja.model.NivelClube;
import com.loja.model.Regiao;
import com.loja.model.cupom.CupomRegistry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Validador {
    public Optional<String> validar(CheckoutRequest request) {
        if (pedidoInvalido(request)) {
            return Optional.of("PEDIDO_INVALIDO");
        }

        if (nivelInvalido(request.getNivelClube())) {
            return Optional.of("NIVEL_CLUBE_INVALIDO");
        }

        if (regiaoInvalida(request.getRegiao())) {
            return Optional.of("REGIAO_INVALIDA");
        }

        if (modalidadeInvalida(request.getModalidadeEntrega())) {
            return Optional.of("MODALIDADE_INVALIDA");
        }

        if (modalidadeIndisponivel(request)) {
            return Optional.of("MODALIDADE_INDISPONIVEL");
        }

        if (cupomInvalido(request.getCupom())) {
            return Optional.of("CUPOM_INVALIDO");
        }

        if (cupomNaoAplicavel(request)) {
            return Optional.of("CUPOM_NAO_APLICAVEL");
        }

        if (formaPagamentoInvalida(request.getFormaPagamento())) {
            return Optional.of("FORMA_PAGAMENTO_INVALIDA");
        }

        if (parcelamentoInvalido(request)) {
            return Optional.of("PARCELAMENTO_INVALIDO");
        }

        if (formaPagamentoIndisponivel(request)) {
            return Optional.of("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return Optional.empty();
    }

    private boolean pedidoInvalido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return true;
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getNome() == null || item.getNome().isEmpty()) return true;
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) return true;
            if (item.getQuantidade() <= 0) return true;
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) return true;
        }

        return false;
    }

    private boolean nivelInvalido(String nivel) {
        if (nivel == null || nivel.isEmpty()) {
            return true;
        }
        try {
            NivelClube.valueOf(nivel);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean regiaoInvalida(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            return true;
        }
        try {
            Regiao.valueOf(regiao);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean modalidadeInvalida(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            return true;
        }
        try {
            ModalidadeEntrega.valueOf(modalidade);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean modalidadeIndisponivel(CheckoutRequest request) {
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());

        if (modalidade.isMototoy()) {
            BigDecimal pesoTotal = request.getItens().stream()
                    .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return pesoTotal.compareTo(new BigDecimal("5")) > 0;
        }

        return false;
    }

    private boolean cupomInvalido(String cupom) {
        if (cupom == null || cupom.isEmpty()) {
            return false;
        }
        return !CupomRegistry.obter(cupom).isPresent();
    }

    private boolean cupomNaoAplicavel(CheckoutRequest request) {
        if (request.getCupom() == null || request.getCupom().isEmpty()) {
            return false;
        }

        BigDecimal subtotal = calcularSubtotal(request.getItens());
        return !CupomRegistry.podeAplicar(request.getCupom(), subtotal);
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean formaPagamentoInvalida(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            return true;
        }
        try {
            FormaPagamento.valueOf(formaPagamento);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean parcelamentoInvalido(CheckoutRequest request) {
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());

        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            return parcelas != 1;
        }

        if (forma == FormaPagamento.CARTAO) {
            return parcelas < 1 || parcelas > 12;
        }

        return false;
    }

    private boolean formaPagamentoIndisponivel(CheckoutRequest request) {
        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());

        if (forma == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotal(request.getItens());
            return subtotal.compareTo(new BigDecimal("1000.00")) > 0;
        }

        return false;
    }
}
