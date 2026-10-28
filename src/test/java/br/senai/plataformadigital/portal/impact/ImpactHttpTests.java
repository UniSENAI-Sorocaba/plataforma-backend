package br.senai.plataformadigital.portal.impact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("impact-local")
@Import(ImpactHttpTests.PermissionFixtures.class)
class ImpactHttpTests {
    private static final String ROOT = "/api/v1/impacts";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @TestConfiguration(proxyBeanMethods = false)
    static class PermissionFixtures {
        @Bean
        ImpactPermissionProvider fixturePermissions() {
            return (actor, action, id) -> actor.equals("synthetic-editor")
                    || (actor.equals("synthetic-reader") && action == Impact.Action.READ)
                    || (id != null && actor.equals("synthetic-scoped:" + id) && action == Impact.Action.READ);
        }
    }

    private MockHttpServletRequestBuilder editor(MockHttpServletRequestBuilder request) {
        return request.principal(() -> "synthetic-editor").header("X-Correlation-Id", "synthetic-correlation");
    }

    private String draft() throws Exception {
        try (var input = getClass().getResourceAsStream("/impact/draft.json")) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private JsonNode createDraft() throws Exception {
        MvcResult result = mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content(draft()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().exists("Location")).andReturn();
        contract(result, ROOT, "post");
        return json.readTree(result.getResponse().getContentAsString());
    }

    private MvcResult approve(String id, long version, long revision) throws Exception {
        MvcResult result = mvc.perform(editor(post(ROOT + "/{id}/versions/{version}/approval", id, version))
                        .header("If-Match", "\"" + revision + "\""))
                .andExpect(status().isOk()).andReturn();
        contract(result, ROOT + "/{id}/versions/{version}/approval", "post");
        return result;
    }

    private MvcResult publish(String id, long version, long revision) throws Exception {
        MvcResult result = mvc.perform(editor(put(ROOT + "/{id}/publication", id)).header("If-Match", "\"" + revision + "\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andExpect(status().isOk()).andReturn();
        contract(result, ROOT + "/{id}/publication", "put");
        return result;
    }

    @Test
    void completeVersionedPublicationWithdrawalAndPrivateTraceability() throws Exception {
        String id = createDraft().path("id").asText();
        MvcResult hidden = mvc.perform(get(ROOT + "/{id}", id)).andExpect(status().isNotFound()).andReturn();
        contract(hidden, ROOT + "/{id}", "get");
        approve(id, 1, 1);
        mvc.perform(get(ROOT + "/{id}", id)).andExpect(status().isNotFound());
        publish(id, 1, 2);
        MvcResult publicResult = mvc.perform(get(ROOT + "/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.sourceReference").doesNotExist())
                .andExpect(jsonPath("$.responsibleReference").doesNotExist()).andReturn();
        contract(publicResult, ROOT + "/{id}", "get");

        ObjectNode next = (ObjectNode) json.readTree(draft());
        next.put("title", "Segunda versão sintética");
        MvcResult revised = mvc.perform(editor(post(ROOT + "/{id}/versions", id)).header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(next)))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"4\""))
                .andExpect(jsonPath("$.latestVersion").value(2)).andReturn();
        contract(revised, ROOT + "/{id}/versions", "post");
        mvc.perform(get(ROOT + "/{id}", id)).andExpect(jsonPath("$.version").value(1));
        mvc.perform(editor(put(ROOT + "/{id}/publication", id)).header("If-Match", "\"4\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}"))
                .andExpect(status().isConflict());
        approve(id, 2, 4);
        publish(id, 2, 5);
        mvc.perform(get(ROOT + "/{id}", id)).andExpect(jsonPath("$.title").value("Segunda versão sintética"));
        MvcResult withdrawn = mvc.perform(editor(delete(ROOT + "/{id}/publication", id)).header("If-Match", "\"6\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedVersion").isEmpty()).andReturn();
        contract(withdrawn, ROOT + "/{id}/publication", "delete");
        mvc.perform(get(ROOT + "/{id}", id)).andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));

        MvcResult provenance = mvc.perform(editor(get(ROOT + "/{id}/versions/{version}/provenance", id, 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceReference").value("synthetic-private-source"))
                .andExpect(jsonPath("$.responsibleReference").value("synthetic-responsible"))
                .andExpect(jsonPath("$.periodStart").value("2026-01-01"))
                .andExpect(jsonPath("$.approvedBy").value("synthetic-editor")).andReturn();
        contract(provenance, ROOT + "/{id}/versions/{version}/provenance", "get");
        MvcResult history = mvc.perform(editor(get(ROOT + "/{id}/history", id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7)).andExpect(jsonPath("$.items[6].action").value("WITHDRAW"))
                .andExpect(jsonPath("$.items[6].correlationId").value("synthetic-correlation")).andReturn();
        contract(history, ROOT + "/{id}/history", "get");
        MvcResult versions = mvc.perform(editor(get(ROOT + "/{id}/versions", id)))
                .andExpect(jsonPath("$.totalElements").value(2)).andReturn();
        contract(versions, ROOT + "/{id}/versions", "get");
    }

    @Test
    void approvalAndPublicationAreSeparateAndRequireExistingVersions() throws Exception {
        String id = createDraft().path("id").asText();
        mvc.perform(editor(get(ROOT + "/{id}/versions/1/provenance", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approvedBy").isEmpty());
        mvc.perform(editor(put(ROOT + "/{id}/publication", id)).header("If-Match", "\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isConflict());
        mvc.perform(editor(post(ROOT + "/{id}/versions/99/approval", id)).header("If-Match", "\"1\""))
                .andExpect(status().isNotFound());
        mvc.perform(editor(delete(ROOT + "/{id}/publication", id)).header("If-Match", "\"1\""))
                .andExpect(status().isConflict());
        approve(id, 1, 1);
        mvc.perform(editor(post(ROOT + "/{id}/versions/1/approval", id)).header("If-Match", "\"2\""))
                .andExpect(status().isConflict());
        publish(id, 1, 2);
        mvc.perform(editor(put(ROOT + "/{id}/publication", id)).header("If-Match", "\"3\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isConflict());
    }

    @Test
    void authenticatedWithoutAuditPermissionCannotReadSourceOrHistory() throws Exception {
        String id = createDraft().path("id").asText();
        mvc.perform(get(ROOT + "/{id}/versions", id).principal(() -> "synthetic-reader"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceReference").doesNotExist());
        MvcResult denied = mvc.perform(get(ROOT + "/{id}/versions/1/provenance", id).principal(() -> "synthetic-reader"))
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Acesso negado")).andReturn();
        contract(denied, ROOT + "/{id}/versions/{version}/provenance", "get");
        assertFalse(denied.getResponse().getContentAsString().contains("synthetic-private-source"));
        mvc.perform(get(ROOT + "/{id}/history", id).principal(() -> "synthetic-reader"))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgedClientFieldsDoNotAuthenticateOrGrantPermissions() throws Exception {
        MvcResult anonymous = mvc.perform(post(ROOT).contentType(MediaType.APPLICATION_JSON).content(draft())
                        .header("Authorization", "Bearer synthetic-forged-token").header("X-Roles", "ADMIN")
                        .header("X-Actor", "synthetic-editor")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer")).andReturn();
        contract(anonymous, ROOT, "post");
        mvc.perform(post(ROOT).principal(() -> "synthetic-reader").header("X-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON).content(draft())).andExpect(status().isForbidden());
        ObjectNode spoofed = (ObjectNode) json.readTree(draft());
        spoofed.put("approved", true);
        mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(spoofed)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readPermissionDoesNotGrantAnyMutationAndDeniedCommandsDoNotCreateEvents() throws Exception {
        String id = createDraft().path("id").asText();
        for (MockHttpServletRequestBuilder mutation : new MockHttpServletRequestBuilder[]{
                post(ROOT + "/{id}/versions", id).contentType(MediaType.APPLICATION_JSON).content(draft()),
                post(ROOT + "/{id}/versions/1/approval", id),
                put(ROOT + "/{id}/publication", id).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"),
                delete(ROOT + "/{id}/publication", id)}) {
            mvc.perform(mutation.principal(() -> "synthetic-reader").header("If-Match", "\"1\""))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(editor(get(ROOT + "/{id}/history", id))).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void backendPermissionProviderReceivesTheResourceAndCannotBeBypassedByChangingItsId() throws Exception {
        String first = createDraft().path("id").asText();
        String second = createDraft().path("id").asText();
        java.security.Principal scoped = () -> "synthetic-scoped:" + first;
        mvc.perform(get(ROOT + "/{id}/versions", first).principal(scoped)).andExpect(status().isOk());
        mvc.perform(get(ROOT + "/{id}/versions", second).principal(scoped)
                        .header("X-Impact-Id", first).param("impactId", first))
                .andExpect(status().isForbidden());
    }

    @Test
    void originAndValidPeriodAreRequiredAndValidationErrorsAreSanitized() throws Exception {
        for (String field : new String[]{"sourceReference", "responsibleReference", "periodStart", "periodEnd"}) {
            ObjectNode bad = (ObjectNode) json.readTree(draft());
            ((ObjectNode) bad.path("provenance")).remove(field);
            mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }
        ObjectNode reversed = (ObjectNode) json.readTree(draft());
        ((ObjectNode) reversed.path("provenance")).put("periodEnd", "2025-01-01");
        mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(reversed)))
                .andExpect(status().isBadRequest());
        ObjectNode blank = (ObjectNode) json.readTree(draft());
        blank.put("title", " ");
        MvcResult bad = mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(blank)))
                .andExpect(status().isBadRequest()).andReturn();
        contract(bad, ROOT, "post");
        assertFalse(bad.getResponse().getContentAsString().contains("synthetic-private-source"));
        mvc.perform(editor(post(ROOT)).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ROOT + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void optimisticRevisionIsMandatoryAndStaleRequestsDoNotChangeHistory() throws Exception {
        String id = createDraft().path("id").asText();
        MvcResult absent = mvc.perform(editor(post(ROOT + "/{id}/versions/1/approval", id)))
                .andExpect(status().isPreconditionRequired()).andReturn();
        contract(absent, ROOT + "/{id}/versions/{version}/approval", "post");
        for (String invalid : new String[]{"1", "*", "W/\"1\"", "\"999999999999999999999999999\""}) {
            mvc.perform(editor(post(ROOT + "/{id}/versions/1/approval", id)).header("If-Match", invalid))
                    .andExpect(status().isBadRequest());
        }
        approve(id, 1, 1);
        MvcResult stale = mvc.perform(editor(delete(ROOT + "/{id}/publication", id)).header("If-Match", "\"1\""))
                .andExpect(status().isPreconditionFailed()).andReturn();
        contract(stale, ROOT + "/{id}/publication", "delete");
        mvc.perform(editor(get(ROOT + "/{id}/history", id))).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void publicAndErrorResponsesNeverEnableCachingAndCorrelationIsSanitized() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get(ROOT + "/{id}", id).header("X-Correlation-Id", "valid-123"))
                .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Correlation-Id", "valid-123"))
                .andExpect(jsonPath("$.correlationId").value("valid-123"));
        MvcResult invalid = mvc.perform(get(ROOT + "/{id}", id).header("X-Correlation-Id", "invalid private value"))
                .andExpect(status().isNotFound()).andReturn();
        assertNotEquals("invalid private value", invalid.getResponse().getHeader("X-Correlation-Id"));
    }

    @Test
    void versionAndHistoryQueriesHaveBoundedConsistentPagination() throws Exception {
        String id = createDraft().path("id").asText();
        approve(id, 1, 1);
        mvc.perform(editor(get(ROOT + "/{id}/history", id)).param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("APPROVE"));
        mvc.perform(editor(get(ROOT + "/{id}/versions", id)).param("page", "2147483647").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        for (String[] pagination : new String[][]{{"-1", "25"}, {"0", "0"}, {"0", "101"}}) {
            mvc.perform(editor(get(ROOT + "/{id}/versions", id)).param("page", pagination[0]).param("size", pagination[1]))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void everyImpactOperationIsRepresentedInLocalOpenApiAndProtectedOperationsDeclare401403() throws Exception {
        JsonNode document = json.readTree(Path.of("contracts/impact.openapi.json").toFile());
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getBeanType() != ImpactController.class) return;
            mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods().forEach(method -> {
                JsonNode operation = document.path("paths").path(path).path(method.name().toLowerCase(java.util.Locale.ROOT));
                assertFalse(operation.isMissingNode(), method + " " + path);
                if (operation.has("x-permission")) {
                    assertFalse(operation.path("security").isEmpty());
                    assertTrue(operation.path("responses").has("401"));
                    assertTrue(operation.path("responses").has("403"));
                }
            }));
        });
    }

    private void contract(MvcResult result, String path, String method) throws Exception {
        JsonNode document = json.readTree(Path.of("contracts/impact.openapi.json").toFile());
        int status = result.getResponse().getStatus();
        JsonNode response = document.path("paths").path(path).path(method).path("responses").path(String.valueOf(status));
        if (response.has("$ref")) response = document.at(response.path("$ref").asText().substring(1));
        assertFalse(response.isMissingNode(), "Resposta não documentada: " + status);
        String media = status >= 400 ? "application/problem+json" : "application/json";
        ObjectNode schema = json.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.set("$defs", document.path("components").path("schemas"));
        schema.setAll((ObjectNode) response.path("content").path(media).path("schema"));
        JsonNode standardSchema = json.readTree(schema.toString().replace("#/components/schemas/", "#/$defs/"));
        var validator = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(standardSchema);
        assertTrue(validator.validate(json.readTree(result.getResponse().getContentAsString())).isEmpty(),
                "Resposta viola o schema: " + result.getResponse().getContentAsString());
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
        assertNotNull(result.getResponse().getHeader("X-Correlation-Id"));
    }
}
