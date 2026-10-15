package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import com.loja.exception.CheckoutException;
import com.loja.model.DadosPedido;
import com.loja.model.Regiao;
import com.loja.strategy.clube.*;
import com.loja.strategy.cupom.*;
import com.loja.strategy.modalidade.*;
import com.loja.strategy.pagamento.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CheckoutService {

    private static final Map<String, Modalidade> MODALIDADES = Map.of(
        "ECONOMICA", new ModalidadeEconomica(),
        "EXPRESSA", new ModalidadeExpressa(),
        "RETIRADA_LOJA", new ModalidadeRetiradaLoja(),
        "MOTOBOY", new ModalidadeMotoboy()
    );

    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new CupomBemVindo10(),
        "MENOS50", new CupomMenos50(),
        "FRETEGRATIS", new CupomFreteGratis(),
        "LEVE3PAGUE2", new CupomLeve3Pague2()
    );

    private static final Map<String, NivelClube> NIVEIS_CLUBE = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    private static final Map<String, FormaPagamento> FORMAS_PAGAMENTO = Map.of(
        "PIX", new PagamentoPix(),
        "BOLETO", new PagamentoBoleto(),
        "CARTAO", new PagamentoCartao()
    );

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        DadosPedido dados = new DadosPedido();

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request);
        dados.setSubtotalProdutos(subtotalProdutos);

        NivelClube nivelClube = obterNivelClube(request.nivelClube());
        Regiao regiao = obterRegiao(request.regiao());

        BigDecimal pesoTotal = calcularPesoTotal(request);
        Modalidade modalidade = obterModalidade(request.modalidadeEntrega(), pesoTotal);

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivelClube.isFretGratis()) {
            frete = new BigDecimal("0.00");
        }
        dados.setFrete(frete);
        dados.setPrazoEntregaDias(modalidade.getPrazoEntregaDias());

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            Cupom cupom = obterCupom(request.cupom(), subtotalProdutos, request);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, request);
        }
        dados.setDescontoCupom(descontoCupom);

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);
        dados.setSeguro(seguro);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        dados.setTotalPedido(totalPedido);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = obterFormaPagamento(request.formaPagamento(), totalPedido, parcelas);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        dados.setAjustePagamento(ajustePagamento);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        dados.setTotalFinal(totalFinal);

        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, parcelas);
        dados.setParcelas(parcelas);
        dados.setValorParcela(valorParcela);

        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        dados.setCreditoProximaCompra(credito);

        boolean brinde = nivelClube.concedeBrinde(subtotalProdutos);
        dados.setBrinde(brinde);

        return new CheckoutResponse(
            dados.getSubtotalProdutos(),
            dados.getDescontoCupom(),
            dados.getFrete(),
            dados.getPrazoEntregaDias(),
            dados.getSeguro(),
            dados.getAjustePagamento(),
            dados.getTotalFinal(),
            dados.getParcelas(),
            dados.getValorParcela(),
            dados.getCreditoProximaCompra(),
            dados.isBrinde()
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(CheckoutRequest request) {
        return request.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        return request.itens().stream()
                .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NivelClube obterNivelClube(String nivel) {
        if (nivel == null || !NIVEIS_CLUBE.containsKey(nivel)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return NIVEIS_CLUBE.get(nivel);
    }

    private Regiao obterRegiao(String nomeRegiao) {
        if (nomeRegiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(nomeRegiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private Modalidade obterModalidade(String nomeModalidade, BigDecimal pesoTotal) {
        if (nomeModalidade == null || !MODALIDADES.containsKey(nomeModalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        Modalidade modalidade = MODALIDADES.get(nomeModalidade);
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        return modalidade;
    }

    private Cupom obterCupom(String codigoCupom, BigDecimal subtotalProdutos, CheckoutRequest request) {
        if (!CUPONS.containsKey(codigoCupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        Cupom cupom = CUPONS.get(codigoCupom);
        if (!cupom.isAplicavel(subtotalProdutos, request)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        return cupom;
    }

    private FormaPagamento obterFormaPagamento(String nomeFormaPagamento, BigDecimal totalPedido, int parcelas) {
        if (nomeFormaPagamento == null || !FORMAS_PAGAMENTO.containsKey(nomeFormaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FORMAS_PAGAMENTO.get(nomeFormaPagamento);

        if (!formaPagamento.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return formaPagamento;
    }
}
