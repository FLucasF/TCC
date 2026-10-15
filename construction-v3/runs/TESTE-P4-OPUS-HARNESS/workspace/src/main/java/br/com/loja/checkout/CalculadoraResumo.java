package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.cupom.SemCupom;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroCheckout;
import br.com.loja.checkout.dominio.ItemPedido;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem combinada com o financeiro. */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                      Catalogo<Cupom> cupons,
                      Catalogo<NivelClube> niveisClube,
                      Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoDe(requisicao.itens());
        NivelClube nivel = escolher(niveisClube, requisicao.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Regiao.porNome(requisicao.regiao())
                .orElseThrow(() -> new ErroCheckout(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade =
                escolher(modalidades, requisicao.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        exigir(modalidade.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.frete(Dinheiro.centavos(modalidade.frete(pedido)));

        Cupom cupom = cupomDe(requisicao.cupom());
        exigir(cupom.aplicavel(pedido, frete), CodigoErro.CUPOM_NAO_APLICAVEL);
        BigDecimal descontoCupom = Dinheiro.centavos(cupom.desconto(pedido, frete));

        FormaPagamento forma =
                escolher(formasPagamento, requisicao.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Optional.ofNullable(requisicao.parcelas()).orElse(PARCELAS_PADRAO);
        exigir(forma.permiteParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);

        BigDecimal produtosComDesconto = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom));
        BigDecimal totalAntesDoImposto = Dinheiro.centavos(produtosComDesconto.add(frete));
        exigir(forma.atende(totalAntesDoImposto), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);

        BigDecimal imposto = regiao.imposto(produtosComDesconto);
        BigDecimal totalPedido = Dinheiro.centavos(totalAntesDoImposto.add(imposto));
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                imposto,
                Dinheiro.centavos(cobranca.valorFinal().subtract(totalPedido)),
                Dinheiro.centavos(cobranca.valorFinal()),
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private Pedido pedidoDe(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::itemDe).toList());
    }

    private ItemPedido itemDe(ItemRequest item) {
        exigir(item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Cupom cupomDe(String codigo) {
        return codigo == null
                ? SemCupom.INSTANCIA
                : escolher(cupons, codigo, CodigoErro.CUPOM_INVALIDO);
    }

    private <T extends Codificado> T escolher(Catalogo<T> catalogo, String codigo, CodigoErro erro) {
        return catalogo.buscar(codigo).orElseThrow(() -> new ErroCheckout(erro));
    }

    private void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new ErroCheckout(erro);
        }
    }
}
