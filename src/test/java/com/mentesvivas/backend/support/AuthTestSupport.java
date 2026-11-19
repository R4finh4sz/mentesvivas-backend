package com.mentesvivas.backend.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class AuthTestSupport {

    public static final String PASSWORD = "SenhaForte1";

    private final ObjectProvider<MockMvc> mockMvcProvider;
    private final TermsTestSupport terms;

    public AuthTestSupport(ObjectProvider<MockMvc> mockMvcProvider, TermsTestSupport terms) {
        this.mockMvcProvider = mockMvcProvider;
        this.terms = terms;
    }

    public static final String ADDRESS_JSON = """
            ,"address":{"cep":"01001000","street":"Praca da Se","number":"1","city":"Sao Paulo","state":"SP"}""".strip();

    public static final String CONTACTS_JSON = """
            ,"emergencyContacts":[{"name":"Maria Souza","relationship":"Filha","phone":"11977776666","order":1},
            {"name":"Joao Souza","relationship":"Filho","phone":"11966665555","order":2}]""".strip();

    public static String registrationExtras(String role) {
        return "PATIENT".equals(role) ? ADDRESS_JSON + CONTACTS_JSON : ADDRESS_JSON;
    }

    public String registerAndLogin(String email, String cpf, String role) throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"%s","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"%s","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true%s}
                """.formatted(cpf, email, PASSWORD, PASSWORD, role, version, registrationExtras(role)))).andExpect(status().isCreated());
        return login(email);
    }

    public String login(String email) throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
