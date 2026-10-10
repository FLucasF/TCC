package com.loja.checkout.calculadora;

import com.loja.checkout.clube.BeneficioClube;
import com.loja.checkout.clube.FabricaBeneficioClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.FabricaCupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.entrega.FabricaEntrega;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.model.enums.*;
import com.loja.checkout.pagamento.FabricaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculadoraResumo {

    public CheckoutResponse calcular(CheckoutRequest request) {
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega());
        NivelClube nivelClube = NivelClube.valueOf(request.nivelClube());
        Regiao regiao = Regiao.valueOf(request.regiao());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        Entrega entrega = FabricaEntrega.criar(modalidade);
        BeneficioClube beneficio = FabricaBeneficioClube.criar(nivelClube);

        BigDecimal freteOriginal = entrega.calcularFrete(request.itens());
        BigDecimal frete = beneficio.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteOriginal;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null) {
            CodigoCupom codigoCupom = CodigoCupom.valueOf(request.cupom());
            Cupom cupom = FabricaCupom.criar(codigoCupom);
            descontoCupom = cupom.calcularDesconto(request.itens(), freteOriginal);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        Pagamento pagamento = FabricaPagamento.criar(formaPagamento);
        BigDecimal ajustePagamento = pagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = pagamento.calcularValorParcela(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = beneficio.calcularCredito(request.itens());
        boolean brinde = beneficio.ganharBrinde(subtotalProdutos);

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            entrega.getPrazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return subtotalProdutos.multiply(regiao.getTaxaSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
