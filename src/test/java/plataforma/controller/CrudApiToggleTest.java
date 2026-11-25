package plataforma.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import plataforma.dto.PageResponse;
import plataforma.service.UserService;

/** Os CRUDs não têm autenticação ainda: só podem responder quando a propriedade está ligada. */
class CrudApiToggleTest {

    @Nested
    @WebMvcTest(controllers = UserController.class)
    class PadraoDesligado {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService service;

        @Test
        void rotaNaoExisteSemAPropriedade() throws Exception {
            mockMvc.perform(get("/api/v1/users")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @WebMvcTest(controllers = UserController.class, properties = "platform.crud.api.enabled=true")
    class Ligado {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService service;

        @Test
        void rotaRespondeComAPropriedadeLigada() throws Exception {
            when(service.findAll(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
            mockMvc.perform(get("/api/v1/users")).andExpect(status().isOk());
        }
    }
}
