package com.greenink.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void guestLoginPremiumAndProgressFlow() throws Exception {
        mvc.perform(get("/api/v1/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("u1"))
                .andExpect(jsonPath("$[0].chapters.length()").value(35));

        mvc.perform(get("/api/v1/chapters/u1-c1/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("HTML_FRAGMENT"));

        mvc.perform(get("/api/v1/chapters/u1-c2/notes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"));

        String otpBody = mvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debugOtp").value("1234"))
                .andReturn().getResponse().getContentAsString();
        String challengeId = mapper.readTree(otpBody).path("challengeId").asText();

        String authBody = mvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"" + challengeId + "\",\"otp\":\"1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = mapper.readTree(authBody).path("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(false));

        mvc.perform(get("/api/v1/chapters/u1-c2/notes").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"));

        String orderBody = mvc.perform(post("/api/v1/billing/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"YEARLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(99900))
                .andReturn().getResponse().getContentAsString();
        String orderId = mapper.readTree(orderBody).path("orderId").asText();

        mvc.perform(post("/api/v1/billing/payments/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + orderId + "\",\"paymentId\":\"demo_pay_1\",\"signature\":\"dev-valid-signature\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(true));

        mvc.perform(get("/api/v1/chapters/u1-c2/notes").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/me/progress/chapters/u1-c1")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notesCompleted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notesCompleted").value(true));

        mvc.perform(get("/api/v1/me/progress").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes.completedChapters").value(1));
    }

    @Test
    void guestPyqAttemptRequiresAttemptTokenAfterStart() throws Exception {
        String startBody = mvc.perform(post("/api/v1/chapters/u1-c1/pyq/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode start = mapper.readTree(startBody);
        String attemptId = start.path("attemptId").asText();
        String guestToken = start.path("guestAttemptToken").asText();
        String questionId = start.path("questions").get(0).path("id").asText();

        mvc.perform(post("/api/v1/pyq/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + questionId + "\",\"selectedOption\":\"B\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/pyq/attempts/" + attemptId + "/answers")
                        .header("X-Attempt-Token", guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + questionId + "\",\"selectedOption\":\"B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctOption").value("B"));
    }
}
