package com.loja.servico;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.enums.Cupom;
import com.loja.enums.FormaPagamento;
import com.loja.enums.NivelClube;
import com.loja.enums.Regiao;
import com.loja.enums.TipoEntrega;
import com.loja.estrategia.AjustePagamentoResult;
import com.loja.estrategia.CupomFactory;
import com.loja.estrategia.EstrategiaAjustePagamento;
import com.loja.estrategia.EstrategiaCupom;
import com.loja.estrategia.EstrategiaFrete;
import com.loja.estrategia.FreteFactory;
import com.loja.estrategia.PagamentoFactory;
import com.loja.util.ArredondadorMoeda;
import com.loja.validacao.ValidadorCheckout;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ServicoCheckout {
    private final ValidadorCheckout validador = new ValidadorCheckout();

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        String erro = validador.validar(request);
        if (erro != null) {
            throw new ErroCheckout(erro);
        }

        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
        subtotalProdutos = ArredondadorMoeda.arredondar(subtotalProdutos);

        TipoEntrega tipoEntrega = TipoEntrega.fromString(request.getModalidadeEntrega());
        Cupom cupom = Cupom.fromString(request.getCupom());
        NivelClube nivelClube = NivelClube.fromString(request.getNivelClube());
        Regiao regiao = Regiao.fromString(request.getRegiao());
        FormaPagamento formaPagamento = FormaPagamento.fromString(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, request.getItens(), tipoEntrega);
        descontoCupom = ArredondadorMoeda.arredondar(descontoCupom);

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        BigDecimal frete = calcularFrete(tipoEntrega, pesoTotal, nivelClube);
        frete = ArredondadorMoeda.arredondar(frete);
        int prazoEntrega = obterPrazoEntrega(tipoEntrega);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        seguro = ArredondadorMoeda.arredondar(seguro);

        BigDecimal totalAntesAjuste = ArredondadorMoeda.arredondar(
            subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro)
        );

        AjustePagamentoResult ajuste = calcularAjustePagamento(formaPagamento, totalAntesAjuste, parcelas);
        BigDecimal ajustePagamento = ArredondadorMoeda.arredondar(ajuste.ajuste);

        BigDecimal totalFinal = ArredondadorMoeda.arredondar(totalAntesAjuste.add(ajustePagamento));
        totalFinal = ArredondadorMoeda.arredondar(totalFinal);

        BigDecimal valorParcela = ArredondadorMoeda.arredondar(ajuste.valorParcela);

        BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);
        creditoProximaCompra = ArredondadorMoeda.arredondar(creditoProximaCompra);

        boolean brinde = verificarBrinde(nivelClube, subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntrega,
                seguro,
                ajustePagamento,
                totalFinal,
                ajuste.parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde
        );
    }

    private BigDecimal calcularSubtotal(java.util.List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal, java.util.List<ItemRequest> itens, TipoEntrega tipoEntrega) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        EstrategiaCupom estrategia = CupomFactory.criar(cupom, itens);

        BigDecimal frete = BigDecimal.ZERO;
        if (cupom == Cupom.FRETEGRATIS) {
            EstrategiaFrete estrategiaFrete = FreteFactory.criar(tipoEntrega);
            BigDecimal pesoTotal = calcularPesoTotal(itens);
            frete = estrategiaFrete.calcularCusto(pesoTotal);
        }

        return estrategia.aplicar(subtotal, frete);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
        }
        return peso;
    }

    private BigDecimal calcularFrete(TipoEntrega tipoEntrega, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        EstrategiaFrete estrategia = FreteFactory.criar(tipoEntrega);
        return estrategia.calcularCusto(pesoTotal);
    }

    private int obterPrazoEntrega(TipoEntrega tipoEntrega) {
        EstrategiaFrete estrategia = FreteFactory.criar(tipoEntrega);
        return estrategia.prazo();
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = obterPercentualSeguro(regiao);
        return subtotal.multiply(percentual);
    }

    private BigDecimal obterPercentualSeguro(Regiao regiao) {
        return switch (regiao) {
            case SUDESTE, SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };
    }

    private AjustePagamentoResult calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, int parcelas) {
        EstrategiaAjustePagamento estrategia = PagamentoFactory.criar(formaPagamento);
        return estrategia.calcular(total, parcelas);
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotal) {
        if (nivelClube == NivelClube.BRONZE) {
            return BigDecimal.ZERO;
        } else if (nivelClube == NivelClube.PRATA) {
            return subtotal.multiply(new BigDecimal("0.02"));
        } else if (nivelClube == NivelClube.OURO) {
            return subtotal.multiply(new BigDecimal("0.05"));
        }
        return BigDecimal.ZERO;
    }

    private boolean verificarBrinde(NivelClube nivelClube, BigDecimal subtotal) {
        return nivelClube == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }
}
