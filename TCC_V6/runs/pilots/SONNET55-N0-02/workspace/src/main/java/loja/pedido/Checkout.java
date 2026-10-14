package loja.pedido;

import static loja.dominio.NegocioException.invalido;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import loja.api.PedidoRequest;
import loja.clube.NivelClube;
import loja.cupom.Cupom;
import loja.cupom.Cupom.ContextoCupom;
import loja.dominio.Dinheiro;
import loja.dominio.LinhaItem;
import loja.dominio.Produto;
import loja.dominio.Regiao;
import loja.dominio.Resumo;
import loja.entrega.ModalidadeEntrega;
import loja.entrega.ModalidadeEntrega.Cotacao;
import loja.entrega.ModalidadeEntrega.ContextoEntrega;
import loja.infra.PedidoRepository;
import loja.infra.ProdutoRepository;
import loja.pagamento.FormaPagamento;
import loja.pagamento.FormaPagamento.Cobranca;

/** Única fonte do cálculo: serve ao resumo e ao registro do pedido, então os valores nunca divergem. */
@Component
public class Checkout {

    private static final java.util.regex.Pattern CEP = java.util.regex.Pattern.compile("\\d{8}");

    private final ProdutoRepository produtos;
    private final PedidoRepository pedidos;
    private final Map<String, ModalidadeEntrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public Checkout(ProdutoRepository produtos, PedidoRepository pedidos, List<ModalidadeEntrega> entregas,
            List<Cupom> cupons, List<NivelClube> niveis, List<FormaPagamento> pagamentos) {
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.entregas = indexar(entregas, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.pagamentos = indexar(pagamentos, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        return lista.stream().collect(Collectors.toMap(chave, Function.identity()));
    }

    public Resumo calcular(PedidoRequest req) {
        if (req == null) {
            throw invalido("PEDIDO_INVALIDO");
        }
        // 1. formato básico
        validarBasico(req);
        // 2-4. catálogo
        Map<String, Produto> catalogo = produtos.buscarVarios(
                req.itens().stream().map(PedidoRequest.Item::sku).toList());
        for (var item : req.itens()) {
            if (!catalogo.containsKey(item.sku())) {
                throw invalido("PRODUTO_INEXISTENTE");
            }
        }
        for (var item : req.itens()) {
            if (catalogo.get(item.sku()).foraDeLinha()) {
                throw invalido("PRODUTO_FORA_DE_LINHA");
            }
        }
        for (var item : req.itens()) {
            if (catalogo.get(item.sku()).estoque() < item.quantidade()) {
                throw invalido("ESTOQUE_INSUFICIENTE");
            }
        }
        // 5-8
        NivelClube nivel = req.nivelClube() == null ? null : niveis.get(req.nivelClube());
        if (nivel == null) {
            throw invalido("NIVEL_CLUBE_INVALIDO");
        }
        Regiao regiao = Regiao.de(req.regiao()).orElseThrow(() -> invalido("REGIAO_INVALIDA"));
        if (req.cep() == null || !CEP.matcher(req.cep()).matches()) {
            throw invalido("CEP_INVALIDO");
        }
        ModalidadeEntrega entrega = req.modalidadeEntrega() == null ? null : entregas.get(req.modalidadeEntrega());
        if (entrega == null) {
            throw invalido("MODALIDADE_INVALIDA");
        }

        List<LinhaItem> linhas = req.itens().stream().map(i -> {
            Produto p = catalogo.get(i.sku());
            return new LinhaItem(p.sku(), p.preco(), p.pesoKg(), i.quantidade());
        }).toList();
        BigDecimal subtotal = Dinheiro.arredondar(
                linhas.stream().map(LinhaItem::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal peso = linhas.stream().map(l -> l.pesoKg().multiply(BigDecimal.valueOf(l.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 9-10. entrega (pode falhar por indisponibilidade ou transportadora fora do ar)
        Cotacao cotacao = entrega.cotar(new ContextoEntrega(req.cep(), regiao, peso));
        BigDecimal frete = Dinheiro.arredondar(cotacao.valor());
        if (nivel.isentaFrete() && entrega.aceitaIsencaoDoClube()) {
            frete = Dinheiro.ZERO;
        }

        // 11-12. cupom
        boolean comCupom = req.cupom() != null && !req.cupom().isBlank();
        BigDecimal desconto = Dinheiro.ZERO;
        if (comCupom) {
            Cupom cupom = cupons.get(req.cupom());
            if (cupom == null) {
                throw invalido("CUPOM_INVALIDO");
            }
            var ctx = new ContextoCupom(linhas, subtotal, frete, entrega.temFrete(),
                    () -> !pedidos.cpfTemPedidoPago(req.cliente().cpf()));
            desconto = Dinheiro.arredondar(
                    cupom.desconto(ctx).orElseThrow(() -> invalido("CUPOM_NAO_APLICAVEL")));
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete).add(seguro));

        // 13-15. pagamento
        FormaPagamento pagamento = req.formaPagamento() == null ? null : pagamentos.get(req.formaPagamento());
        if (pagamento == null) {
            throw invalido("FORMA_PAGAMENTO_INVALIDA");
        }
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        pagamento.validarParcelas(parcelas);
        pagamento.validarDisponibilidade(total);
        Cobranca cobranca = pagamento.cobrar(total, parcelas, comCupom);

        return new Resumo(subtotal, desconto, frete, cotacao.prazoDias(), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(), parcelas, cobranca.valorParcela(),
                nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private static void validarBasico(PedidoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw invalido("PEDIDO_INVALIDO");
        }
        Set<String> vistos = new HashSet<>();
        for (var item : req.itens()) {
            if (item == null || item.sku() == null || item.sku().isBlank() || item.quantidade() == null
                    || item.quantidade() <= 0 || !vistos.add(item.sku())) {
                throw invalido("PEDIDO_INVALIDO");
            }
        }
        var c = req.cliente();
        if (c == null || c.cpf() == null || c.cpf().isBlank() || c.email() == null || c.email().isBlank()) {
            throw invalido("PEDIDO_INVALIDO");
        }
    }
}
