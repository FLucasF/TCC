package br.com.loja.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base dos testes: sobe o serviço com um banco H2 em memória (no modo compatível com PostgreSQL)
 * e uma TransLog falsa, que responde o que cada teste definir.
 */
@SpringBootTest
public abstract class TesteIntegracao {

    /** Resposta da TransLog falsa: situação HTTP e corpo. */
    public record RespostaTransLog(int status, String corpo, long atrasoMs) {

        public static RespostaTransLog cotacao(long centavos, long horas) {
            return new RespostaTransLog(200, "{\"price_cents\": " + centavos + ", \"eta_hours\": " + horas + "}", 0);
        }
    }

    protected static final List<URI> CONSULTAS_TRANSLOG = new CopyOnWriteArrayList<>();
    protected static volatile Function<URI, RespostaTransLog> transLog;

    private static final HttpServer SERVIDOR_TRANSLOG = iniciarTransLog();

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private DataSource dataSource;

    @Autowired
    protected JdbcClient jdbc;

    protected MockMvc mvc;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("DB_URL", () -> "jdbc:h2:mem:loja;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        registro.add("DB_USER", () -> "sa");
        registro.add("translog.url", () -> "http://localhost:" + SERVIDOR_TRANSLOG.getAddress().getPort() + "/");
        registro.add("translog.tempo-limite", () -> "500ms");
    }

    @BeforeEach
    void prepararBase() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        jdbc.sql("DELETE FROM aviso").update();
        jdbc.sql("DELETE FROM pedido_item").update();
        jdbc.sql("DELETE FROM pedido").update();
        jdbc.sql("DELETE FROM cliente").update();
        jdbc.sql("DELETE FROM produto").update();
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        CONSULTAS_TRANSLOG.clear();
        transLog = uri -> RespostaTransLog.cotacao(3310, 41);
    }

    private static HttpServer iniciarTransLog() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "translog-falsa");
                t.setDaemon(true);
                return t;
            }));
            servidor.createContext("/", troca -> {
                URI uri = troca.getRequestURI();
                CONSULTAS_TRANSLOG.add(uri);
                RespostaTransLog resposta = transLog.apply(uri);
                try {
                    Thread.sleep(resposta.atrasoMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                byte[] corpo = resposta.corpo().getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                try {
                    troca.sendResponseHeaders(resposta.status(), corpo.length);
                    troca.getResponseBody().write(corpo);
                } catch (IOException e) {
                    // o cliente desistiu (tempo limite)
                }
                troca.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- ajudantes ----

    /** Dados de compra válidos (retirada na loja, Pix, BRONZE, Sudeste), para ajustar em cada teste. */
    protected static Compra compra(String cpf, String... skuEQuantidade) {
        return new Compra(cpf, skuEQuantidade);
    }

    protected MvcResult postar(String caminho, String json) throws Exception {
        return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    protected MvcResult postar(String caminho) throws Exception {
        return mvc.perform(post(caminho)).andReturn();
    }

    protected MvcResult consultar(String caminho) throws Exception {
        return mvc.perform(get(caminho)).andReturn();
    }

    protected static <T> T ler(MvcResult resultado, String caminho) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), caminho);
    }

    protected static String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected long registrar(Compra compra) throws Exception {
        MvcResult resultado = postar("/pedidos", compra.json());
        if (resultado.getResponse().getStatus() != 201) {
            throw new AssertionError("Pedido não registrado: " + corpo(resultado));
        }
        return ((Number) ler(resultado, "$.id")).longValue();
    }

    protected MvcResult pagar(long id, String idTransacao, String pagoEm) throws Exception {
        return postar("/pedidos/" + id + "/pagamento", "{\"idTransacao\": \"" + idTransacao + "\", \"pagoEm\": \"" + pagoEm + "\"}");
    }

    protected MvcResult pagarAgora(long id, String idTransacao) throws Exception {
        String criadoEm = ler(consultar("/pedidos/" + id), "$.criadoEm");
        return pagar(id, idTransacao, criadoEm);
    }

    protected int estoque(String sku) throws Exception {
        return ler(consultar("/produtos/" + sku), "$.estoque");
    }

    protected Map<String, Object> mapa(MvcResult resultado, String caminho) throws Exception {
        return ler(resultado, caminho);
    }

    /** Construtor dos dados de compra em JSON. */
    public static final class Compra {
        private final String cpf;
        private final String[] itens;
        String cep = "01310100";
        String regiao = "SUDESTE";
        String modalidade = "RETIRADA_LOJA";
        String cupom;
        String forma = "PIX";
        Integer parcelas;
        String nivel = "BRONZE";

        Compra(String cpf, String... itens) {
            this.cpf = cpf;
            this.itens = itens;
        }

        public Compra cep(String v) { cep = v; return this; }
        public Compra regiao(String v) { regiao = v; return this; }
        public Compra entrega(String v) { modalidade = v; return this; }
        public Compra cupom(String v) { cupom = v; return this; }
        public Compra pagamento(String v) { forma = v; return this; }
        public Compra parcelas(int v) { parcelas = v; return this; }
        public Compra nivel(String v) { nivel = v; return this; }

        public String json() {
            StringBuilder itensJson = new StringBuilder();
            for (int i = 0; i < itens.length; i += 2) {
                if (i > 0) {
                    itensJson.append(", ");
                }
                itensJson.append("{\"sku\": \"").append(itens[i]).append("\", \"quantidade\": ").append(itens[i + 1]).append("}");
            }
            return "{"
                    + "\"cliente\": {\"cpf\": \"" + cpf + "\", \"email\": \"cliente@exemplo.com\"},"
                    + "\"itens\": [" + itensJson + "],"
                    + texto("cep", cep) + texto("regiao", regiao) + texto("modalidadeEntrega", modalidade)
                    + texto("cupom", cupom) + texto("formaPagamento", forma) + texto("nivelClube", nivel)
                    + (parcelas == null ? "" : "\"parcelas\": " + parcelas + ",")
                    + "\"fim\": true}";
        }

        private static String texto(String campo, String valor) {
            return valor == null ? "" : "\"" + campo + "\": \"" + valor + "\",";
        }
    }
}
