package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal LIMITE_BRINDE_OURO = new BigDecimal("500.00");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        NivelClube nivelClube = parseNivelClube(request.getNivelClube());
        Regiao regiao = parseRegiao(request.getRegiao());
        ModalidadeEntrega modalidade = parseModalidadeEntrega(request.getModalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        validarCupom(request.getCupom(), subtotalProdutos);

        FormaPagamento formaPagamento = parseFormaPagamento(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), request.getItens(),
            subtotalProdutos, modalidade, pesoTotal, nivelClube);

        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);
        BigDecimal seguro = arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = calcularPagamento(formaPagamento, parcelas, totalPedido);

        BigDecimal creditoProximaCompra = arredondar(subtotalProdutos.multiply(nivelClube.getPercentualCredito()));
        boolean brinde = nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(LIMITE_BRINDE_OURO) > 0;

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazoEntregaDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(resultadoPagamento.ajuste);
        response.setTotalFinal(resultadoPagamento.totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(resultadoPagamento.valorParcela);
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
    }

    private NivelClube parseNivelClube(String nivel) {
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega parseModalidadeEntrega(String modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupom, BigDecimal subtotalProdutos) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        if (!cupomValido(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (cupom.equals("MENOS50") && subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private boolean cupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private FormaPagamento parseFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, List<ItemCarrinho> itens,
            BigDecimal subtotalProdutos, ModalidadeEntrega modalidade, BigDecimal pesoTotal,
            NivelClube nivelClube) {
        if (cupom == null || cupom.isEmpty()) {
            return arredondar(BigDecimal.ZERO);
        }

        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> calcularFrete(modalidade, pesoTotal, NivelClube.BRONZE);
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> arredondar(BigDecimal.ZERO);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis)));
            }
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube.isFreteGratis()) {
            return arredondar(BigDecimal.ZERO);
        }

        BigDecimal frete = modalidade.getValorBase()
            .add(modalidade.getValorPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private ResultadoPagamento calcularPagamento(FormaPagamento formaPagamento, int parcelas, BigDecimal totalPedido) {
        return switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
                BigDecimal totalFinal = totalPedido.subtract(desconto);
                yield new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
            }
            case BOLETO -> {
                BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
                yield new ResultadoPagamento(TARIFA_BOLETO, totalFinal, totalFinal);
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS) {
                    BigDecimal valorParcela = arredondar(totalPedido.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
                    yield new ResultadoPagamento(arredondar(BigDecimal.ZERO), totalPedido, valorParcela);
                } else {
                    BigDecimal taxa = TAXA_JUROS_CARTAO;
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                    BigDecimal divisor = BigDecimal.ONE.subtract(
                        BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 10, RoundingMode.HALF_EVEN)
                    );
                    BigDecimal valorParcela = arredondar(totalPedido.multiply(taxa).divide(divisor, 10, RoundingMode.HALF_EVEN));
                    BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new ResultadoPagamento(ajuste, totalFinal, valorParcela);
                }
            }
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
