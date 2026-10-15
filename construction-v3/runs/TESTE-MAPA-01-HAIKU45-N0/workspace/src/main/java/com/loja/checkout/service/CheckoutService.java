package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarPedido(request);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        validarModalidadeEntrega(request.getModalidadeEntrega());
        validarDisponibilidadeModalidade(request.getModalidadeEntrega(), request.getItens());

        if (request.getCupom() != null) {
            if (!isCupomValido(request.getCupom())) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            validarAplicabilidadeCupom(request.getCupom(), subtotalProdutos);
        }

        BigDecimal frete = calcularFrete(
            request.getModalidadeEntrega(),
            request.getItens(),
            request.getNivelClube()
        );

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, frete, request.getItens());

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        validarFormaPagamento(request.getFormaPagamento());
        validarParcelamento(request.getFormaPagamento(), parcelas);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarDisponibilidadeFormaPagamento(request.getFormaPagamento(), totalPedido);

        BigDecimal ajustePagamento = calcularAjustePagamento(
            request.getFormaPagamento(),
            totalPedido,
            parcelas
        );

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, request.getNivelClube());

        boolean brinde = temBrinde(subtotalProdutos, request.getNivelClube());

        BigDecimal valorParcela = calcularValorParcela(
            request.getFormaPagamento(),
            totalFinal,
            parcelas
        );

        return new CheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            obterPrazoEntrega(request.getModalidadeEntrega()),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (CheckoutRequest.Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null || !isNivelClubeValido(request.getNivelClube())) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null || !isRegiaoValida(request.getRegiao())) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null || !isModalidadeValida(request.getModalidadeEntrega())) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (request.getFormaPagamento() == null || !isFormaPagamentoValida(request.getFormaPagamento())) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<CheckoutRequest.Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CheckoutRequest.Item item : itens) {
            BigDecimal itemTotal = BigDecimal.valueOf(item.getPrecoUnitario())
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete, List<CheckoutRequest.Item> itens) {
        if (cupom == null) {
            return arredondar(BigDecimal.ZERO);
        }

        if ("BEMVINDO10".equals(cupom)) {
            return arredondar(subtotal.multiply(BigDecimal.valueOf(0.10)));
        } else if ("MENOS50".equals(cupom)) {
            return arredondar(BigDecimal.valueOf(50.00));
        } else if ("FRETEGRATIS".equals(cupom)) {
            return arredondar(frete);
        } else if ("LEVE3PAGUE2".equals(cupom)) {
            return calcularDescontoLeve3Pague2(itens, subtotal);
        }

        return arredondar(BigDecimal.ZERO);
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<CheckoutRequest.Item> itens, BigDecimal subtotal) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (CheckoutRequest.Item item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal precoUnitario = BigDecimal.valueOf(item.getPrecoUnitario());
                BigDecimal descontoItem = precoUnitario.multiply(BigDecimal.valueOf(unidadesGratis));
                desconto = desconto.add(descontoItem);
            }
        }

        return arredondar(desconto);
    }

    private void validarDisponibilidadeModalidade(String modalidade, List<CheckoutRequest.Item> itens) {
        if ("MOTOBOY".equals(modalidade)) {
            double pesoTotal = itens.stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
            if (pesoTotal > 5.0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarAplicabilidadeCupom(String cupom, BigDecimal subtotal) {
        if ("MENOS50".equals(cupom)) {
            if (subtotal.compareTo(BigDecimal.valueOf(300.00)) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private BigDecimal calcularFrete(String modalidade, List<CheckoutRequest.Item> itens, String nivelClube) {
        if ("OURO".equals(nivelClube)) {
            return arredondar(BigDecimal.ZERO);
        }

        double pesoTotal = itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();

        BigDecimal frete = switch (modalidade) {
            case "ECONOMICA" -> BigDecimal.valueOf(12.00)
                .add(BigDecimal.valueOf(pesoTotal).multiply(BigDecimal.valueOf(2.00)));
            case "EXPRESSA" -> BigDecimal.valueOf(25.00)
                .add(BigDecimal.valueOf(pesoTotal).multiply(BigDecimal.valueOf(4.50)));
            case "RETIRADA_LOJA" -> BigDecimal.ZERO;
            case "MOTOBOY" -> BigDecimal.valueOf(18.00);
            default -> BigDecimal.ZERO;
        };

        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, String regiao) {
        BigDecimal percentual = switch (regiao) {
            case "SUDESTE" -> BigDecimal.valueOf(0.01);
            case "SUL" -> BigDecimal.valueOf(0.01);
            case "CENTRO_OESTE" -> BigDecimal.valueOf(0.015);
            case "NORTE" -> BigDecimal.valueOf(0.025);
            case "NORDESTE" -> BigDecimal.valueOf(0.02);
            default -> BigDecimal.ZERO;
        };

        return arredondar(subtotal.multiply(percentual));
    }

    private void validarParcelamento(String formaPagamento, int parcelas) {
        if ("PIX".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if ("CARTAO".equals(formaPagamento)) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarDisponibilidadeFormaPagamento(String formaPagamento, BigDecimal total) {
        if ("BOLETO".equals(formaPagamento) && total.compareTo(BigDecimal.valueOf(1000.00)) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularAjustePagamento(String formaPagamento, BigDecimal total, int parcelas) {
        return switch (formaPagamento) {
            case "PIX" -> arredondar(total.multiply(BigDecimal.valueOf(-0.05)));
            case "CARTAO" -> calcularAjusteCartao(total, parcelas);
            case "BOLETO" -> arredondar(BigDecimal.valueOf(3.49));
            default -> arredondar(BigDecimal.ZERO);
        };
    }

    private BigDecimal calcularAjusteCartao(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return arredondar(BigDecimal.ZERO);
        }

        BigDecimal taxa = BigDecimal.valueOf(0.0199);
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxa);

        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal umMaisTaxaNegativo = um.divide(
            umMaisTaxaElevado,
            20,
            RoundingMode.HALF_EVEN
        );

        BigDecimal denominador = um.subtract(umMaisTaxaNegativo);

        BigDecimal parcela = total
            .multiply(taxa)
            .divide(denominador, 10, RoundingMode.DOWN)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
        return arredondar(totalComJuros.subtract(total));
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, String nivelClube) {
        BigDecimal percentual = switch (nivelClube) {
            case "PRATA" -> BigDecimal.valueOf(0.02);
            case "OURO" -> BigDecimal.valueOf(0.05);
            default -> BigDecimal.ZERO;
        };

        return arredondar(subtotal.multiply(percentual));
    }

    private boolean temBrinde(BigDecimal subtotal, String nivelClube) {
        if ("OURO".equals(nivelClube) && subtotal.compareTo(BigDecimal.valueOf(500.00)) > 0) {
            return true;
        }
        return false;
    }

    private BigDecimal calcularValorParcela(String formaPagamento, BigDecimal totalFinal, int parcelas) {
        return arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN));
    }

    private Integer obterPrazoEntrega(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> 0;
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isNivelClubeValido(String nivel) {
        return Arrays.asList("BRONZE", "PRATA", "OURO").contains(nivel);
    }

    private boolean isRegiaoValida(String regiao) {
        return Arrays.asList("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE").contains(regiao);
    }

    private boolean isModalidadeValida(String modalidade) {
        return Arrays.asList("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY").contains(modalidade);
    }

    private boolean isCupomValido(String cupom) {
        return Arrays.asList("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2").contains(cupom);
    }

    private boolean isFormaPagamentoValida(String forma) {
        return Arrays.asList("PIX", "CARTAO", "BOLETO").contains(forma);
    }

    private void validarModalidadeEntrega(String modalidade) {
        if (!isModalidadeValida(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarFormaPagamento(String forma) {
        if (!isFormaPagamentoValida(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
