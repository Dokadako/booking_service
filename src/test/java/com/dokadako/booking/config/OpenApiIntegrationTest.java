package com.dokadako.booking.config;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.dokadako.booking.AbstractIntegrationTest;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldDocumentAuthEndpointsAsPublicAndOthersAsProtected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").value(empty()))
                .andExpect(jsonPath("$.security").value(not(empty())));
    }
}
