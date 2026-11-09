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

    public static final String PASSWORD = "senhaForte1";

    private final ObjectProvider<MockMvc> mockMvcProvider;
    private final TermsTestSupport terms;

    public AuthTestSupport(ObjectProvider<MockMvc> mockMvcProvider, TermsTestSupport terms) {
        this.mockMvcProvider = mockMvcProvider;
        this.terms = terms;
    }

    public String registerAndLogin(String email, String cpf, String role) throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        int version = terms.publish();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Pessoa Teste","cpf":"%s","birthDate":"1950-03-10",
                 "phone":"11999998888","email":"%s","password":"%s","passwordConfirmation":"%s",
                 "role":"%s","termsVersion":%d,"acceptTerms":true,"acceptHealthData":true}
                """.formatted(cpf, email, PASSWORD, PASSWORD, role, version))).andExpect(status().isCreated());
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
