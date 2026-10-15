package com.loja.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Calcula o resumo da compra. É a única fonte dos valores, tanto do resumo quanto do pedido registrado. */
@Component
public class CalculadoraResumo {

    public record Calculo(Resumo resumo, List<Item> itens) {
    }

    private record Disponivel(String sku, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {
    }

    private final Armazem armazem;
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formas;

    public CalculadoraResumo(Armazem armazem, List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                             List<NivelClube> niveis, List<FormaPagamento> formas) {
        this.armazem = armazem;
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.formas = indexar(formas, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> codigo) {
        return lista.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Calculo calcular(DadosCompra dados) {
        // 1. estrutura do pedido
        if (dados == null || dados.cliente() == null || vazio(dados.cliente().cpf())
                || vazio(dados.cliente().email()) || dados.itens() == null || dados.itens().isEmpty()) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
        String cpf = dados.cliente().cpf();
        Set<String> vistos = new HashSet<>();
        List<Item> itens = new ArrayList<>();
        for (DadosCompra.ItemDados item : dados.itens()) {
            if (item == null || vazio(item.sku()) || !vistos.add(item.sku())
                    || item.quantidade() == null || item.quantidade() <= 0) {
                throw ErroNegocio.invalido("PEDIDO_INVALIDO");
            }
            itens.add(new Item(item.sku(), item.quantidade()));
        }

        // 2, 3 e 4. produtos
        Map<String, Disponivel> catalogo = armazem.ler(estado -> {
            Map<String, Disponivel> copia = new java.util.HashMap<>();
            for (Item item : itens) {
                Produto p = estado.produtos.get(item.sku());
                if (p != null) {
                    copia.put(p.sku(), new Disponivel(p.sku(), p.preco(), p.pesoKg(), p.estoque(), p.foraDeLinha()));
                }
            }
            return copia;
        });
        for (Item item : itens) {
            if (!catalogo.containsKey(item.sku())) {
                throw ErroNegocio.invalido("PRODUTO_INEXISTENTE");
            }
        }
        for (Item item : itens) {
            if (catalogo.get(item.sku()).foraDeLinha()) {
                throw ErroNegocio.invalido("PRODUTO_FORA_DE_LINHA");
            }
        }
        for (Item item : itens) {
            if (catalogo.get(item.sku()).estoque() < item.quantidade()) {
                throw ErroNegocio.invalido("ESTOQUE_INSUFICIENTE");
            }
        }

        // 5 a 8. nível, região, CEP e opção de entrega
        NivelClube nivel = dados.nivelClube() == null ? null : niveis.get(dados.nivelClube());
        if (nivel == null) {
            throw ErroNegocio.invalido("NIVEL_CLUBE_INVALIDO");
        }
        Regiao regiao = Regiao.de(dados.regiao());
        if (regiao == null) {
            throw ErroNegocio.invalido("REGIAO_INVALIDA");
        }
        if (dados.cep() == null || !dados.cep().matches("[0-9]{8}")) {
            throw ErroNegocio.invalido("CEP_INVALIDO");
        }
        ModalidadeEntrega modalidade = dados.modalidadeEntrega() == null ? null
                : modalidades.get(dados.modalidadeEntrega());
        if (modalidade == null) {
            throw ErroNegocio.invalido("MODALIDADE_INVALIDA");
        }

        BigDecimal subtotal = Dinheiro.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        List<Cupom.Linha> linhas = new ArrayList<>();
        for (Item item : itens) {
            Disponivel p = catalogo.get(item.sku());
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(p.preco().multiply(quantidade));
            peso = peso.add(p.pesoKg().multiply(quantidade));
            linhas.add(new Cupom.Linha(p.sku(), p.preco(), item.quantidade()));
        }
        subtotal = Dinheiro.arredondar(subtotal);

        // 9 e 10. frete
        ModalidadeEntrega.Cotacao cotacao = modalidade.cotar(new ModalidadeEntrega.Contexto(dados.cep(), peso, regiao));
        BigDecimal frete = nivel.freteGratis() && modalidade.aceitaFreteGratisDoClube()
                ? Dinheiro.ZERO : cotacao.frete();

        // 11 e 12. cupom
        BigDecimal descontoCupom = Dinheiro.ZERO;
        boolean usouCupom = !vazio(dados.cupom());
        if (usouCupom) {
            Cupom cupom = cupons.get(dados.cupom());
            if (cupom == null) {
                throw ErroNegocio.invalido("CUPOM_INVALIDO");
            }
            Cupom.Contexto contexto = new Cupom.Contexto(linhas, subtotal, frete, modalidade.codigo(),
                    () -> primeiraCompra(cpf));
            Optional<BigDecimal> desconto = cupom.desconto(contexto);
            descontoCupom = Dinheiro.arredondar(desconto
                    .orElseThrow(() -> ErroNegocio.invalido("CUPOM_NAO_APLICAVEL")));
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = Dinheiro.arredondar(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 13 a 15. pagamento
        FormaPagamento forma = dados.formaPagamento() == null ? null : formas.get(dados.formaPagamento());
        if (forma == null) {
            throw ErroNegocio.invalido("FORMA_PAGAMENTO_INVALIDA");
        }
        int parcelas = dados.parcelas() == null ? 1 : dados.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw ErroNegocio.invalido("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivel(total)) {
            throw ErroNegocio.invalido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        FormaPagamento.Resultado pagamento = forma.calcular(total, parcelas, usouCupom);

        Resumo resumo = new Resumo(
                subtotal,
                descontoCupom,
                frete,
                cotacao.prazoDias(),
                seguro,
                Dinheiro.arredondar(pagamento.totalFinal().subtract(total)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                Dinheiro.porcentagem(subtotal, nivel.taxaCredito()),
                nivel.brinde(subtotal));
        return new Calculo(resumo, itens);
    }

    private boolean primeiraCompra(String cpf) {
        return armazem.ler(estado -> estado.pedidosDoCliente(cpf).stream().noneMatch(p ->
                p.situacao() == SituacaoPedido.PAGO || p.situacao() == SituacaoPedido.ENVIADO
                        || p.situacao() == SituacaoPedido.ENTREGUE));
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
