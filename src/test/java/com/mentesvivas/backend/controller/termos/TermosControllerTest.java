package com.mentesvivas.backend.controller.termos;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.TermosTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TermosControllerTest {

    private static final String SENHA = "senhaForte1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermosTestSupport termos;

    @Test
    void semTermosPublicadosRetorna404ECadastroNaoConclui() throws Exception {
        mockMvc.perform(get("/api/terms")).andExpect(status().isNotFound());
        registrar("ana@exemplo.com", 1).andExpect(status().isNotFound());
    }

    @Test
    void adminPublicaNovaVersaoETermosAtuaisSaoPublicos() throws Exception {
        String token = tokenDe(TermosTestSupport.ADMIN_EMAIL, TermosTestSupport.ADMIN_SENHA);

        publicar(token, "Termos v1").andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(1));
        publicar(token, "Termos v2").andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(2));

        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.titulo").value("Termos v2"));
        mockMvc.perform(get("/api/terms/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void naoAdminNaoPublicaTermos() throws Exception {
        int versao = termos.publicar();
        registrar("bia@exemplo.com", versao).andExpect(status().isCreated());

        publicar(tokenDe("bia@exemplo.com", SENHA), "Termos falsos").andExpect(status().isForbidden());
        publicar(null, "Termos falsos").andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroComVersaoDesatualizadaRetornaConflito() throws Exception {
        int antiga = termos.publicar();
        termos.publicar();

        registrar("carla@exemplo.com", antiga).andExpect(status().isConflict());
    }

    @Test
    void cadastroRegistraAceiteENovaVersaoExigeNovoAceite() throws Exception {
        int v1 = termos.publicar();
        registrar("dani@exemplo.com", v1).andExpect(status().isCreated());
        String token = tokenDe("dani@exemplo.com", SENHA);

        mockMvc.perform(get("/api/terms/accepted").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].versao").value(v1))
                .andExpect(jsonPath("$[0].aceitoEm").exists());

        int v2 = termos.publicar();
        login("dani@exemplo.com").andExpect(jsonPath("$.termosAceitos").value(false));

        aceitar(token, v1).andExpect(status().isConflict());
        aceitar(token, v2).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(v2));

        login("dani@exemplo.com").andExpect(jsonPath("$.termosAceitos").value(true));
        mockMvc.perform(get("/api/terms/accepted").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void aceiteExigeConfirmacao() throws Exception {
        int versao = termos.publicar();
        registrar("edu@exemplo.com", versao).andExpect(status().isCreated());

        mockMvc.perform(post("/api/terms/accept").header("Authorization", "Bearer " + tokenDe("edu@exemplo.com", SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versao\":%d,\"aceiteTermos\":false}".formatted(versao)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions registrar(String email, int versaoTermos) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nomeCompleto":"Pessoa Teste","cpf":"52998224725","dataNascimento":"1950-03-10",
                 "telefone":"11999998888","email":"%s","senha":"%s","confirmacaoSenha":"%s",
                 "role":"CUIDADOR","versaoTermos":%d,"aceiteTermos":true,"aceiteDadosSaude":true}
                """.formatted(email, SENHA, SENHA, versaoTermos)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)));
    }

    private String tokenDe(String email, String senha) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions publicar(String token, String titulo) throws Exception {
        var request = post("/api/terms").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"%s\",\"conteudo\":\"Texto dos termos\"}".formatted(titulo));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private ResultActions aceitar(String token, int versao) throws Exception {
        return mockMvc.perform(post("/api/terms/accept").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,\"aceiteTermos\":true}".formatted(versao)));
    }
}
