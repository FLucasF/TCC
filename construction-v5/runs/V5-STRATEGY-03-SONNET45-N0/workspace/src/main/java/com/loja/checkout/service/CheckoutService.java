package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.*;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        NivelClube nivelClube = validarNivelClube(request.getNivelClube());
        Regiao regiao = validarRegiao(request.getRegiao());
        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.getModalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarModalidadeDisponivel(modalidade, pesoTotal);

        Cupom cupom = validarCupom(request.getCupom());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        validarCupomAplicavel(cupom, subtotalProdutos, request.getItens());

        FormaPagamento formaPagamento = validarFormaPagamento(request.getFormaPagamento());

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);

        BigDecimal freteInicial = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.isFreteGratis()
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
            : freteInicial;

        BigDecimal descontoCupom = cupom != null
            ? cupom.calcularDesconto(subtotalProdutos, freteInicial, request.getItens())
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalPedido = totalPedido.setScale(2, RoundingMode.HALF_EVEN);

        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = totalFinal.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalFinal, parcelas);

        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);

        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazoDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(credito);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (!item.isValido()) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivelClubeStr) {
        if (nivelClubeStr == null || nivelClubeStr.isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        try {
            return NivelClube.valueOf(nivelClubeStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiaoStr) {
        if (regiaoStr == null || regiaoStr.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        try {
            return Regiao.valueOf(regiaoStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidadeStr) {
        if (modalidadeStr == null || modalidadeStr.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            return ModalidadeEntrega.valueOf(modalidadeStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String cupomStr) {
        if (cupomStr == null || cupomStr.isEmpty()) {
            return null;
        }

        try {
            return Cupom.valueOf(cupomStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        if (cupom != null && !cupom.isAplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamentoStr) {
        if (formaPagamentoStr == null || formaPagamentoStr.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            return FormaPagamento.valueOf(formaPagamentoStr);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceitaValor(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.calcularSubtotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            pesoTotal = pesoTotal.add(item.calcularPesoTotal());
        }
        return pesoTotal;
    }
}
