package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.catalogo.Produto;
import br.com.loja.pedidos.catalogo.ProdutoRepositorio;
import br.com.loja.pedidos.checkout.clube.NiveisClube;
import br.com.loja.pedidos.checkout.clube.NivelClube;
import br.com.loja.pedidos.checkout.cupom.Cupons;
import br.com.loja.pedidos.checkout.cupom.PedidoParaCupom;
import br.com.loja.pedidos.checkout.entrega.CotacaoEntrega;
import br.com.loja.pedidos.checkout.entrega.ModalidadeEntrega;
import br.com.loja.pedidos.checkout.entrega.ModalidadesEntrega;
import br.com.loja.pedidos.checkout.entrega.PedidoParaEntrega;
import br.com.loja.pedidos.checkout.pagamento.CondicaoPagamento;
import br.com.loja.pedidos.checkout.pagamento.FormaPagamento;
import br.com.loja.pedidos.checkout.pagamento.FormasPagamento;
import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.comum.ErroNegocio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * O único lugar onde o resumo da compra é calculado: tanto o resumo mostrado antes de confirmar
 * quanto o resumo gravado no pedido passam por aqui, então os valores são sempre os mesmos.
 */
@Component
public class CalculadoraCheckout {

    private static final Pattern CEP = Pattern.compile("\\d{8}");

    private final ProdutoRepositorio produtos;
    private final NiveisClube niveis;
    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final FormasPagamento formasPagamento;

    public CalculadoraCheckout(ProdutoRepositorio produtos, NiveisClube niveis, ModalidadesEntrega modalidades,
                               Cupons cupons, FormasPagamento formasPagamento) {
        this.produtos = produtos;
        this.niveis = niveis;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public Checkout calcular(DadosCompra dados) {
        List<ItemPrecificado> itens = precificar(dados.itens());
        NivelClube nivel = niveis.buscar(dados.nivelClube()).orElseThrow(() -> recusa("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.de(dados.regiao()).orElseThrow(() -> recusa("REGIAO_INVALIDA"));
        if (dados.cep() == null || !CEP.matcher(dados.cep()).matches()) {
            throw recusa("CEP_INVALIDO");
        }
        ModalidadeEntrega modalidade = modalidades.buscar(dados.modalidadeEntrega())
                .orElseThrow(() -> recusa("MODALIDADE_INVALIDA"));

        // 1. Produtos
        BigDecimal subtotal = Dinheiro.arredondar(itens.stream().map(ItemPrecificado::valor).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoKg = itens.stream().map(ItemPrecificado::pesoKg).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Frete (cotado antes do cupom, porque o FRETEGRATIS depende dele)
        CotacaoEntrega entrega = modalidade.cotar(new PedidoParaEntrega(dados.cep(), regiao, pesoKg, subtotal));
        BigDecimal frete = nivel.freteGratis(modalidade) ? Dinheiro.ZERO : Dinheiro.arredondar(entrega.frete());

        // 2. Cupom
        boolean usouCupom = dados.cupom() != null;
        BigDecimal desconto = usouCupom
                ? cupons.desconto(dados.cupom(), new PedidoParaCupom(dados.cpf(), itens, subtotal, modalidade, frete))
                : Dinheiro.ZERO;

        // 4. Seguro e 5. total do pedido
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete).add(seguro));

        // 6. Forma de pagamento
        FormaPagamento forma = formasPagamento.buscar(dados.formaPagamento())
                .orElseThrow(() -> recusa("FORMA_PAGAMENTO_INVALIDA"));
        if (dados.parcelas() == null || !forma.aceitaParcelas(dados.parcelas())) {
            throw recusa("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivelPara(totalPedido)) {
            throw recusa("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        CondicaoPagamento pagamento = forma.condicao(totalPedido, dados.parcelas(), usouCupom);
        BigDecimal totalFinal = Dinheiro.arredondar(pagamento.totalFinal());

        Resumo resumo = new Resumo(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                totalFinal.subtract(totalPedido),
                totalFinal,
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()),
                Dinheiro.arredondar(nivel.credito(subtotal)),
                nivel.brinde(subtotal));
        return new Checkout(dados, itens, resumo);
    }

    /** Confere os produtos do carrinho no catálogo, na ordem: existe, está em linha, tem estoque. */
    private List<ItemPrecificado> precificar(List<ItemCompra> carrinho) {
        Map<String, Produto> catalogo = produtos.buscarVarios(carrinho.stream().map(ItemCompra::sku).toList());
        if (carrinho.stream().anyMatch(item -> !catalogo.containsKey(item.sku()))) {
            throw recusa("PRODUTO_INEXISTENTE");
        }
        if (carrinho.stream().anyMatch(item -> catalogo.get(item.sku()).foraDeLinha())) {
            throw recusa("PRODUTO_FORA_DE_LINHA");
        }
        if (carrinho.stream().anyMatch(item -> catalogo.get(item.sku()).estoque() < item.quantidade())) {
            throw recusa("ESTOQUE_INSUFICIENTE");
        }
        return carrinho.stream()
                .map(item -> {
                    Produto produto = catalogo.get(item.sku());
                    return new ItemPrecificado(item.sku(), item.quantidade(), produto.preco(), produto.pesoKg());
                })
                .toList();
    }

    private static ErroNegocio recusa(String codigo) {
        return ErroNegocio.naoProcessavel(codigo);
    }
}
