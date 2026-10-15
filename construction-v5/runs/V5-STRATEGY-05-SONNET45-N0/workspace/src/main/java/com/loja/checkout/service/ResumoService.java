package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.CodigoErro;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.NivelClube;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal LIMITE_BRINDE_OURO = new BigDecimal("500.00");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal MINIMO_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());

        BigDecimal freteBruto = calcularFreteBruto(pedido, pesoTotal);
        ResultadoFreteDesconto freteDesconto = calcularFreteEDesconto(pedido, subtotalProdutos, freteBruto);

        BigDecimal frete = freteDesconto.frete;
        BigDecimal descontoCupom = freteDesconto.desconto;
        BigDecimal seguro = calcularSeguro(pedido.getRegiao(), subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
        ResultadoPagamento resultadoPagamento = calcularPagamento(
                pedido.getFormaPagamento(),
                totalPedido,
                parcelas
        );

        BigDecimal creditoProximaCompra = calcularCreditoClube(
                pedido.getNivelClube(),
                subtotalProdutos
        );

        boolean brinde = pedido.getNivelClube() == NivelClube.OURO
                && subtotalProdutos.compareTo(LIMITE_BRINDE_OURO) > 0;

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(pedido.getModalidadeEntrega().getPrazoDias());
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(resultadoPagamento.ajuste);
        resposta.setTotalFinal(resultadoPagamento.totalFinal);
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(resultadoPagamento.valorParcela);
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ValidacaoException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemCarrinho item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacaoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }

        if (pedido.getNivelClube() == null) {
            throw new ValidacaoException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        if (pedido.getRegiao() == null) {
            throw new ValidacaoException(CodigoErro.REGIAO_INVALIDA);
        }

        if (pedido.getModalidadeEntrega() == null) {
            throw new ValidacaoException(CodigoErro.MODALIDADE_INVALIDA);
        }

        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());
        if (!pedido.getModalidadeEntrega().aceitaPeso(pesoTotal)) {
            throw new ValidacaoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            try {
                Cupom.valueOf(pedido.getCupom());
            } catch (IllegalArgumentException e) {
                throw new ValidacaoException(CodigoErro.CUPOM_INVALIDO);
            }

            validarCupomAplicavel(pedido);
        }

        if (pedido.getFormaPagamento() == null) {
            throw new ValidacaoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        int parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
        if (!pedido.getFormaPagamento().aceitaParcelas(parcelas)) {
            throw new ValidacaoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        validarFormaPagamentoDisponivel(pedido);
    }

    private void validarCupomAplicavel(PedidoRequest pedido) {
        Cupom cupom = Cupom.valueOf(pedido.getCupom());

        if (cupom == Cupom.MENOS50) {
            BigDecimal subtotal = calcularSubtotalProdutos(pedido.getItens());
            if (subtotal.compareTo(MINIMO_MENOS50) < 0) {
                throw new ValidacaoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }
    }

    private void validarFormaPagamentoDisponivel(PedidoRequest pedido) {
        if (pedido.getFormaPagamento() == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotalProdutos(pedido.getItens());
            if (subtotal.compareTo(LIMITE_BOLETO) > 0) {
                throw new ValidacaoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario()
                    .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
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

    private ResultadoFreteDesconto calcularFreteEDesconto(PedidoRequest pedido, BigDecimal subtotal, BigDecimal freteBruto) {
        BigDecimal frete;
        BigDecimal desconto;

        if (pedido.getNivelClube() == NivelClube.OURO) {
            frete = arredondar(BigDecimal.ZERO);
            desconto = calcularDescontoCupomSemFrete(pedido, subtotal);
        } else if (pedido.getCupom() != null && Cupom.FRETEGRATIS.name().equals(pedido.getCupom())) {
            frete = freteBruto;
            desconto = freteBruto;
        } else {
            frete = freteBruto;
            desconto = calcularDescontoCupomSemFrete(pedido, subtotal);
        }

        return new ResultadoFreteDesconto(frete, desconto);
    }

    private BigDecimal calcularDescontoCupomSemFrete(PedidoRequest pedido, BigDecimal subtotal) {
        if (pedido.getCupom() == null || pedido.getCupom().isEmpty()) {
            return arredondar(BigDecimal.ZERO);
        }

        Cupom cupom = Cupom.valueOf(pedido.getCupom());

        return switch (cupom) {
            case BEMVINDO10 -> arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case MENOS50 -> new BigDecimal("50.00");
            case FRETEGRATIS -> arredondar(BigDecimal.ZERO);
            case LEVE3PAGUE2 -> calcularDescontoLeve3Pague2(pedido.getItens());
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(new BigDecimal(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
        }

        return arredondar(descontoTotal);
    }

    private BigDecimal calcularFreteBruto(PedidoRequest pedido, BigDecimal pesoTotal) {
        BigDecimal valorFrete = pedido.getModalidadeEntrega().getValorBase()
                .add(pedido.getModalidadeEntrega().getValorPorKg().multiply(pesoTotal));

        return arredondar(valorFrete);
    }

    private BigDecimal calcularSeguro(com.loja.checkout.enums.Regiao regiao, BigDecimal subtotal) {
        return arredondar(subtotal.multiply(regiao.getPercentualSeguro()));
    }

    private ResultadoPagamento calcularPagamento(FormaPagamento forma, BigDecimal totalPedido, int parcelas) {
        return switch (forma) {
            case PIX -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
                BigDecimal totalFinal = totalPedido.subtract(desconto);
                yield new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
            }
            case BOLETO -> {
                BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
                yield new ResultadoPagamento(TARIFA_BOLETO, totalFinal, totalFinal);
            }
            case CARTAO -> calcularPagamentoCartao(totalPedido, parcelas);
        };
    }

    private ResultadoPagamento calcularPagamentoCartao(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = arredondar(totalPedido.divide(
                    new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(arredondar(BigDecimal.ZERO), totalPedido, valorParcela);
        }

        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_CARTAO);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_CARTAO);
        BigDecimal umMaisTaxaPotencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(umMaisTaxaPotencia, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private BigDecimal calcularCreditoClube(NivelClube nivel, BigDecimal subtotal) {
        return arredondar(subtotal.multiply(nivel.getPercentualCredito()));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}

    private record ResultadoFreteDesconto(BigDecimal frete, BigDecimal desconto) {}
}
