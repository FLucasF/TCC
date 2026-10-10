package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final CupomService cupomService;

    public CheckoutService(CupomService cupomService) {
        this.cupomService = cupomService;
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarModalidadeEntrega(request.getModalidadeEntrega(), pesoTotal);

        cupomService.validarCupom(request.getCupom(), subtotalProdutos);

        validarFormaPagamento(request);

        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getNivelClube(), pesoTotal);
        BigDecimal descontoCupom = cupomService.calcularDesconto(
            request.getCupom(), subtotalProdutos, frete, request.getItens());
        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarLimiteFormaPagamento(request.getFormaPagamento(), totalPedido);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        CalculoPagamento calculoPagamento = calcularPagamento(
            request.getFormaPagamento(), totalPedido, parcelas);

        BigDecimal creditoProximaCompra = calcularCredito(
            request.getNivelClube(), subtotalProdutos);

        boolean brinde = calcularBrinde(request.getNivelClube(), subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(request.getModalidadeEntrega().getPrazoDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(calculoPagamento.ajuste());
        response.setTotalFinal(calculoPagamento.totalFinal());
        response.setParcelas(parcelas);
        response.setValorParcela(calculoPagamento.valorParcela());
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (request.getFormaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal totalItem = item.getPrecoUnitario()
                .multiply(new BigDecimal(item.getQuantidade()))
                .setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(totalItem);
        }

        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg()
                .multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }

    private void validarModalidadeEntrega(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, NivelClube nivel, BigDecimal pesoTotal) {
        if (nivel.isFreteGratis()) {
            return new BigDecimal("0.00");
        }

        BigDecimal frete = modalidade.getValorBase()
            .add(modalidade.getValorPorKg().multiply(pesoTotal))
            .setScale(2, RoundingMode.HALF_EVEN);

        return frete;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, com.loja.checkout.model.Regiao regiao) {
        return subtotalProdutos
            .multiply(regiao.getPercentualSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private void validarFormaPagamento(CheckoutRequest request) {
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        if (!request.getFormaPagamento().aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarLimiteFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceitaValor(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private CalculoPagamento calcularPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = totalPedido
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new CalculoPagamento(totalFinal, totalFinal, desconto.negate());
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal tarifa = formaPagamento.getTarifa();
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new CalculoPagamento(totalFinal, totalFinal, tarifa);
        }

        if (parcelas <= 3) {
            BigDecimal valorParcela = totalPedido
                .divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new CalculoPagamento(totalPedido, valorParcela, new BigDecimal("0.00"));
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN));

        BigDecimal valorParcela = totalPedido
            .multiply(taxa)
            .divide(denominador, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new CalculoPagamento(totalFinal, valorParcela, ajuste);
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotalProdutos) {
        return subtotalProdutos
            .multiply(nivel.getPercentualCredito())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean calcularBrinde(NivelClube nivel, BigDecimal subtotalProdutos) {
        return nivel.isRecebeBrinde() && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }

    private record CalculoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {}
}
