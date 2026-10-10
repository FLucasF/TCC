package com.loja.checkout.service;

import com.loja.checkout.domain.ContextoCalculo;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoService {

    private static final BigDecimal TAXA_JUROS_CARTAO_AO_MES = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS_ATE = 3;
    private static final int PARCELAS_MAXIMAS_CARTAO = 12;
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req.itens());
        NivelClube nivelClube = parseEnum(NivelClube.class, req.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parseEnum(Regiao.class, req.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal subtotalProdutos = calcularSubtotal(req.itens());
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());

        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivelClube.freteGratis()
                ? BigDecimal.ZERO
                : Dinheiro.arredondar(modalidade.calcularFrete(pesoTotal));

        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = parseEnum(Cupom.class, req.cupom(), CodigoErro.CUPOM_INVALIDO);
        }

        ContextoCalculo contexto = new ContextoCalculo(req.itens(), subtotalProdutos, frete);

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (cupom != null) {
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(contexto);
        }

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, req.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        PagamentoCalculado pagamento = calcularPagamento(formaPagamento, parcelas, totalPedido);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private void validarParcelas(FormaPagamento formaPagamento, int parcelas) {
        boolean valido = switch (formaPagamento) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS_CARTAO;
        };
        if (!valido) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private PagamentoCalculado calcularPagamento(FormaPagamento formaPagamento, int parcelas, BigDecimal totalPedido) {
        return switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO_PIX));
                BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
                yield new PagamentoCalculado(desconto.negate(), totalFinal, totalFinal);
            }
            case BOLETO -> {
                BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA_BOLETO));
                yield new PagamentoCalculado(TARIFA_BOLETO, totalFinal, totalFinal);
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS_ATE) {
                    BigDecimal valorParcela = Dinheiro.arredondar(
                            totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
                    yield new PagamentoCalculado(BigDecimal.ZERO, totalPedido, valorParcela);
                }
                BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_CARTAO_AO_MES);
                BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                        BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 20, java.math.RoundingMode.HALF_EVEN));
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.multiply(TAXA_JUROS_CARTAO_AO_MES)
                                .divide(fatorDesconto, 10, java.math.RoundingMode.HALF_EVEN));
                BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
                yield new PagamentoCalculado(ajuste, totalFinal, valorParcela);
            }
        };
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, CodigoErro codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private record PagamentoCalculado(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}
