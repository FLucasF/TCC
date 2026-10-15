package com.loja.checkout.validation;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.strategy.CouponStrategyFactory;
import java.math.BigDecimal;
import java.util.List;

public class CheckoutValidator {

    public static void validar(CheckoutRequest request) {
        validarPedido(request.getItens());
        validarNivelClube(request.getNivelClube());
        validarRegiao(request.getRegiao());
        validarModalidadeEntrega(request.getModalidadeEntrega());
        validarModalidadeDisponivel(request.getItens(), request.getModalidadeEntrega());

        BigDecimal subtotal = calcularSubtotal(request.getItens());

        validarCupom(request.getCupom(), subtotal, request.getItens());
        validarFormaPagamento(request.getFormaPagamento());
        validarParcelamento(request.getFormaPagamento(), request.getParcelas());
    }

    public static void validarFormaPagamentoDisponivel(String formaPagamento, BigDecimal total) {
        if (FormaPagamento.BOLETO.name().equals(formaPagamento)) {
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private static void validarPedido(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getNome() == null || item.getNome().isEmpty()) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private static void validarNivelClube(String nivelClube) {
        if (nivelClube == null || nivelClube.isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        try {
            NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private static void validarRegiao(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        try {
            Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private static void validarModalidadeEntrega(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private static void validarModalidadeDisponivel(List<Item> itens, String modalidade) {
        if (ModalidadeEntrega.MOTOBOY.name().equals(modalidade)) {
            BigDecimal pesoTotal = calcularPesoTotal(itens);
            if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private static void validarCupom(String cupom, BigDecimal subtotal, List<Item> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        var strategy = CouponStrategyFactory.create(cupom);
        if (strategy == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (!strategy.estaAplicavel(subtotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private static void validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private static void validarParcelamento(String formaPagamento, Integer parcelas) {
        int parcelasVal = (parcelas != null) ? parcelas : 1;

        FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);

        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelasVal != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelasVal < 1 || parcelasVal > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private static BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
