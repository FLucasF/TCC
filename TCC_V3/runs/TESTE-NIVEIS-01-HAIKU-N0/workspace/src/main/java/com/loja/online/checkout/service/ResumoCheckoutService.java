package com.loja.online.checkout.service;

import com.loja.online.checkout.dto.ItemCarrinhoDTO;
import com.loja.online.checkout.dto.ResumoCheckoutRequestDTO;
import com.loja.online.checkout.dto.ResumoCheckoutResponseDTO;
import com.loja.online.checkout.enums.FormaPagamento;
import com.loja.online.checkout.enums.ModalidadeEntrega;
import com.loja.online.checkout.enums.NivelClube;
import com.loja.online.checkout.enums.Regiao;
import com.loja.online.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResumoCheckoutService {

    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;
    private static final int ESCALA_MOEDA = 2;

    public ResumoCheckoutResponseDTO calcularResumo(ResumoCheckoutRequestDTO request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());

        BigDecimal frete = calcularFrete(request.modalidadeEntrega(), request.itens(), request.nivelClube());
        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotalProdutos, request.itens(), frete);
        BigDecimal baseFrete = subtotalProdutos.subtract(descontoCupom);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.regiao());

        BigDecimal totalAntesAjuste = baseFrete.add(frete).add(seguro);
        Integer parcelas = request.parcelas() != null ? request.parcelas() : 1;

        BigDecimal valorParcela = calcularValorParcela(request.formaPagamento(), totalAntesAjuste, parcelas);
        BigDecimal totalFinal = arredondar(valorParcela.multiply(new BigDecimal(parcelas)));
        BigDecimal ajustePagamento = totalFinal.subtract(totalAntesAjuste);

        BigDecimal creditoProximaCompra = calcularCredito(request.nivelClube(), subtotalProdutos);
        Boolean brinde = verificarBrinde(request.nivelClube(), subtotalProdutos);

        Integer prazoEntregaDias = obterPrazoEntrega(request.modalidadeEntrega());

        return new ResumoCheckoutResponseDTO(
            arredondar(subtotalProdutos),
            arredondar(descontoCupom),
            arredondar(frete),
            prazoEntregaDias,
            arredondar(seguro),
            arredondar(ajustePagamento),
            arredondar(totalFinal),
            parcelas,
            arredondar(valorParcela),
            arredondar(creditoProximaCompra),
            brinde
        );
    }

    private void validarPedido(ResumoCheckoutRequestDTO request) {
        // 1. Validar carrinho
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinhoDTO item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar nível do clube
        if (request.nivelClube() == null ||
            !Arrays.stream(NivelClube.values()).anyMatch(n -> n == request.nivelClube())) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Validar região
        if (request.regiao() == null ||
            !Arrays.stream(Regiao.values()).anyMatch(r -> r == request.regiao())) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        // 4. Validar modalidade de entrega existe
        if (request.modalidadeEntrega() == null ||
            !Arrays.stream(ModalidadeEntrega.values()).anyMatch(m -> m == request.modalidadeEntrega())) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // 5. Validar modalidade de entrega atende o pedido
        BigDecimal pesoTotal = request.itens().stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.modalidadeEntrega() == ModalidadeEntrega.MOTOBOY &&
            pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Validar cupom existe
        if (request.cupom() != null && !cupomValido(request.cupom())) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        // 7. Validar cupom é aplicável
        if (request.cupom() != null) {
            BigDecimal subtotal = calcularSubtotalProdutos(request.itens());
            BigDecimal freteTmp = calcularFrete(request.modalidadeEntrega(), request.itens(), request.nivelClube());
            if (!cupomAplicavel(request.cupom(), subtotal, request.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Validar forma de pagamento existe
        if (request.formaPagamento() == null ||
            !Arrays.stream(FormaPagamento.values()).anyMatch(f -> f == request.formaPagamento())) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Validar número de parcelas
        Integer parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!parcelasValidas(request.formaPagamento(), parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // 10. Validar forma de pagamento atende o pedido
        BigDecimal subtotal = calcularSubtotalProdutos(request.itens());
        BigDecimal frete2 = calcularFrete(request.modalidadeEntrega(), request.itens(), request.nivelClube());
        BigDecimal desconto = calcularDescontoCupom(request.cupom(), subtotal, request.itens(), frete2);
        BigDecimal seguro = calcularSeguro(subtotal, request.regiao());
        BigDecimal total = subtotal.subtract(desconto).add(frete2).add(seguro);

        if (request.formaPagamento() == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinhoDTO> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos, List<ItemCarrinhoDTO> itens, BigDecimal frete) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> arredondar(new BigDecimal("50.00"));
            case "FRETEGRATIS" -> arredondar(frete);
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinhoDTO> itens) {
        BigDecimal totalDesconto = BigDecimal.ZERO;
        for (ItemCarrinhoDTO item : itens) {
            int gruposCompletos = item.quantidade() / 3;
            BigDecimal descontoItem = new BigDecimal(gruposCompletos).multiply(item.precoUnitario());
            totalDesconto = totalDesconto.add(descontoItem);
        }
        return arredondar(totalDesconto);
    }

    private boolean cupomValido(String cupom) {
        Set<String> cuponsValidos = new HashSet<>(Arrays.asList(
            "BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"
        ));
        return cuponsValidos.contains(cupom);
    }

    private boolean cupomAplicavel(String cupom, BigDecimal subtotal, List<ItemCarrinhoDTO> itens) {
        return switch (cupom) {
            case "BEMVINDO10", "FRETEGRATIS", "LEVE3PAGUE2" -> true;
            case "MENOS50" -> subtotal.compareTo(new BigDecimal("300.00")) >= 0;
            default -> false;
        };
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemCarrinhoDTO> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return switch (modalidade) {
            case ECONOMICA -> arredondar(
                new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotal))
            );
            case EXPRESSA -> arredondar(
                new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotal))
            );
            case RETIRADA_LOJA -> BigDecimal.ZERO;
            case MOTOBOY -> new BigDecimal("18.00");
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal percentual = switch (regiao) {
            case SUDESTE, SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        return arredondar(subtotalProdutos.multiply(percentual));
    }


    private BigDecimal calcularValorParcela(FormaPagamento forma, BigDecimal total, Integer parcelas) {
        BigDecimal totalAjustado = total;

        if (forma == FormaPagamento.PIX) {
            totalAjustado = arredondar(total.multiply(new BigDecimal("0.95")));
        } else if (forma == FormaPagamento.BOLETO) {
            totalAjustado = total.add(new BigDecimal("3.49"));
        } else if (forma == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = umMaisTaxa.pow(parcelas);
            BigDecimal numerador = totalAjustado.multiply(taxa).multiply(potencia);
            BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
            totalAjustado = numerador.divide(denominador, ESCALA_MOEDA, ARREDONDAMENTO);
            return totalAjustado;
        }

        return arredondar(totalAjustado.divide(new BigDecimal(parcelas), ESCALA_MOEDA, ARREDONDAMENTO));
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        return switch (nivelClube) {
            case BRONZE -> BigDecimal.ZERO;
            case PRATA -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
            case OURO -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        };
    }

    private Boolean verificarBrinde(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        return nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }

    private Integer obterPrazoEntrega(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> 7;
            case EXPRESSA -> 2;
            case RETIRADA_LOJA -> 1;
            case MOTOBOY -> 0;
        };
    }

    private boolean parcelasValidas(FormaPagamento forma, Integer parcelas) {
        return switch (forma) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= 12;
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA_MOEDA, ARREDONDAMENTO);
    }
}
