package br.com.loja.checkout.calculo;

import br.com.loja.checkout.api.ItemRequisicao;
import br.com.loja.checkout.api.ResumoRequisicao;
import br.com.loja.checkout.api.ResumoResposta;
import br.com.loja.checkout.clube.CatalogoClube;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.CatalogoCupons;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroCheckoutException;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.CatalogoEntregas;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.CatalogoPagamentos;
import br.com.loja.checkout.pagamento.ContextoPagamento;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem combinada com o financeiro. */
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

    public ResumoResposta calcular(ResumoRequisicao requisicao) {
        if (requisicao == null) {
            throw new ErroCheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        Pedido pedido = pedidoValidado(requisicao.itens());

        NivelClube nivelClube = clube.porCodigo(requisicao.nivelClube())
                .orElseThrow(() -> new ErroCheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.porCodigo(requisicao.regiao())
                .orElseThrow(() -> new ErroCheckoutException(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = entregas.porCodigo(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ErroCheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(modalidade.custo(pedido));

        BigDecimal descontoCupom = descontoDoCupom(requisicao.cupom(), pedido, subtotalProdutos, frete);

        FormaPagamento formaPagamento = pagamentos.porCodigo(requisicao.formaPagamento())
                .orElseThrow(() -> new ErroCheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new ErroCheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalSemImposto = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete));
        BigDecimal imposto = regiao.imposto(Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom)));
        BigDecimal totalPedido = Dinheiro.centavos(totalSemImposto.add(imposto));

        ContextoPagamento contextoPagamento =
                new ContextoPagamento(totalPedido, totalSemImposto, parcelas);
        if (!formaPagamento.atende(contextoPagamento)) {
            throw new ErroCheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.aplicar(contextoPagamento);
        BigDecimal ajustePagamento = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResposta(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(pedido),
                imposto,
                ajustePagamento,
                Dinheiro.centavos(resultado.totalFinal()),
                parcelas,
                Dinheiro.centavos(resultado.valorParcela()),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private BigDecimal descontoDoCupom(String codigo, Pedido pedido,
                                       BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.porCodigo(codigo)
                .orElseThrow(() -> new ErroCheckoutException(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroCheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private Pedido pedidoValidado(List<ItemRequisicao> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> validados = new ArrayList<>(itens.size());
        for (ItemRequisicao item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new ErroCheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            validados.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(validados);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
