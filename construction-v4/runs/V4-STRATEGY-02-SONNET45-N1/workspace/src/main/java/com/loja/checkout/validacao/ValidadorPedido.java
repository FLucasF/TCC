package com.loja.checkout.validacao;

import com.loja.checkout.clube.BeneficioClube;
import com.loja.checkout.clube.FabricaBeneficioClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.FabricaCupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.entrega.FabricaEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.model.enums.*;
import com.loja.checkout.pagamento.FabricaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ValidadorPedido {

    public void validar(CheckoutRequest request) {
        validarPedido(request.itens());
        validarNivelClube(request.nivelClube());
        validarRegiao(request.regiao());
        validarModalidadeEntrega(request.modalidadeEntrega(), request.itens());
        validarCupom(request.cupom(), request.itens());
        validarFormaPagamento(request.formaPagamento(), request.parcelas(), request.itens());
    }

    private void validarPedido(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private void validarNivelClube(String nivelClubeStr) {
        if (nivelClubeStr == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        try {
            NivelClube.valueOf(nivelClubeStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private void validarRegiao(String regiaoStr) {
        if (regiaoStr == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        try {
            Regiao.valueOf(regiaoStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private void validarModalidadeEntrega(String modalidadeStr, List<ItemCarrinho> itens) {
        if (modalidadeStr == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(modalidadeStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        Entrega entrega = FabricaEntrega.criar(modalidade);
        if (!entrega.estaDisponivel(itens)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    private void validarCupom(String cupomStr, List<ItemCarrinho> itens) {
        if (cupomStr == null) {
            return;
        }

        CodigoCupom codigoCupom;
        try {
            codigoCupom = CodigoCupom.valueOf(cupomStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }

        Cupom cupom = FabricaCupom.criar(codigoCupom);
        if (!cupom.ehAplicavel(itens)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }

    private void validarFormaPagamento(String formaPagamentoStr, Integer parcelas, List<ItemCarrinho> itens) {
        if (formaPagamentoStr == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(formaPagamentoStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        int numParcelas = parcelas != null ? parcelas : 1;
        Pagamento pagamento = FabricaPagamento.criar(formaPagamento);

        if (!pagamento.aceitaParcelas(numParcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = calcularSubtotalProdutos(itens);
        if (!pagamento.estaDisponivel(subtotal)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
