package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculadorResumo {

    public ResumoResponse calcular(PedidoRequest request) {
        List<ItemPedido> itens = converterItens(request.itens());
        BigDecimal subtotalProdutos = calcularSubtotal(itens);

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega());
        NivelClube nivelClube = NivelClube.valueOf(request.nivelClube());
        Regiao regiao = Regiao.valueOf(request.regiao());

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal freteBase = Arredondador.arredondarParaCentavos(modalidade.calcularFrete(pesoTotal));
        BigDecimal frete = nivelClube.isGratisFrete() ? BigDecimal.ZERO : freteBase;

        Cupom cupom = request.cupom() != null ? Cupom.porCodigo(request.cupom()) : null;
        BigDecimal descontoCupom = Arredondador.arredondarParaCentavos(BigDecimal.ZERO);

        if (cupom != null) {
            if (cupom instanceof Cupom.CupomFreteGratis) {
                descontoCupom = frete;
                frete = BigDecimal.ZERO;
            } else {
                descontoCupom = Arredondador.arredondarParaCentavos(cupom.calcularDesconto(subtotalProdutos, itens));
            }
        }

        BigDecimal seguro = Arredondador.arredondarParaCentavos(regiao.calcularSeguro(subtotalProdutos));
        BigDecimal creditoProximaCompra = Arredondador.arredondarParaCentavos(nivelClube.calcularCredito(subtotalProdutos));

        BigDecimal totalAntesPagamento = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalAntesPagamento = Arredondador.arredondarParaCentavos(totalAntesPagamento);

        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();

        AjusteParcelas ajuste = calcularAjusteParcelamento(formaPagamento, totalAntesPagamento, parcelas);
        boolean temBrinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
            Arredondador.arredondarParaCentavos(subtotalProdutos),
            Arredondador.arredondarParaCentavos(descontoCupom),
            Arredondador.arredondarParaCentavos(frete),
            modalidade.getPrazo(),
            Arredondador.arredondarParaCentavos(seguro),
            Arredondador.arredondarParaCentavos(ajuste.ajuste),
            Arredondador.arredondarParaCentavos(ajuste.total),
            parcelas,
            Arredondador.arredondarParaCentavos(ajuste.valorParcela),
            Arredondador.arredondarParaCentavos(creditoProximaCompra),
            temBrinde
        );
    }

    private AjusteParcelas calcularAjusteParcelamento(FormaPagamento formaPagamento, BigDecimal total, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = Arredondador.arredondarParaCentavos(total.multiply(BigDecimal.valueOf(0.05)));
            BigDecimal totalComAjuste = Arredondador.arredondarParaCentavos(total.subtract(desconto));
            return new AjusteParcelas(desconto.negate(), totalComAjuste, totalComAjuste);
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal ajuste = BigDecimal.valueOf(3.49);
            BigDecimal totalComAjuste = Arredondador.arredondarParaCentavos(total.add(ajuste));
            return new AjusteParcelas(ajuste, totalComAjuste, totalComAjuste);
        }

        if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Arredondador.arredondarParaCentavos(total.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
                return new AjusteParcelas(BigDecimal.ZERO, total, valorParcela);
            }
            BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
            BigDecimal fator = BigDecimal.ONE.add(taxaMensal).pow(parcelas);
            BigDecimal divisor = fator.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = Arredondador.arredondarParaCentavos(
                total.multiply(taxaMensal).multiply(fator).divide(divisor, 10, java.math.RoundingMode.HALF_EVEN)
            );
            BigDecimal totalComAjuste = Arredondador.arredondarParaCentavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Arredondador.arredondarParaCentavos(totalComAjuste.subtract(total));
            return new AjusteParcelas(ajuste, totalComAjuste, valorParcela);
        }

        return new AjusteParcelas(BigDecimal.ZERO, total, total);
    }

    private List<ItemPedido> converterItens(List<PedidoRequest.Item> itens) {
        return itens.stream()
            .map(item -> new ItemPedido(
                item.nome(),
                new BigDecimal(item.precoUnitario().toString()),
                item.quantidade(),
                new BigDecimal(item.pesoKg().toString())
            ))
            .toList();
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            BigDecimal linhaTotal = item.precoUnitario().multiply(quantidade);
            subtotal = subtotal.add(linhaTotal);
        }
        return Arredondador.arredondarParaCentavos(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            BigDecimal pesoLinha = item.pesoKg().multiply(quantidade);
            pesoTotal = pesoTotal.add(pesoLinha);
        }
        return pesoTotal;
    }

    record AjusteParcelas(BigDecimal ajuste, BigDecimal total, BigDecimal valorParcela) {
    }
}
