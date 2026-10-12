package loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@WebAppConfiguration
class ResumoControllerTest {
    @Autowired
    WebApplicationContext contexto;

    @Test
    void devolveResumoEErro() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        String corpo = """
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                          {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""";
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalFinal\":391.47,\"frete\":33.10,\"brinde\":false}"));
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content(corpo.replace("NORTE", "MARTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"REGIAO_INVALIDA\"}"));
    }
}
