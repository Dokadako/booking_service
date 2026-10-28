package com.dokadako.booking.room;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.dokadako.booking.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;

class RoomIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminShouldCreateUpdateAndDeleteRoom() throws Exception {
        String admin = adminToken();
        long id = createRoom(admin, uniqueName());

        String newName = uniqueName();
        mockMvc.perform(put("/api/rooms/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson(newName, 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.capacity").value(12));

        mockMvc.perform(delete("/api/rooms/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void userShouldReadRoomsButNotModifyThem() throws Exception {
        long id = createRoom(adminToken(), uniqueName());
        String user = registerAndLogin("user-" + UUID.randomUUID() + "@test.local");

        mockMvc.perform(get("/api/rooms/{id}", id).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson(uniqueName(), 5)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/rooms/{id}", id).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicateRoomNameIgnoringCase() throws Exception {
        String admin = adminToken();
        String name = uniqueName();
        createRoom(admin, name);

        mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson(name.toUpperCase(), 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidRoom() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson("", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());
    }

    @Test
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Room %d not found".formatted(Long.MAX_VALUE)));
    }

    private long createRoom(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson(name, 8)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String roomJson(String name, int capacity) {
        return """
                {"name": "%s", "capacity": %d, "description": "Test room"}
                """.formatted(name, capacity);
    }

    private static String uniqueName() {
        return "Room " + UUID.randomUUID();
    }
}
