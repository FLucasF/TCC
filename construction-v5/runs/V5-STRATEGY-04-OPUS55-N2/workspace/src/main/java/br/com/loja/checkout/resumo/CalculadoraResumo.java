package br.com.loja.checkout.resumo;

import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PARCELAMENTO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PEDIDO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.REGIAO_INVALIDA;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra, validando o pedido na ordem definida pelo negócio. */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = obrigatorio(niveis.buscar(pedido.nivelClube()), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = obrigatorio(Regiao.buscar(pedido.regiao()), REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = obrigatorio(modalidades.buscar(pedido.modalidadeEntrega()), MODALIDADE_INVALIDA);
        exigir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = descontoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(carrinho);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento pagamento = obrigatorio(formasPagamento.buscar(pedido.formaPagamento()), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(pagamento.aceitaParcelas(parcelas), PARCELAMENTO_INVALIDO);
        exigir(pagamento.atende(totalPedido), FORMA_PAGAMENTO_INDISPONIVEL);
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(cobranca.totalFinal()),
                cobranca.parcelas(),
                Dinheiro.centavos(cobranca.valorParcela()),
                nivel.credito(carrinho),
                nivel.brinde(carrinho));
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = obrigatorio(cupons.buscar(codigo), CUPOM_INVALIDO);
        exigir(cupom.aplicavel(carrinho, frete), CUPOM_NAO_APLICAVEL);
        return cupom.desconto(carrinho, frete);
    }

    private static Carrinho carrinho(List<ResumoRequest.ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), PEDIDO_INVALIDO);
        return new Carrinho(itens.stream().map(CalculadoraResumo::item).toList());
    }

    private static Item item(ResumoRequest.ItemRequest item) {
        exigir(item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg()), PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static <T> T obrigatorio(Optional<T> opcao, CodigoErro erro) {
        return opcao.orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    private static void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
