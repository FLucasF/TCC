package com.loja.checkout.dominio;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadesEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra na ordem combinada com o financeiro: produtos, desconto
 * do cupom, frete, seguro, total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final ModalidadesEntrega modalidades;
    private final NiveisClube niveis;
    private final Cupons cupons;
    private final FormasPagamento formasPagamento;

    public CalculadoraResumo(ModalidadesEntrega modalidades, NiveisClube niveis, Cupons cupons,
            FormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.niveis = niveis;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(DadosCompra dados) {
        List<Item> itens = itensValidados(dados.itens());

        NivelClube nivelClube = niveis.porCodigo(dados.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.porCodigo(dados.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.porCodigo(dados.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        Pedido pedido = new Pedido(itens, modalidade, nivelClube, regiao);
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(modalidade.frete(pedido));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (dados.cupom() != null) {
            Cupom cupom = cupons.porCodigo(dados.cupom())
                    .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
            ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));
        }

        FormaPagamento formaPagamento = formasPagamento.porCodigo(dados.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = dados.parcelas() == null ? 1 : dados.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                Dinheiro.centavos(pagamento.totalFinal()),
                parcelas,
                Dinheiro.centavos(pagamento.valorParcela()),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private List<Item> itensValidados(List<DadosCompra.DadosItem> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::itemValidado).toList();
    }

    private Item itemValidado(DadosCompra.DadosItem dados) {
        if (dados == null || !positivo(dados.precoUnitario()) || !positivo(dados.pesoKg())
                || dados.quantidade() == null || dados.quantidade() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(dados.nome(), dados.precoUnitario(), dados.quantidade(), dados.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
