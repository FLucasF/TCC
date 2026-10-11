package com.loja.checkout.service;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ResumoService {

    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal LIMITE_PESO_MOTOBOY = new BigDecimal("5");
    private static final BigDecimal LIMITE_BRINDE_OURO = new BigDecimal("500.00");

    public ResumoResponse calcular(ResumoRequest request) {
        validarItens(request.itens());
        NivelClube nivelClube = parseNivelClube(request.nivelClube());
        Regiao regiao = parseRegiao(request.regiao());
        ModalidadeEntrega modalidade = parseModalidade(request.modalidadeEntrega());

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal peso = calcularPeso(request.itens());

        FreteCalculado freteCalculado = calcularFrete(modalidade, peso);
        BigDecimal frete = nivelClube == NivelClube.OURO ? BigDecimal.ZERO.setScale(2) : freteCalculado.custo();
        int prazoEntregaDias = freteCalculado.prazoDias();

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotal, frete, request.itens());

        BigDecimal seguro = arredondar(subtotal.multiply(percentualSeguro(regiao)));

        BigDecimal totalPedido = arredondar(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        validarParcelas(formaPagamento, parcelas);
        validarDisponibilidadePagamento(formaPagamento, totalPedido);

        PagamentoCalculado pagamento = calcularPagamento(formaPagamento, parcelas, totalPedido);

        BigDecimal credito = arredondar(subtotal.multiply(percentualCredito(nivelClube)));
        boolean brinde = nivelClube == NivelClube.OURO && subtotal.compareTo(LIMITE_BRINDE_OURO) > 0;

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                prazoEntregaDias,
                seguro,
                pagamento.ajuste(),
                pagamento.valorFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemDTO> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDTO item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube parseNivelClube(String valor) {
        return parseEnum(NivelClube.class, valor, "NIVEL_CLUBE_INVALIDO");
    }

    private Regiao parseRegiao(String valor) {
        return parseEnum(Regiao.class, valor, "REGIAO_INVALIDA");
    }

    private ModalidadeEntrega parseModalidade(String valor) {
        return parseEnum(ModalidadeEntrega.class, valor, "MODALIDADE_INVALIDA");
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        return parseEnum(FormaPagamento.class, valor, "FORMA_PAGAMENTO_INVALIDA");
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDTO> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemDTO> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private FreteCalculado calcularFrete(ModalidadeEntrega modalidade, BigDecimal peso) {
        return switch (modalidade) {
            case ECONOMICA -> new FreteCalculado(
                    arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(peso))), 7);
            case EXPRESSA -> new FreteCalculado(
                    arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(peso))), 2);
            case RETIRADA_LOJA -> new FreteCalculado(BigDecimal.ZERO.setScale(2), 1);
            case MOTOBOY -> {
                if (peso.compareTo(LIMITE_PESO_MOTOBOY) > 0) {
                    throw new CheckoutException("MODALIDADE_INDISPONIVEL");
                }
                yield new FreteCalculado(new BigDecimal("18.00"), 0);
            }
        };
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete, List<ItemDTO> itens) {
        if (cupom == null || cupom.isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> {
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
                yield new BigDecimal("50.00");
            }
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> {
                BigDecimal desconto = BigDecimal.ZERO;
                for (ItemDTO item : itens) {
                    int unidadesGratis = item.quantidade() / 3;
                    desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
                }
                yield arredondar(desconto);
            }
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }

    private BigDecimal percentualSeguro(Regiao regiao) {
        return switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };
    }

    private BigDecimal percentualCredito(NivelClube nivelClube) {
        return switch (nivelClube) {
            case BRONZE -> BigDecimal.ZERO;
            case PRATA -> new BigDecimal("0.02");
            case OURO -> new BigDecimal("0.05");
        };
    }

    private void validarParcelas(FormaPagamento formaPagamento, int parcelas) {
        boolean valido = switch (formaPagamento) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= 12;
        };
        if (!valido) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarDisponibilidadePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private PagamentoCalculado calcularPagamento(FormaPagamento formaPagamento, int parcelas, BigDecimal totalPedido) {
        return switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
                BigDecimal valorFinal = arredondar(totalPedido.subtract(desconto));
                yield new PagamentoCalculado(desconto.negate(), valorFinal, valorFinal);
            }
            case BOLETO -> {
                BigDecimal valorFinal = arredondar(totalPedido.add(TARIFA_BOLETO));
                yield new PagamentoCalculado(TARIFA_BOLETO, valorFinal, valorFinal);
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                    yield new PagamentoCalculado(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
                } else {
                    double totalD = totalPedido.doubleValue();
                    double taxaD = TAXA_JUROS_CARTAO.doubleValue();
                    double fatorDivisor = 1 - Math.pow(1 + taxaD, -parcelas);
                    double valorParcelaD = totalD * taxaD / fatorDivisor;
                    BigDecimal valorParcela = BigDecimal.valueOf(valorParcelaD).setScale(2, RoundingMode.HALF_EVEN);
                    BigDecimal valorFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                    BigDecimal ajuste = valorFinal.subtract(totalPedido);
                    yield new PagamentoCalculado(ajuste, valorFinal, valorParcela);
                }
            }
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record FreteCalculado(BigDecimal custo, int prazoDias) {
    }

    private record PagamentoCalculado(BigDecimal ajuste, BigDecimal valorFinal, BigDecimal valorParcela) {
    }
}
