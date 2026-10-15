package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.strategy.CouponStrategyFactory;
import com.loja.checkout.strategy.CouponStrategy;
import com.loja.checkout.strategy.PaymentAdjustmentStrategy;
import com.loja.checkout.strategy.PaymentAdjustmentStrategyFactory;
import com.loja.checkout.strategy.ShippingStrategy;
import com.loja.checkout.strategy.ShippingStrategyFactory;
import com.loja.checkout.util.MoneyRounder;
import com.loja.checkout.validation.CheckoutValidator;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        try {
            CheckoutValidator.validar(request);

            List<Item> itens = request.getItens();
            ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
            NivelClube nivel = NivelClube.valueOf(request.getNivelClube());
            Regiao regiao = Regiao.valueOf(request.getRegiao());
            FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());
            Integer parcelas = (request.getParcelas() != null) ? request.getParcelas() : 1;

            BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

            BigDecimal freteCalculado = calcularFrete(modalidade, itens, nivel);

            BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, itens, freteCalculado);

            BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

            BigDecimal totalAntesPagamento = subtotalProdutos
                    .subtract(descontoCupom)
                    .add(freteCalculado)
                    .add(seguro);
            totalAntesPagamento = MoneyRounder.round(totalAntesPagamento);

            CheckoutValidator.validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalAntesPagamento);

            PaymentAdjustmentStrategy paymentStrategy = PaymentAdjustmentStrategyFactory.create(forma);
            paymentStrategy.validarParcelas(parcelas);

            BigDecimal ajustePagamento = paymentStrategy.calcularAjuste(totalAntesPagamento, parcelas);

            BigDecimal totalFinal = totalAntesPagamento.add(ajustePagamento);
            totalFinal = MoneyRounder.round(totalFinal);

            BigDecimal valorParcela = paymentStrategy.calcularValorParcela(totalAntesPagamento, parcelas);

            BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, nivel);

            boolean temBrinde = verificarBrinde(subtotalProdutos, nivel);

            Integer prazoEntrega = obterPrazoEntrega(modalidade);

            CheckoutResponse response = new CheckoutResponse();
            response.setSubtotalProdutos(subtotalProdutos);
            response.setDescontoCupom(descontoCupom);
            response.setFrete(freteCalculado);
            response.setPrazoEntregaDias(prazoEntrega);
            response.setSeguro(seguro);
            response.setAjustePagamento(ajustePagamento);
            response.setTotalFinal(totalFinal);
            response.setParcelas(parcelas);
            response.setValorParcela(valorParcela);
            response.setCreditoProximaCompra(creditoProximaCompra);
            response.setBrinde(temBrinde);

            return response;
        } catch (CheckoutException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar checkout", e);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<Item> itens, NivelClube nivel) {
        if (nivel.temFreteGratis()) {
            return MoneyRounder.round(BigDecimal.ZERO);
        }

        ShippingStrategy strategy = ShippingStrategyFactory.create(modalidade);
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return strategy.calcularFrete(pesoTotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteCalculado) {
        if (cupom == null || cupom.isEmpty()) {
            return MoneyRounder.round(BigDecimal.ZERO);
        }

        CouponStrategy strategy = CouponStrategyFactory.create(cupom);
        if (strategy == null) {
            return MoneyRounder.round(BigDecimal.ZERO);
        }

        return strategy.calcularDesconto(subtotalProdutos, itens, freteCalculado);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return MoneyRounder.round(seguro);
    }

    private BigDecimal calcularCredito(BigDecimal subtotalProdutos, NivelClube nivel) {
        BigDecimal credito = subtotalProdutos.multiply(nivel.getPercentualCredito());
        return MoneyRounder.round(credito);
    }

    private boolean verificarBrinde(BigDecimal subtotalProdutos, NivelClube nivel) {
        if (!nivel.temBrinde()) {
            return false;
        }
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }

    private Integer obterPrazoEntrega(ModalidadeEntrega modalidade) {
        ShippingStrategy strategy = ShippingStrategyFactory.create(modalidade);
        return strategy.getPrazoEntregaDias();
    }
}
