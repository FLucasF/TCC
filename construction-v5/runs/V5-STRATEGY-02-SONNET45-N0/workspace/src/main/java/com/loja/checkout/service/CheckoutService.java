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
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal MINIMO_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal MINIMO_BRINDE_OURO = new BigDecimal("500.00");
    private static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");
    private static final BigDecimal MAXIMO_BOLETO = new BigDecimal("1000.00");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarModalidadeEntrega(request.getModalidadeEntrega(), pesoTotal);

        validarCupom(request.getCupom(), subtotalProdutos);

        validarFormaPagamento(request.getFormaPagamento(), request.getParcelas());

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal descontoCupom = new BigDecimal("0.00");
        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), pesoTotal);
        Integer prazo = calcularPrazo(request.getModalidadeEntrega());

        boolean freteGratisOuro = request.getNivelClube() == NivelClube.OURO;
        if (freteGratisOuro) {
            frete = new BigDecimal("0.00");
        }

        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            descontoCupom = calcularDescontoCupom(
                request.getCupom(),
                request.getItens(),
                subtotalProdutos,
                frete,
                freteGratisOuro
            );
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalPedido = arredondar(totalPedido);

        validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalPedido);

        Map<String, BigDecimal> resultadoPagamento = calcularPagamento(
            request.getFormaPagamento(),
            totalPedido,
            parcelas
        );

        BigDecimal totalFinal = resultadoPagamento.get("totalFinal");
        BigDecimal valorParcela = resultadoPagamento.get("valorParcela");
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);
        ajustePagamento = arredondar(ajustePagamento);

        BigDecimal creditoProximaCompra = calcularCreditoClube(
            request.getNivelClube(),
            subtotalProdutos
        );

        boolean brinde = request.getNivelClube() == NivelClube.OURO
            && subtotalProdutos.compareTo(MINIMO_BRINDE_OURO) > 0;

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazo);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
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

    private void validarModalidadeEntrega(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(PESO_MAXIMO_MOTOBOY) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupom, BigDecimal subtotal) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        if (!List.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2").contains(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if ("MENOS50".equals(cupom) && subtotal.compareTo(MINIMO_MENOS50) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento, Integer parcelas) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int numeroParcelas = parcelas != null ? parcelas : 1;

        if (formaPagamento == FormaPagamento.PIX && numeroParcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (formaPagamento == FormaPagamento.BOLETO && numeroParcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (formaPagamento == FormaPagamento.CARTAO && (numeroParcelas < 1 || numeroParcelas > 12)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(MAXIMO_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = switch (modalidade) {
            case ECONOMICA -> new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoTotal));
            case EXPRESSA -> new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoTotal));
            case RETIRADA_LOJA -> new BigDecimal("0.00");
            case MOTOBOY -> new BigDecimal("18.00");
        };
        return arredondar(frete);
    }

    private Integer calcularPrazo(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> 7;
            case EXPRESSA -> 2;
            case RETIRADA_LOJA -> 1;
            case MOTOBOY -> 0;
        };
    }

    private BigDecimal calcularDescontoCupom(
        String cupom,
        List<ItemCarrinho> itens,
        BigDecimal subtotal,
        BigDecimal frete,
        boolean freteGratisOuro
    ) {
        BigDecimal desconto = switch (cupom) {
            case "BEMVINDO10" -> subtotal.multiply(new BigDecimal("0.10"));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> freteGratisOuro ? new BigDecimal("0.00") : frete;
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> new BigDecimal("0.00");
        };
        return arredondar(desconto);
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

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        BigDecimal seguro = subtotal.multiply(percentual);
        return arredondar(seguro);
    }

    private Map<String, BigDecimal> calcularPagamento(
        FormaPagamento formaPagamento,
        BigDecimal totalPedido,
        int numeroParcelas
    ) {
        BigDecimal totalFinal;
        BigDecimal valorParcela;

        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
            desconto = arredondar(desconto);
            totalFinal = totalPedido.subtract(desconto);
            totalFinal = arredondar(totalFinal);
            valorParcela = totalFinal;
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            totalFinal = totalPedido.add(new BigDecimal("3.49"));
            totalFinal = arredondar(totalFinal);
            valorParcela = totalFinal;
        } else {
            if (numeroParcelas <= 3) {
                valorParcela = totalPedido.divide(
                    new BigDecimal(numeroParcelas),
                    2,
                    RoundingMode.HALF_EVEN
                );
                totalFinal = totalPedido;
            } else {
                BigDecimal taxa = new BigDecimal("0.0199");
                BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                BigDecimal umMaisTaxaPotencia = umMaisTaxa.pow(numeroParcelas);
                BigDecimal divisor = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(umMaisTaxaPotencia, 10, RoundingMode.HALF_EVEN)
                );

                valorParcela = totalPedido.multiply(taxa).divide(divisor, 10, RoundingMode.HALF_EVEN);
                valorParcela = arredondar(valorParcela);

                totalFinal = valorParcela.multiply(new BigDecimal(numeroParcelas));
                totalFinal = arredondar(totalFinal);
            }
        }

        return Map.of(
            "totalFinal", totalFinal,
            "valorParcela", valorParcela
        );
    }

    private BigDecimal calcularCreditoClube(NivelClube nivelClube, BigDecimal subtotal) {
        BigDecimal percentual = switch (nivelClube) {
            case BRONZE -> new BigDecimal("0.00");
            case PRATA -> new BigDecimal("0.02");
            case OURO -> new BigDecimal("0.05");
        };

        BigDecimal credito = subtotal.multiply(percentual);
        return arredondar(credito);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
