package com.loja.roupas.domain;

import com.loja.roupas.domain.cupons.*;
import com.loja.roupas.domain.formas.*;
import com.loja.roupas.domain.util.Arredondador;
import com.loja.roupas.dto.ItemCarrinho;
import com.loja.roupas.dto.ResumoCheckoutRequest;
import com.loja.roupas.dto.ResumoCheckoutResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CheckoutService {

    public ResumoCheckoutResponse calcular(ResumoCheckoutRequest request) throws CheckoutException {
        validarPedido(request);

        List<ItemPedido> itens = converterItens(request.getItens());

        BigDecimal subtotalProdutos = calcularSubtotal(itens);

        Double pesoTotal = calcularPeso(itens);

        Entrega entrega = obterEntrega(request.getModalidadeEntrega());
        validarEntrega(entrega, pesoTotal);

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        frete = Arredondador.arredondar(frete);

        BigDecimal descontoCupom = calcularDesconto(request.getCupom(), subtotalProdutos, pesoTotal, frete, itens);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete);
        totalPedido = Arredondador.arredondar(totalPedido);

        FormaPagamento forma = obterFormaPagamento(request.getFormaPagamento());
        Integer parcelas = request.getParcelas();

        validarParcelamento(parcelas);
        validarFormaPagamento(forma, parcelas, totalPedido);

        BigDecimal ajustePagamento = forma.calcularAjuste(totalPedido, parcelas);
        ajustePagamento = Arredondador.arredondar(ajustePagamento);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = Arredondador.arredondar(totalFinal);

        BigDecimal valorParcela = forma.calcularValorParcela(totalFinal, parcelas);
        valorParcela = Arredondador.arredondar(valorParcela);

        return new ResumoCheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            entrega.getPrazo(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(ResumoCheckoutRequest request) throws CheckoutException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private List<ItemPedido> converterItens(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(i -> new ItemPedido(
                i.getNome(),
                new BigDecimal(i.getPrecoUnitario().toString()),
                i.getQuantidade(),
                i.getPesoKg()
            ))
            .collect(Collectors.toList());
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
            .map(ItemPedido::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private Double calcularPeso(List<ItemPedido> itens) {
        return itens.stream()
            .mapToDouble(ItemPedido::calcularPesoTotal)
            .sum();
    }

    private Entrega obterEntrega(String modalidade) throws CheckoutException {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            return Entrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarEntrega(Entrega entrega, Double pesoTotal) throws CheckoutException {
        if (!entrega.estaDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private BigDecimal calcularDesconto(String codigoCupom, BigDecimal subtotalProdutos, Double pesoTotal,
                                       BigDecimal frete, List<ItemPedido> itens) throws CheckoutException {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }

        Cupom cupom = obterCupom(codigoCupom);

        if (!cupom.podeAplicar(subtotalProdutos, pesoTotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        BigDecimal desconto = cupom.calcularDesconto(subtotalProdutos, pesoTotal, frete, itens);
        return Arredondador.arredondar(desconto);
    }

    private Cupom obterCupom(String codigo) throws CheckoutException {
        return switch (codigo) {
            case "BEMVINDO10" -> new CupomBemVindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }

    private FormaPagamento obterFormaPagamento(String forma) throws CheckoutException {
        if (forma == null || forma.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        return switch (forma) {
            case "PIX" -> new PagamentoPix();
            case "CARTAO" -> new PagamentoCartao();
            case "BOLETO" -> new PagamentoBoleto();
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        };
    }

    private void validarParcelamento(Integer parcelas) throws CheckoutException {
        if (parcelas == null || parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamento(FormaPagamento forma, Integer parcelas, BigDecimal total) throws CheckoutException {
        if (forma instanceof PagamentoPix || forma instanceof PagamentoBoleto) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma instanceof PagamentoCartao) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if (!forma.estaDisponivel(parcelas, total)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
