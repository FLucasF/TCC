package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.CatalogoClube;
import com.loja.checkout.dominio.clube.PoliticaClube;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.PoliticaCupom;
import com.loja.checkout.dominio.entrega.CatalogoEntregas;
import com.loja.checkout.dominio.entrega.PoliticaEntrega;
import com.loja.checkout.dominio.pagamento.CatalogoPagamentos;
import com.loja.checkout.dominio.pagamento.PoliticaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra na ordem definida pelo negocio: produtos,
 * desconto do cupom, frete, seguro, total do pedido e, por fim, o ajuste
 * da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoClube clube;
    private final CatalogoPagamentos pagamentos;

    public CalculadoraResumo(CatalogoEntregas entregas, CatalogoCupons cupons,
                             CatalogoClube clube, CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.clube = clube;
        this.pagamentos = pagamentos;
    }

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        BaseCalculo base = BaseCalculo.de(validarItens(solicitacao));

        PoliticaClube nivelClube = clube.buscar(nome(solicitacao.nivelClube()))
                .orElseThrow(() -> new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.porNome(nome(solicitacao.regiao()))
                .orElseThrow(() -> new CheckoutException(ErroCheckout.REGIAO_INVALIDA));

        PoliticaEntrega entrega = entregas.buscar(nome(solicitacao.modalidadeEntrega()))
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!entrega.atende(base)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivelClube.isentaFrete()
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(entrega.frete(base));

        BigDecimal descontoCupom = calcularDescontoCupom(solicitacao, base, frete);

        PoliticaPagamento pagamento = pagamentos.buscar(nome(solicitacao.formaPagamento()))
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = solicitacao.parcelas() == null ? 1 : solicitacao.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(base.subtotalProdutos());
        BigDecimal totalPedido = Dinheiro.arredondar(base.subtotalProdutos()
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.arredondar(resultado.totalFinal().subtract(totalPedido));

        return new ResumoCompra(
                base.subtotalProdutos(),
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivelClube.creditoProximaCompra(base.subtotalProdutos()),
                nivelClube.temBrinde(base.subtotalProdutos()));
    }

    private BigDecimal calcularDescontoCupom(SolicitacaoResumo solicitacao, BaseCalculo base, BigDecimal frete) {
        String codigo = nome(solicitacao.cupom());
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        PoliticaCupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(base, frete)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(base, frete));
    }

    private List<ItemPedido> validarItens(SolicitacaoResumo solicitacao) {
        List<SolicitacaoResumo.ItemSolicitado> solicitados = solicitacao.itens();
        if (solicitados == null || solicitados.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>(solicitados.size());
        for (SolicitacaoResumo.ItemSolicitado item : solicitados) {
            if (item == null
                    || !Dinheiro.positivo(item.precoUnitario())
                    || !Dinheiro.positivo(item.pesoKg())
                    || item.quantidade() == null
                    || item.quantidade() <= 0) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    /** Codigos vem sempre em letras maiusculas; ausente e o mesmo que nao informado. */
    private String nome(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
