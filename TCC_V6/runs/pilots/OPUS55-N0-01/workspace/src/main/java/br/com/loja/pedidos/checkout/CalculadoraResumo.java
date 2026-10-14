package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.catalogo.Produto;
import br.com.loja.pedidos.catalogo.ProdutoRepositorio;
import br.com.loja.pedidos.clube.NiveisClube;
import br.com.loja.pedidos.clube.NivelClube;
import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.cupom.ContextoCupom;
import br.com.loja.pedidos.cupom.Cupom;
import br.com.loja.pedidos.cupom.Cupons;
import br.com.loja.pedidos.entrega.ContextoEntrega;
import br.com.loja.pedidos.entrega.CotacaoEntrega;
import br.com.loja.pedidos.entrega.ModalidadeEntrega;
import br.com.loja.pedidos.entrega.ModalidadesEntrega;
import br.com.loja.pedidos.pagamento.FormaPagamento;
import br.com.loja.pedidos.pagamento.FormasPagamento;
import br.com.loja.pedidos.pagamento.ResultadoPagamento;
import tools.jackson.databind.JsonNode;

/**
 * Confere os dados da compra e calcula o resumo. É o único lugar onde o resumo é calculado: o resumo mostrado antes
 * de confirmar e o do pedido registrado saem daqui.
 */
@Component
public class CalculadoraResumo {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CEP = Pattern.compile("\\d{8}");

    private final ProdutoRepositorio produtos;
    private final NiveisClube niveis;
    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final FormasPagamento formas;

    public CalculadoraResumo(ProdutoRepositorio produtos, NiveisClube niveis, ModalidadesEntrega modalidades,
            Cupons cupons, FormasPagamento formas) {
        this.produtos = produtos;
        this.niveis = niveis;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formas = formas;
    }

    public Checkout calcular(JsonNode dados) {
        if (dados == null || !dados.isObject()) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
        JsonNode cliente = dados.get("cliente");
        String cpf = cliente != null && cliente.isObject() ? texto(cliente, "cpf") : null;
        String email = cliente != null && cliente.isObject() ? texto(cliente, "email") : null;
        List<ItemSolicitado> solicitados = lerItens(dados.get("itens"));
        if (cpf == null || !CPF.matcher(cpf).matches() || email == null || email.isBlank()) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }

        List<ItemCarrinho> itens = conferirCatalogo(solicitados);
        BigDecimal subtotal = Dinheiro.centavos(itens.stream().map(ItemCarrinho::valor).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal peso = itens.stream().map(ItemCarrinho::peso).reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivel = niveis.buscar(texto(dados, "nivelClube"))
                .orElseThrow(() -> ErroNegocio.invalido("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Optional.ofNullable(texto(dados, "regiao")).flatMap(Regiao::deCodigo)
                .orElseThrow(() -> ErroNegocio.invalido("REGIAO_INVALIDA"));
        String cep = texto(dados, "cep");
        if (cep == null || !CEP.matcher(cep).matches()) {
            throw ErroNegocio.invalido("CEP_INVALIDO");
        }
        ModalidadeEntrega modalidade = modalidades.buscar(texto(dados, "modalidadeEntrega"))
                .orElseThrow(() -> ErroNegocio.invalido("MODALIDADE_INVALIDA"));
        CotacaoEntrega cotacao = modalidade.cotar(new ContextoEntrega(cep, regiao, peso, subtotal));
        BigDecimal frete = nivel.isentaFrete(modalidade) ? Dinheiro.ZERO : Dinheiro.centavos(cotacao.frete());

        Optional<Cupom> cupom = lerCupom(dados);
        BigDecimal descontoCupom = cupom
                .map(c -> Dinheiro.centavos(c.desconto(new ContextoCupom(cpf, itens, subtotal, modalidade, frete))))
                .orElse(Dinheiro.ZERO);

        FormaPagamento forma = formas.buscar(texto(dados, "formaPagamento"))
                .orElseThrow(() -> ErroNegocio.invalido("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = lerParcelas(dados.get("parcelas"));
        if (!forma.aceitaParcelas(parcelas)) {
            throw ErroNegocio.invalido("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);
        if (!forma.atende(totalPedido)) {
            throw ErroNegocio.invalido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas, cupom.isPresent());
        BigDecimal totalFinal = Dinheiro.centavos(pagamento.totalFinal());

        Resumo resumo = new Resumo(subtotal, descontoCupom, frete, cotacao.prazoDias(), seguro,
                totalFinal.subtract(totalPedido), totalFinal, pagamento.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()), Dinheiro.centavos(nivel.credito(subtotal)),
                nivel.ganhaBrinde(subtotal));
        return new Checkout(cpf, email, itens, resumo);
    }

    private record ItemSolicitado(String sku, int quantidade) {
    }

    private static List<ItemSolicitado> lerItens(JsonNode itens) {
        if (itens == null || !itens.isArray() || itens.isEmpty()) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
        List<ItemSolicitado> lidos = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        for (JsonNode item : itens.values()) {
            String sku = item.isObject() ? texto(item, "sku") : null;
            JsonNode quantidade = item.isObject() ? item.get("quantidade") : null;
            if (sku == null || sku.isBlank() || !skus.add(sku) || quantidade == null || !quantidade.isIntegralNumber()
                    || !quantidade.canConvertToInt() || quantidade.intValue() <= 0) {
                throw ErroNegocio.invalido("PEDIDO_INVALIDO");
            }
            lidos.add(new ItemSolicitado(sku, quantidade.intValue()));
        }
        return lidos;
    }

    private List<ItemCarrinho> conferirCatalogo(List<ItemSolicitado> solicitados) {
        List<ItemCarrinho> itens = new ArrayList<>();
        for (ItemSolicitado solicitado : solicitados) {
            Produto produto = produtos.buscar(solicitado.sku())
                    .orElseThrow(() -> ErroNegocio.invalido("PRODUTO_INEXISTENTE"));
            itens.add(new ItemCarrinho(produto, solicitado.quantidade()));
        }
        if (itens.stream().anyMatch(item -> item.produto().foraDeLinha())) {
            throw ErroNegocio.invalido("PRODUTO_FORA_DE_LINHA");
        }
        if (itens.stream().anyMatch(item -> item.produto().estoque() < item.quantidade())) {
            throw ErroNegocio.invalido("ESTOQUE_INSUFICIENTE");
        }
        return itens;
    }

    private Optional<Cupom> lerCupom(JsonNode dados) {
        JsonNode codigo = dados.get("cupom");
        if (codigo == null || codigo.isNull() || (codigo.isString() && codigo.stringValue().isEmpty())) {
            return Optional.empty();
        }
        if (!codigo.isString()) {
            throw ErroNegocio.invalido("CUPOM_INVALIDO");
        }
        return Optional.of(cupons.buscar(codigo.stringValue()).orElseThrow(() -> ErroNegocio.invalido("CUPOM_INVALIDO")));
    }

    private static int lerParcelas(JsonNode parcelas) {
        if (parcelas == null || parcelas.isNull()) {
            return 1;
        }
        if (!parcelas.isIntegralNumber() || !parcelas.canConvertToInt()) {
            throw ErroNegocio.invalido("PARCELAMENTO_INVALIDO");
        }
        return parcelas.intValue();
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.get(campo);
        return valor != null && valor.isString() ? valor.stringValue() : null;
    }
}
