package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS_CARTAO = 12;
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request);
        BigDecimal pesoTotal = calcularPesoTotal(request);

        validarModalidadeDisponivel(request.getModalidadeEntrega(), pesoTotal);

        BigDecimal descontoCupom = calcularDescontoCupom(request, subtotalProdutos, pesoTotal);

        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), pesoTotal, request.getNivelClube());
        int prazoEntregaDias = request.getModalidadeEntrega().getPrazoEntregaDias();

        BigDecimal seguro = calcularSeguro(request.getRegiao(), subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int numeroParcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(request.getFormaPagamento(), numeroParcelas);
        validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalPedido);

        Map<String, BigDecimal> resultadoPagamento = calcularPagamento(
                request.getFormaPagamento(),
                totalPedido,
                numeroParcelas
        );

        BigDecimal totalFinal = resultadoPagamento.get("totalFinal");
        BigDecimal valorParcela = resultadoPagamento.get("valorParcela");
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        BigDecimal creditoProximaCompra = calcularCredito(request.getNivelClube(), subtotalProdutos);
        boolean brinde = request.getNivelClube().temBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntregaDias);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(numeroParcelas);
        response.setValorParcela(valorParcela);
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

    private BigDecimal calcularSubtotalProdutos(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrinho item : request.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario()
                    .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }

        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : request.getItens()) {
            BigDecimal pesoItem = item.getPesoKg()
                    .multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.isDisponivelParaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private BigDecimal calcularDescontoCupom(CheckoutRequest request, BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
        String cupom = request.getCupom();

        if (cupom == null || cupom.isEmpty()) {
            return new BigDecimal("0.00");
        }

        return switch (cupom) {
            case "BEMVINDO10" -> {
                BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
                yield arredondar(desconto);
            }
            case "MENOS50" -> {
                if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
                yield new BigDecimal("50.00");
            }
            case "FRETEGRATIS" -> {
                BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), pesoTotal, NivelClube.BRONZE);
                yield frete;
            }
            case "LEVE3PAGUE2" -> {
                BigDecimal desconto = BigDecimal.ZERO;
                for (ItemCarrinho item : request.getItens()) {
                    int quantidade = item.getQuantidade();
                    int itensGratis = quantidade / 3;
                    if (itensGratis > 0) {
                        BigDecimal descontoItem = item.getPrecoUnitario()
                                .multiply(new BigDecimal(itensGratis));
                        desconto = desconto.add(descontoItem);
                    }
                }
                yield arredondar(desconto);
            }
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube.isFreteGratis()) {
            return new BigDecimal("0.00");
        }

        BigDecimal valorBase = modalidade.getValorBase();
        BigDecimal valorPorKg = modalidade.getValorPorKg();
        BigDecimal frete = valorBase.add(valorPorKg.multiply(pesoTotal));

        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(com.loja.checkout.enums.Regiao regiao, BigDecimal subtotalProdutos) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return arredondar(seguro);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int numeroParcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (numeroParcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (numeroParcelas < 1 || numeroParcelas > MAX_PARCELAS_CARTAO) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private Map<String, BigDecimal> calcularPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int numeroParcelas) {
        Map<String, BigDecimal> resultado = new HashMap<>();

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
            totalFinal = totalPedido.subtract(desconto);
            valorParcela = totalFinal;
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            totalFinal = totalPedido.add(TARIFA_BOLETO);
            valorParcela = totalFinal;
        } else {
            if (numeroParcelas <= PARCELAS_SEM_JUROS) {
                totalFinal = totalPedido;
                valorParcela = arredondar(totalFinal.divide(new BigDecimal(numeroParcelas), 10, RoundingMode.HALF_EVEN));
            } else {
                valorParcela = calcularParcelaComJuros(totalPedido, numeroParcelas);
                totalFinal = valorParcela.multiply(new BigDecimal(numeroParcelas));
            }
        }

        resultado.put("totalFinal", totalFinal);
        resultado.put("valorParcela", valorParcela);

        return resultado;
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal principal, int numeroParcelas) {
        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaPotencia = umMaisTaxa.pow(numeroParcelas);

        BigDecimal numerador = principal.multiply(taxa).multiply(umMaisTaxaPotencia);
        BigDecimal denominador = umMaisTaxaPotencia.subtract(BigDecimal.ONE);

        BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);

        return arredondar(parcela);
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(nivelClube.getPercentualCredito());
        return arredondar(credito);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
