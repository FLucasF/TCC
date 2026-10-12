package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClubeRegistry;
import com.loja.checkout.clube.NivelClubeStrategy;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.entrega.ModalidadeEntregaStrategy;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.regiao.RegiaoSeguro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutService {

    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;
    private static final int ESCALA = 2;
    private static final int ESCALA_INTERMEDIARIA = 10;

    private static final Set<String> CUPONS_VALIDOS = Set.of(
            "BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"
    );
    private static final Set<String> FORMAS_PAGAMENTO_VALIDAS = Set.of("PIX", "CARTAO", "BOLETO");

    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    private final ModalidadeEntregaRegistry entregaRegistry;
    private final NivelClubeRegistry clubeRegistry;

    public CheckoutService(ModalidadeEntregaRegistry entregaRegistry, NivelClubeRegistry clubeRegistry) {
        this.entregaRegistry = entregaRegistry;
        this.clubeRegistry = clubeRegistry;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Validar itens
        validarItens(req.itens());

        // 2. Validar nível do clube
        NivelClubeStrategy clube = clubeRegistry.encontrar(req.nivelClube());

        // 3. Validar região
        RegiaoSeguro regiao = RegiaoSeguro.parse(req.regiao());

        // 4. Validar modalidade de entrega
        ModalidadeEntregaStrategy entrega = entregaRegistry.encontrar(req.modalidadeEntrega());

        // 5. Verificar se a modalidade atende o pedido
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!entrega.atende(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Validar cupom (existência)
        BigDecimal subtotalProdutos = calcularSubtotal(req.itens());
        String cupom = req.cupom();
        if (cupom != null && !CUPONS_VALIDOS.contains(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        // 7. Validar aplicabilidade do cupom
        if (cupom != null) {
            validarAplicabilidadeCupom(cupom, subtotalProdutos, req.itens());
        }

        // 8. Validar forma de pagamento
        String formaPagamento = req.formaPagamento();
        if (formaPagamento == null || !FORMAS_PAGAMENTO_VALIDAS.contains(formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Validar parcelas
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);

        // --- Cálculos ---

        BigDecimal freteBase = arredondar(entrega.calcularFrete(pesoTotal));
        BigDecimal frete = clube.isFreteGratis() ? BigDecimal.ZERO.setScale(ESCALA) : freteBase;

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, frete, req.itens());

        BigDecimal seguro = arredondar(subtotalProdutos.multiply(regiao.getPercentual()));

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        // 10. Verificar disponibilidade da forma de pagamento
        if ("BOLETO".equals(formaPagamento) && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste de pagamento
        ResultadoPagamento pagamento = calcularPagamento(formaPagamento, totalPedido, parcelas);

        BigDecimal credito = arredondar(subtotalProdutos.multiply(clube.getPercentualCredito()));
        boolean brinde = clube.temBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.getPrazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.quantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemDto> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        return itens.stream()
                .map(i -> arredondar(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade()))))
                .reduce(BigDecimal.ZERO.setScale(ESCALA), BigDecimal::add);
    }

    private void validarAplicabilidadeCupom(String cupom, BigDecimal subtotalProdutos, List<ItemDto> itens) {
        if ("MENOS50".equals(cupom) && subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        if (cupom == null) return BigDecimal.ZERO.setScale(ESCALA);
        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> BigDecimal.ZERO.setScale(ESCALA);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemDto> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return arredondar(desconto);
    }

    private void validarParcelas(String formaPagamento, int parcelas) {
        boolean valido = switch (formaPagamento) {
            case "PIX", "BOLETO" -> parcelas == 1;
            case "CARTAO" -> parcelas >= 1 && parcelas <= 12;
            default -> false;
        };
        if (!valido) throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }

    private ResultadoPagamento calcularPagamento(String formaPagamento, BigDecimal totalPedido, int parcelas) {
        return switch (formaPagamento) {
            case "PIX" -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
                BigDecimal totalFinal = totalPedido.subtract(desconto);
                yield new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
            }
            case "BOLETO" -> {
                BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
                yield new ResultadoPagamento(TARIFA_BOLETO, totalFinal, totalFinal);
            }
            case "CARTAO" -> calcularPagamentoCartao(totalPedido, parcelas);
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        };
    }

    private ResultadoPagamento calcularPagamentoCartao(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), ESCALA_INTERMEDIARIA, ARREDONDAMENTO)
            );
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(ESCALA), totalPedido, valorParcela);
        }
        // Tabela Price: parcela = total × taxa / (1 − (1 + taxa)^-n)
        BigDecimal um = BigDecimal.ONE;
        BigDecimal unMaisTaxa = um.add(TAXA_JUROS_CARTAO);
        BigDecimal fatorN = unMaisTaxa.pow(parcelas, new MathContext(ESCALA_INTERMEDIARIA + 2));
        BigDecimal denominador = um.subtract(
                um.divide(fatorN, ESCALA_INTERMEDIARIA, ARREDONDAMENTO)
        );
        BigDecimal valorParcela = arredondar(
                totalPedido.multiply(TAXA_JUROS_CARTAO)
                        .divide(denominador, ESCALA_INTERMEDIARIA, ARREDONDAMENTO)
        );
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    private record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
