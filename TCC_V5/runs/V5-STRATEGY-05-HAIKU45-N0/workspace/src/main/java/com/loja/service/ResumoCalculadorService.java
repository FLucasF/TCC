package com.loja.service;

import com.loja.dto.Item;
import com.loja.dto.ResumoRequest;
import com.loja.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class ResumoCalculadorService {

    private static final Map<String, Integer> PRAZO_ENTREGA = Map.of(
        "ECONOMICA", 7,
        "EXPRESSA", 2,
        "RETIRADA_LOJA", 1,
        "MOTOBOY", 0
    );

    private static final Map<String, Double> TAXA_SEGURO = Map.of(
        "SUDESTE", 0.01,
        "SUL", 0.01,
        "CENTRO_OESTE", 0.015,
        "NORTE", 0.025,
        "NORDESTE", 0.02
    );

    public ResumoResponse calcular(ResumoRequest request) throws ValidationException {
        validar(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getNivelClube());

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, request.getItens(), frete);

        Integer prazoEntrega = PRAZO_ENTREGA.get(request.getModalidadeEntrega());

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        BigDecimal ajustePagamento = calcularAjustePagamento(request.getFormaPagamento(), totalPedido, parcelas);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        BigDecimal valorParcela = calcularValorParcela(request.getFormaPagamento(), totalFinal, parcelas);

        BigDecimal creditoProximaCompra = calcularCredito(request.getNivelClube(), subtotalProdutos);

        Boolean brinde = calcularBrinde(request.getNivelClube(), subtotalProdutos);

        ResumoResponse response = new ResumoResponse();
        response.setSubtotalProdutos(arredondar(subtotalProdutos));
        response.setDescontoCupom(arredondar(descontoCupom));
        response.setFrete(arredondar(frete));
        response.setPrazoEntregaDias(prazoEntrega);
        response.setSeguro(arredondar(seguro));
        response.setAjustePagamento(arredondar(ajustePagamento));
        response.setTotalFinal(arredondar(totalFinal));
        response.setParcelas(parcelas);
        response.setValorParcela(arredondar(valorParcela));
        response.setCreditoProximaCompra(arredondar(creditoProximaCompra));
        response.setBrinde(brinde);

        return response;
    }

    private void validar(ResumoRequest request) throws ValidationException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ValidationException("PEDIDO_INVALIDO");
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ValidationException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null || !isNivelClubeValido(request.getNivelClube())) {
            throw new ValidationException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null || !isRegiaoValida(request.getRegiao())) {
            throw new ValidationException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null || !isModalidadeValida(request.getModalidadeEntrega())) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }

        if (!isModalidadeDisponivel(request.getModalidadeEntrega(), request.getItens())) {
            throw new ValidationException("MODALIDADE_INDISPONIVEL");
        }

        if (request.getCupom() != null && !isCupomValido(request.getCupom())) {
            throw new ValidationException("CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
        BigDecimal fretePorValidacao = calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getNivelClube());
        if (request.getCupom() != null && !isCupomAplicavel(request.getCupom(), subtotal, request.getItens())) {
            throw new ValidationException("CUPOM_NAO_APLICAVEL");
        }

        if (request.getFormaPagamento() == null || !isFormaPagamentoValida(request.getFormaPagamento())) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!isParcelamentoValido(request.getFormaPagamento(), parcelas)) {
            throw new ValidationException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = calcularSeguro(subtotal, request.getRegiao());
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotal, request.getItens(), fretePorValidacao);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(fretePorValidacao).add(seguro);
        BigDecimal totalFinal = totalPedido.add(calcularAjustePagamento(request.getFormaPagamento(), totalPedido, parcelas));

        if (!isFormaPagamentoDisponivel(request.getFormaPagamento(), totalFinal)) {
            throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private boolean isNivelClubeValido(String nivelClube) {
        return "BRONZE".equals(nivelClube) || "PRATA".equals(nivelClube) || "OURO".equals(nivelClube);
    }

    private boolean isRegiaoValida(String regiao) {
        return TAXA_SEGURO.containsKey(regiao);
    }

    private boolean isModalidadeValida(String modalidade) {
        return PRAZO_ENTREGA.containsKey(modalidade);
    }

    private boolean isModalidadeDisponivel(String modalidade, List<Item> itens) {
        if ("MOTOBOY".equals(modalidade)) {
            double pesoTotal = itens.stream().mapToDouble(item -> item.getPesoKg() * item.getQuantidade()).sum();
            return pesoTotal <= 5;
        }
        return true;
    }

    private boolean isCupomValido(String cupom) {
        return "BEMVINDO10".equals(cupom) || "MENOS50".equals(cupom) || "FRETEGRATIS".equals(cupom) || "LEVE3PAGUE2".equals(cupom);
    }

    private boolean isCupomAplicavel(String cupom, BigDecimal subtotal, List<Item> itens) {
        if ("MENOS50".equals(cupom)) {
            return subtotal.compareTo(BigDecimal.valueOf(300)) >= 0;
        }
        return true;
    }

    private boolean isFormaPagamentoValida(String forma) {
        return "PIX".equals(forma) || "CARTAO".equals(forma) || "BOLETO".equals(forma);
    }

    private boolean isParcelamentoValido(String forma, Integer parcelas) {
        if ("PIX".equals(forma) || "BOLETO".equals(forma)) {
            return parcelas == 1;
        }
        if ("CARTAO".equals(forma)) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean isFormaPagamentoDisponivel(String forma, BigDecimal totalFinal) {
        if ("BOLETO".equals(forma)) {
            return totalFinal.compareTo(BigDecimal.valueOf(1000)) <= 0;
        }
        return true;
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return itens.stream()
            .map(item -> BigDecimal.valueOf(item.getPrecoUnitario()).multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotal.multiply(BigDecimal.valueOf(0.10)));
            case "MENOS50" -> arredondar(BigDecimal.valueOf(50));
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<Item> itens) {
        if (itens == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int gratis = item.getQuantidade() / 3;
            if (gratis > 0) {
                desconto = desconto.add(BigDecimal.valueOf(item.getPrecoUnitario()).multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(String modalidade, List<Item> itens, String nivelClube) {
        if ("RETIRADA_LOJA".equals(modalidade)) {
            return BigDecimal.ZERO;
        }

        if ("OURO".equals(nivelClube)) {
            return BigDecimal.ZERO;
        }

        double pesoTotal = itens.stream().mapToDouble(item -> item.getPesoKg() * item.getQuantidade()).sum();

        return switch (modalidade) {
            case "ECONOMICA" -> arredondar(BigDecimal.valueOf(12 + 2.0 * pesoTotal));
            case "EXPRESSA" -> arredondar(BigDecimal.valueOf(25 + 4.5 * pesoTotal));
            case "MOTOBOY" -> arredondar(BigDecimal.valueOf(18));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, String regiao) {
        Double taxa = TAXA_SEGURO.get(regiao);
        return arredondar(subtotal.multiply(BigDecimal.valueOf(taxa)));
    }

    private BigDecimal calcularAjustePagamento(String forma, BigDecimal total, Integer parcelas) {
        return switch (forma) {
            case "PIX" -> arredondar(total.multiply(BigDecimal.valueOf(-0.05)));
            case "CARTAO" -> calcularAjusteCartao(total, parcelas);
            case "BOLETO" -> arredondar(BigDecimal.valueOf(3.49));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularAjusteCartao(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxa = BigDecimal.valueOf(0.0199);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal potenciaNegativa = BigDecimal.ONE.divide(potencia, 15, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(potenciaNegativa);
        BigDecimal parcela = arredondar(total.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN));

        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        return totalFinal.subtract(total);
    }

    private BigDecimal calcularValorParcela(String forma, BigDecimal totalFinal, Integer parcelas) {
        return arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularCredito(String nivelClube, BigDecimal subtotal) {
        return switch (nivelClube) {
            case "PRATA" -> arredondar(subtotal.multiply(BigDecimal.valueOf(0.02)));
            case "OURO" -> arredondar(subtotal.multiply(BigDecimal.valueOf(0.05)));
            default -> BigDecimal.ZERO;
        };
    }

    private Boolean calcularBrinde(String nivelClube, BigDecimal subtotal) {
        if ("OURO".equals(nivelClube) && subtotal.compareTo(BigDecimal.valueOf(500)) > 0) {
            return true;
        }
        return false;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class ValidationException extends Exception {
        private final String code;

        public ValidationException(String code) {
            super(code);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
