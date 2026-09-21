package vn.simtim;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://127.0.0.1:5433/simtim_test}",
      "app.seed-demo=true",
      "app.demo-password=demo123"
    })
class SprintOneTest {
  @Autowired MockMvc mvc;
  @Autowired EntityManager em;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder encoder;
  String sale =
      "{\"lines\":[{\"id\":\"SP001\",\"quantity\":2}],\"method\":\"Tiền mặt\",\"tendered\":20000}";

  MockHttpServletRequestBuilder salesPost(String path, String body, String key) {
    return post(path)
        .with(user("NV001").roles("sales"))
        .with(csrf())
        .header("Idempotency-Key", key)
        .contentType("application/json")
        .content(body);
  }

  @Test
  void healthAndUnauthorized() throws Exception {
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.database").value("PostgreSQL"));
    mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
  }

  @Test
  void loginUsesHashedPasswordAndSession() throws Exception {
    var account = em.find(Model.Account.class, "NV001");
    assertThat(account.password).startsWith("$2");
    assertThat(encoder.matches("demo123", account.password)).isTrue();
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("username", "NV001")
                .param("password", "wrong"))
        .andExpect(status().isUnauthorized());
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("username", "NV001")
                    .param("password", "demo123"))
            .andExpect(status().isOk())
            .andReturn();
    mvc.perform(
            get("/api/me")
                .session(
                    (org.springframework.mock.web.MockHttpSession)
                        result.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("sales"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void csrfAndRoleEnforcedOnServer() throws Exception {
    mvc.perform(
            post("/api/invoices")
                .with(user("NV001").roles("sales"))
                .contentType("application/json")
                .content(sale))
        .andExpect(status().isForbidden());
    mvc.perform(salesPost("/api/products", "{\"name\":\"forbidden\"}", "denied-0001"))
        .andExpect(status().isForbidden());
    mvc.perform(salesPost("/api/receipts", "{}", "denied-0002")).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/invoices")
                .with(user("KHO001").roles("stock"))
                .with(csrf())
                .header("Idempotency-Key", "denied-0003")
                .contentType("application/json")
                .content(sale))
        .andExpect(status().isForbidden());
  }

  @Test
  void productsUniqueValidationAndSearch() throws Exception {
    String body =
        "{\"name\":\"Sữa thử\",\"barcode\":\"893467312001\",\"category\":\"Sữa & đồ"
            + " uống\",\"price\":\"15000\",\"unit\":\"Hộp\"}";
    mvc.perform(
            post("/api/products")
                .with(user("QL001").roles("manager"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
    mvc.perform(
            get("/api/products").param("query", "893467312001").with(user("NV001").roles("sales")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("SP001"));
    mvc.perform(
            post("/api/categories")
                .with(user("QL001").roles("manager"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void saleServerPriceFefoAndIdempotency() throws Exception {
    int before = em.find(Model.Batch.class, "LO01").quantity;
    int expired = em.find(Model.Batch.class, "LO07").quantity;
    var first =
        mvc.perform(salesPost("/api/invoices", sale, "sale-test-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(15300))
            .andExpect(jsonPath("$.changeDue").value(4700))
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(salesPost("/api/invoices", sale, "sale-test-01"))
        .andExpect(status().isOk())
        .andExpect(content().json(first));
    em.flush();
    assertThat(em.find(Model.Batch.class, "LO01").quantity).isEqualTo(before - 2);
    assertThat(em.find(Model.Batch.class, "LO07").quantity).isEqualTo(expired);
  }

  @Test
  void invalidSaleDoesNotChangeInventory() throws Exception {
    int before = em.find(Model.Batch.class, "LO01").quantity;
    mvc.perform(salesPost("/api/invoices", sale.replace("20000", "1"), "sale-test-02"))
        .andExpect(status().isConflict());
    assertThat(em.find(Model.Batch.class, "LO01").quantity).isEqualTo(before);
  }

  @Test
  void fractionalQuantityAndIncompleteTrainingRejected() throws Exception {
    mvc.perform(
            salesPost(
                "/api/invoices",
                sale.replace("\"quantity\":2", "\"quantity\":1.5"),
                "fractional-01"))
        .andExpect(status().isBadRequest());
    var result =
        mvc.perform(salesPost("/api/training/sessions", "{\"chapter\":0}", "early-train-01"))
            .andExpect(status().isOk())
            .andReturn();
    String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(salesPost("/api/training/sessions/" + id + "/complete", "{}", "early-done-01"))
        .andExpect(status().isConflict());
  }

  @Test
  void countConflictUsesVersionAndDoesNotOverwrite() throws Exception {
    String body =
        "{\"batchId\":\"LO01\",\"expected\":48,\"actual\":35,\"baseVersion\":999,\"note\":\"offline\"}";
    mvc.perform(
            post("/api/counts")
                .with(user("QL001").roles("manager"))
                .with(csrf())
                .header("Idempotency-Key", "count-test-01")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFLICT"));
    assertThat(em.find(Model.Batch.class, "LO01").quantity).isEqualTo(48);
  }

  @Test
  void trainingOwnershipAndFutureChapters() throws Exception {
    mvc.perform(salesPost("/api/training/sessions", "{\"chapter\":1}", "train-test-01"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message").value("Nội dung này đang được cập nhật, vui lòng thử lại sau"));
  }

  @Test
  void trainingCompletesWithoutOperationalMutations() throws Exception {
    long invoices = em.createQuery("select count(i) from Invoice i", Long.class).getSingleResult();
    int stock = em.find(Model.Batch.class, "LO01").quantity;
    var result =
        mvc.perform(salesPost("/api/training/sessions", "{\"chapter\":0}", "train-test-02"))
            .andExpect(status().isOk())
            .andReturn();
    String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(
            post("/api/training/sessions/" + id + "/events")
                .with(user("KHO001").roles("stock"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"station\":\"mentor\"}"))
        .andExpect(status().isNotFound());
    for (String spot : StoreService.STATIONS)
      mvc.perform(
              salesPost(
                  "/api/training/sessions/" + id + "/events",
                  json.writeValueAsString(Map.of("station", spot)),
                  "train-event-01"))
          .andExpect(status().isOk());
    mvc.perform(salesPost("/api/training/sessions/" + id + "/complete", "{}", "train-done-01"))
        .andExpect(status().isConflict());
    mvc.perform(
            salesPost(
                "/api/training/sessions/" + id + "/orientation",
                "{\"welcome\":\"checkout\",\"sensitive\":\"sales\",\"delivery\":\"receiving\"}",
                "train-quiz-01"))
        .andExpect(status().isConflict());
    mvc.perform(
            salesPost(
                "/api/training/sessions/" + id + "/orientation",
                "{\"welcome\":\"checkout\",\"sensitive\":\"manager\",\"delivery\":\"receiving\"}",
                "train-quiz-02"))
        .andExpect(status().isOk());
    mvc.perform(salesPost("/api/training/sessions/" + id + "/complete", "{}", "train-done-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    assertThat(em.find(Model.Batch.class, "LO01").quantity).isEqualTo(stock);
    assertThat(em.createQuery("select count(i) from Invoice i", Long.class).getSingleResult())
        .isEqualTo(invoices);
  }

  @Test
  void fiveRoleWriteBoundaryMatrix() throws Exception {
    String[][] actors = {
      {"ADMIN001", "admin"},
      {"KT001", "accountant"},
      {"QL001", "manager"},
      {"NV001", "sales"},
      {"KHO001", "stock"}
    };
    String[] paths = {
      "/api/invoices",
      "/api/receipts",
      "/api/counts",
      "/api/products",
      "/api/counts/missing/approve"
    };
    for (var actor : actors)
      for (String path : paths) {
        String role = actor[1];
        boolean allowed =
            role.equals("admin")
                || (path.equals("/api/invoices") && role.equals("sales"))
                || (path.equals("/api/receipts") && role.equals("stock"))
                || (Set.of("/api/counts", "/api/products", "/api/counts/missing/approve")
                        .contains(path)
                    && role.equals("manager"));
        int expected =
            !allowed
                ? 403
                : path.endsWith("approve") ? 404 : path.equals("/api/products") ? 409 : 400;
        mvc.perform(
                post(path)
                    .with(user(actor[0]).roles(role))
                    .with(csrf())
                    .header("Idempotency-Key", "matrix-test-01")
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().is(expected));
      }
  }

  @Test
  void readModelsDoNotLeakOtherRoleWorkspaces() throws Exception {
    mvc.perform(get("/api/state").with(user("NV001").roles("sales")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suppliers").isEmpty())
        .andExpect(jsonPath("$.movements").isEmpty())
        .andExpect(jsonPath("$.counts").isEmpty());
    mvc.perform(get("/api/state").with(user("KT001").roles("accountant")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products").isEmpty())
        .andExpect(jsonPath("$.batches").isEmpty())
        .andExpect(jsonPath("$.counts").isEmpty());
    mvc.perform(get("/api/state").with(user("KHO001").roles("stock")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoices").isEmpty())
        .andExpect(jsonPath("$.counts").isEmpty());
    mvc.perform(get("/api/suppliers").with(user("KT001").roles("accountant")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/products").with(user("ADMIN001").roles("admin")))
        .andExpect(status().isOk());
  }
}
