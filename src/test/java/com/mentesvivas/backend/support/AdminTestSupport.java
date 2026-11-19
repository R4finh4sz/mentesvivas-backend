package com.mentesvivas.backend.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class AdminTestSupport {

    private final ObjectProvider<MockMvc> mockMvcProvider;
    private final CapturedEmailSender emails;

    public AdminTestSupport(ObjectProvider<MockMvc> mockMvcProvider, CapturedEmailSender emails) {
        this.mockMvcProvider = mockMvcProvider;
        this.emails = emails;
    }

    public String token() throws Exception {
        MockMvc mockMvc = mockMvcProvider.getObject();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(TermsTestSupport.ADMIN_EMAIL, TermsTestSupport.ADMIN_PASSWORD)))
                .andExpect(status().isAccepted());
        String body = mockMvc.perform(post("/api/auth/2fa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}"
                                .formatted(TermsTestSupport.ADMIN_EMAIL, emails.lastCode())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
