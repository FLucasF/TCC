package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.strategy.clube.ClubeMemberFactory;
import com.loja.checkout.strategy.clube.ClubeMemberStrategy;
import com.loja.checkout.strategy.cupom.CupomFactory;
import com.loja.checkout.strategy.cupom.CupomStrategy;
import com.loja.checkout.strategy.entrega.ModalidadeEntregaFactory;
import com.loja.checkout.strategy.entrega.ModalidadeEntregaStrategy;
import com.loja.checkout.strategy.pagamento.FormaPagamentoFactory;
import com.loja.checkout.strategy.pagamento.FormaPagamentoStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        try {
            validarRequest(request);

            BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());

            CupomStrategy cupomStrategy = CupomFactory.criar(request.getCupom());
            BigDecimal descontoCupom = BigDecimal.ZERO;
            boolean isFreteGratis = false;

            if (cupomStrategy != null) {
                cupomStrategy.validar(subtotalProdutos);
                if ("FRETEGRATIS".equals(request.getCupom())) {
                    isFreteGratis = true;
                } else {
                    descontoCupom = cupomStrategy.calcularDesconto(subtotalProdutos, request);
                }
            }

            double pesoTotal = calcularPesoTotal(request.getItens());
            ModalidadeEntregaStrategy entregaStrategy = ModalidadeEntregaFactory.criar(request.getModalidadeEntrega());
            entregaStrategy.validar(pesoTotal);

            ClubeMemberStrategy clubeStrategy = ClubeMemberFactory.criar(request.getNivelClube());
            boolean temFreteGratisClube = clubeStrategy.temFreteGratis();

            BigDecimal frete = BigDecimal.ZERO;
            int prazoEntrega = entregaStrategy.getPrazoEntregaDias();

            if (!isFreteGratis && !temFreteGratisClube) {
                frete = entregaStrategy.calcularFrete(pesoTotal);
            } else if (isFreteGratis) {
                descontoCupom = entregaStrategy.calcularFrete(pesoTotal);
            }

            BigDecimal totalAntesDoSeguro = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete);

            BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

            BigDecimal totalAntesAjuste = totalAntesDoSeguro.add(seguro);

            Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
            FormaPagamentoStrategy pagamentoStrategy = FormaPagamentoFactory.criar(request.getFormaPagamento());
            pagamentoStrategy.validar(totalAntesAjuste, parcelas);

            BigDecimal ajustePagamento = pagamentoStrategy.calcularAjuste(totalAntesAjuste, parcelas);
            BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);

            BigDecimal valorParcela = MoneyRounder.round(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));

            BigDecimal credito = clubeStrategy.calcularCredito(subtotalProdutos);
            boolean brinde = clubeStrategy.validarBrinde(subtotalProdutos);

            CheckoutResponse response = new CheckoutResponse();
            response.setSubtotalProdutos(subtotalProdutos);
            response.setDescontoCupom(descontoCupom);
            response.setFrete(frete);
            response.setPrazoEntregaDias(prazoEntrega);
            response.setSeguro(seguro);
            response.setAjustePagamento(ajustePagamento);
            response.setTotalFinal(totalFinal);
            response.setParcelas(parcelas);
            response.setValorParcela(valorParcela);
            response.setCreditoProximaCompra(credito);
            response.setBrinde(brinde);

            return response;
        } catch (CheckoutException e) {
            throw e;
        }
    }

    private void validarRequest(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
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

    private BigDecimal calcularSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal valorItem = BigDecimal.valueOf(item.getPrecoUnitario() * item.getQuantidade());
            subtotal = subtotal.add(valorItem);
        }
        return MoneyRounder.round(subtotal);
    }

    private double calcularPesoTotal(List<Item> itens) {
        double peso = 0;
        for (Item item : itens) {
            peso += item.getPesoKg() * item.getQuantidade();
        }
        return peso;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, com.loja.checkout.model.Region regiao) {
        double percentual = regiao.getInsurancePercentage();
        return MoneyRounder.round(subtotalProdutos.multiply(BigDecimal.valueOf(percentual)));
    }
}
