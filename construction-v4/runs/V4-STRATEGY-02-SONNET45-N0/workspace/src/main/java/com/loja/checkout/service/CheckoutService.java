package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.*;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido);
        BigDecimal descontoCupom;
        BigDecimal frete;
        Integer prazoEntregaDias;
        BigDecimal seguro;
        BigDecimal totalPedido;
        BigDecimal ajustePagamento;
        BigDecimal totalFinal;
        Integer parcelas = pedido.getParcelas();
        BigDecimal valorParcela;
        BigDecimal creditoProximaCompra;
        Boolean brinde;

        NivelClube nivelClube = NivelClube.valueOf(pedido.getNivelClube());
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(pedido.getModalidadeEntrega());
        Regiao regiao = Regiao.valueOf(pedido.getRegiao());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(pedido.getFormaPagamento());

        BigDecimal pesoTotal = calcularPesoTotal(pedido);
        frete = calcularFrete(modalidade, pesoTotal);
        prazoEntregaDias = modalidade.getPrazoDias();

        if (nivelClube.getFreteGratis()) {
            frete = arredondar(BigDecimal.ZERO);
        }

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            descontoCupom = calcularDescontoCupom(pedido, subtotalProdutos, frete);
        } else {
            descontoCupom = new BigDecimal("0.00");
        }

        seguro = calcularSeguro(subtotalProdutos, regiao);

        totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalPedido = arredondar(totalPedido);

        validarFormaPagamento(formaPagamento, totalPedido, parcelas);

        Map<String, BigDecimal> resultadoPagamento = calcularPagamento(formaPagamento, totalPedido, parcelas);
        totalFinal = resultadoPagamento.get("totalFinal");
        valorParcela = resultadoPagamento.get("valorParcela");
        ajustePagamento = totalFinal.subtract(totalPedido);

        creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);

        if (nivelClube.getGanhaBrinde() && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0) {
            brinde = true;
        } else {
            brinde = false;
        }

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(prazoEntregaDias);
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(ajustePagamento);
        resposta.setTotalFinal(totalFinal);
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(valorParcela);
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (pedido.getNivelClube() == null || pedido.getNivelClube().isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(pedido.getNivelClube());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (pedido.getRegiao() == null || pedido.getRegiao().isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(pedido.getRegiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (pedido.getModalidadeEntrega() == null || pedido.getModalidadeEntrega().isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(pedido.getModalidadeEntrega());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(pedido);
        if (modalidade.getPesoMaximoKg() != null && pesoTotal.compareTo(modalidade.getPesoMaximoKg()) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            try {
                Cupom.valueOf(pedido.getCupom());
            } catch (IllegalArgumentException e) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido);
            if (pedido.getCupom().equals("MENOS50") && subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        if (pedido.getFormaPagamento() == null || pedido.getFormaPagamento().isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(pedido.getFormaPagamento());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = pedido.getParcelas();
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, Integer parcelas) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(PedidoRequest pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(PedidoRequest pedido) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getValorBase().add(modalidade.getValorPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private BigDecimal calcularDescontoCupom(PedidoRequest pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        String codigoCupom = pedido.getCupom();
        BigDecimal desconto = BigDecimal.ZERO;

        switch (codigoCupom) {
            case "BEMVINDO10":
                desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
                break;
            case "MENOS50":
                desconto = new BigDecimal("50.00");
                break;
            case "FRETEGRATIS":
                desconto = frete;
                break;
            case "LEVE3PAGUE2":
                desconto = calcularDescontoLeve3Pague2(pedido);
                break;
        }

        return arredondar(desconto);
    }

    private BigDecimal calcularDescontoLeve3Pague2(PedidoRequest pedido) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return descontoTotal;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return arredondar(seguro);
    }

    private Map<String, BigDecimal> calcularPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, Integer parcelas) {
        Map<String, BigDecimal> resultado = new HashMap<>();
        BigDecimal totalFinal = totalPedido;
        BigDecimal valorParcela = totalPedido;

        switch (formaPagamento) {
            case PIX:
                BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
                desconto = arredondar(desconto);
                totalFinal = totalPedido.subtract(desconto);
                valorParcela = totalFinal;
                break;
            case BOLETO:
                BigDecimal tarifa = new BigDecimal("3.49");
                totalFinal = totalPedido.add(tarifa);
                valorParcela = totalFinal;
                break;
            case CARTAO:
                if (parcelas <= 3) {
                    valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                    totalFinal = totalPedido;
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                    BigDecimal potencia = umMaisTaxa.pow(parcelas);
                    BigDecimal fatorDivisao = BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN);
                    BigDecimal denominador = BigDecimal.ONE.subtract(fatorDivisao);
                    valorParcela = totalPedido.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                }
                break;
        }

        resultado.put("totalFinal", totalFinal);
        resultado.put("valorParcela", valorParcela);
        return resultado;
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(nivelClube.getPercentualCredito());
        return arredondar(credito);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
