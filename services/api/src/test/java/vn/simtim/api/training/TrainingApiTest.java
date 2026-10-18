package vn.simtim.api.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.simtim.api.auth.domain.PasswordHasher;

@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrainingApiTest {
    private static final UUID ORG = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MAIN = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final String OTHER_STORE_CODE = "TRN-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String PASSWORD = "training-fixture-password";
    private static final String[] STAGES = {"greet", "milk", "checkout", "cooler", "quarantine", "mentor"};
    private static final String[] ACTIONS = {"greet_customer", "pick_product", "tender_payment",
            "inspect_cooler", "quarantine_product", "report_mentor"};
    private static final String[] TARGETS = {"customer", "milk", "checkout", "cooler", "quarantine", "mentor"};

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_training_test");

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry properties) {
        String external = System.getenv("SIMTIM_TEST_DB_URL");
        if (external == null || external.isBlank()) {
            postgres.start();
            properties.add("spring.datasource.url", postgres::getJdbcUrl);
            properties.add("spring.datasource.username", postgres::getUsername);
            properties.add("spring.datasource.password", postgres::getPassword);
        } else {
            properties.add("spring.datasource.url", () -> external);
            properties.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private final UUID scenario = UUID.randomUUID();
    private final UUID otherStore = UUID.randomUUID();
    private final List<UUID> users = new ArrayList<>();
    private final List<UUID> steps = new ArrayList<>();
    private String learnerToken;
    private String managerToken;
    private String newManagerToken;
    private String adminToken;
    private UUID transferLearner;

    @BeforeAll
    void setup() {
        jdbc.update("insert into core.stores(id,organization_id,code,name,status) values(?,?,?,'Other store','ACTIVE')",
                otherStore, ORG, OTHER_STORE_CODE);
        jdbc.update("""
                insert into training.scenarios(id,organization_id,code,scenario_version,name,summary,status,created_at)
                values(?,?,'SCN-RETAIL-TEST',1,'Training test','Provider test','ACTIVE',now())
                """, scenario, ORG);
        for (int i = 0; i < STAGES.length; i++) {
            UUID step = UUID.randomUUID();
            steps.add(step);
            jdbc.update("""
                    insert into training.scenario_steps(id,organization_id,scenario_id,step_order,
                        stage,action_type,target_key,instruction_text) values(?,?,?,?,?,?,?,'Do the step')
                    """, step, ORG, scenario, i + 1, STAGES[i], ACTIONS[i], TARGETS[i]);
            jdbc.update("""
                    insert into training.scenario_choices(id,organization_id,scenario_id,
                        scenario_step_id,choice_key,is_correct,feedback_text)
                    values(?,?,?,?, 'right',true,'Correct'),(?,?,?,?, 'wrong',false,'Try again')
                    """, UUID.randomUUID(), ORG, scenario, step, UUID.randomUUID(), ORG, scenario, step);
        }
        learnerToken = createUser("SALES", MAIN, "MAIN", true).token();
        managerToken = createUser("MANAGER", MAIN, "MAIN", false).token();
        newManagerToken = createUser("MANAGER", otherStore, OTHER_STORE_CODE, false).token();
        adminToken = createUser("ADMIN", MAIN, "MAIN", false).token();
        transferLearner = createUser("SALES", MAIN, "MAIN", true).userId();
    }

    @AfterAll
    void cleanup() {
        try {
            // The seeded retail scenario is also exercised by this fixture. Remove only
            // its sessions owned by the fixture users before deleting those users.
            for (UUID user : users) {
                jdbc.update("""
                        delete from training.session_results where session_id in
                            (select id from training.sessions where learner_user_id=?)
                        """, user);
                jdbc.update("""
                        delete from training.session_actions where session_id in
                            (select id from training.sessions where learner_user_id=?)
                        """, user);
                jdbc.update("delete from training.sessions where learner_user_id=?", user);
            }
            jdbc.update("""
                    delete from training.session_results where session_id in
                        (select id from training.sessions where scenario_id=?)
                    """, scenario);
            jdbc.update("""
                    delete from training.session_actions where session_id in
                        (select id from training.sessions where scenario_id=?)
                    """, scenario);
            jdbc.update("delete from training.sessions where organization_id=? and scenario_id=?", ORG, scenario);
            jdbc.update("delete from training.scenario_choices where scenario_id=?", scenario);
            jdbc.update("delete from training.scenario_steps where scenario_id=?", scenario);
            jdbc.update("delete from training.scenarios where id=?", scenario);
            for (UUID user : users) {
                jdbc.update("delete from iam.auth_sessions where user_id=?", user);
                jdbc.update("delete from iam.user_roles where user_id=?", user);
                jdbc.update("delete from iam.users where id=?", user);
            }
            jdbc.update("delete from core.stores where id=?", otherStore);
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    @Test
    void serverGradesStoredChoicesAndReplayNeverDuplicatesOrTouchesOperations() {
        Map<String, Number> before = operationalCounts();
        UUID key = UUID.randomUUID();
        var started = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString()),
                learnerToken, key);
        assertThat(started.getStatusCode().value()).isEqualTo(201);
        UUID id = UUID.fromString((String) started.getBody().get("id"));
        var replay = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString()),
                learnerToken, key);
        assertThat(replay.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getBody().get("id")).isEqualTo(id.toString());
        var otherActor = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString()),
                managerToken, key);
        assertThat(otherActor.getStatusCode().value()).isEqualTo(409);
        assertThat(otherActor.getBody()).containsEntry("code", "START_REPLAY_MISMATCH");

        var invalid = post(actionPath(id), Map.of("actionSequence", 1, "actionType", ACTIONS[0],
                "targetKey", TARGETS[0], "choiceKey", "forged"), learnerToken, null);
        assertThat(invalid.getStatusCode().value()).isEqualTo(400);
        assertThat(actionCount(id)).isZero();

        var wrongBody = action(1, 0, "wrong");
        var wrong = post(actionPath(id), wrongBody, learnerToken, null);
        assertThat(wrong.getStatusCode().value()).isEqualTo(200);
        assertThat(wrong.getBody()).containsEntry("evaluationStatus", "INCORRECT");
        var wrongReplay = post(actionPath(id), wrongBody, learnerToken, null);
        assertThat(wrongReplay.getBody()).isEqualTo(wrong.getBody());
        assertThat(actionCount(id)).isEqualTo(1);

        var changed = post(actionPath(id), action(1, 0, "right"), learnerToken, null);
        assertThat(changed.getStatusCode().value()).isEqualTo(409);
        assertThat(changed.getBody()).containsEntry("code", "ACTION_REPLAY_MISMATCH");
        var skip = post(actionPath(id), action(3, 0, "right"), learnerToken, null);
        assertThat(skip.getStatusCode().value()).isEqualTo(409);
        assertThat(skip.getBody()).containsEntry("code", "ACTION_OUT_OF_ORDER");

        for (int i = 0; i < STAGES.length; i++) {
            var response = post(actionPath(id), action(i + 2, i, "right"), learnerToken, null);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            if (i == STAGES.length - 1) {
                assertThat(response.getBody()).containsEntry("status", "COMPLETED");
                Map<?, ?> result = (Map<?, ?>) response.getBody().get("result");
                assertThat(result.get("totalScore")).isEqualTo(86);
                assertThat(result.get("passed")).isEqualTo(true);
                assertThat(result.get("grade")).isEqualTo("SATISFACTORY");
                var finalReplay = post(actionPath(id), action(i + 2, i, "right"), learnerToken, null);
                assertThat(finalReplay.getBody()).isEqualTo(response.getBody());
            }
        }
        assertThat(actionCount(id)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select count(*) from training.session_results where session_id=?",
                Integer.class, id)).isEqualTo(1);
        assertThat(operationalCounts()).isEqualTo(before);

        var details = get("/api/v1/training/sessions/" + id, learnerToken);
        assertThat(details.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) details.getBody().get("actions")).hasSize(7);
        var self = get("/api/v1/training/results", learnerToken);
        assertThat(self.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) self.getBody().get("items")).isNotEmpty();
        assertThat(get("/api/v1/training/sessions/" + id, managerToken).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void seededRetailScenarioRequiresTwoDistinctScansAndKeepsOperationsUnchanged() {
        UUID demoScenario = jdbc.queryForObject("""
                select id from training.scenarios
                where organization_id=? and code='SCN-RETAIL-01' and scenario_version=1
                """, UUID.class, ORG);
        assertThat(demoScenario).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from training.scenario_steps where scenario_id=?",
                Integer.class, demoScenario)).isEqualTo(8);
        Map<String, Number> before = operationalCounts();
        UUID id = UUID.fromString((String) post("/api/v1/training/sessions",
                Map.of("scenarioId", demoScenario.toString()), learnerToken, UUID.randomUUID())
                .getBody().get("id"));
        String[] actionTypes = {"greet_customer", "pick_product", "scan_item", "scan_item",
                "tender_payment", "inspect_cooler", "quarantine_product", "report_mentor"};
        String[] targetKeys = {"customer_linh", "shelf_milk", "pos_scanner", "pos_scanner",
                "pos_cash", "cooler_display", "quarantine_bin", "mentor_mai"};
        String[] choiceKeys = {"greet_confirm_need", "milk_pick_2_low_sugar", "scan_item_1",
                "scan_item_2", "tender_3k_receipt", "cooler_quarantine_expired",
                "quarantine_label_expired", "mentor_full_report"};
        for (int i = 0; i < choiceKeys.length; i++) {
            var response = post(actionPath(id), Map.of("actionSequence", i + 1,
                    "actionType", actionTypes[i], "targetKey", targetKeys[i],
                    "choiceKey", choiceKeys[i]), learnerToken, null);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            if (i == choiceKeys.length - 1) {
                assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");
                Map<?, ?> result = (Map<?, ?>) response.getBody().get("result");
                assertThat(result.get("totalScore")).isEqualTo(100);
                assertThat(result.get("passed")).isEqualTo(true);
            }
        }
        assertThat(operationalCounts()).isEqualTo(before);

        UUID retryId = UUID.fromString((String) post("/api/v1/training/sessions",
                Map.of("scenarioId", demoScenario.toString()), learnerToken, UUID.randomUUID())
                .getBody().get("id"));
        for (int i = 0; i < 3; i++) {
            assertThat(post(actionPath(retryId), Map.of("actionSequence", i + 1,
                    "actionType", actionTypes[i], "targetKey", targetKeys[i],
                    "choiceKey", choiceKeys[i]), learnerToken, null).getStatusCode().value())
                    .isEqualTo(200);
        }
        var duplicate = post(actionPath(retryId), Map.of("actionSequence", 4,
                "actionType", "scan_item", "targetKey", "pos_scanner",
                "choiceKey", "scan_item_1"), learnerToken, null);
        assertThat(duplicate.getBody()).containsEntry("evaluationStatus", "INCORRECT")
                .containsEntry("status", "IN_PROGRESS");
        assertThat(jdbc.queryForObject("select step_cursor from training.sessions where id=?",
                Integer.class, retryId)).isEqualTo(3);
        assertThat(operationalCounts()).isEqualTo(before);
    }

    @Test
    void transferChangesManagerVisibilityButKeepsLearnerCertificate() {
        String userName = jdbc.queryForObject("select username from iam.users where id=?", String.class,
                transferLearner);
        String token = login(userName, "MAIN");
        UUID id = UUID.fromString((String) post("/api/v1/training/sessions",
                Map.of("scenarioId", scenario.toString()), token, UUID.randomUUID()).getBody().get("id"));
        for (int i = 0; i < STAGES.length; i++) {
            assertThat(post(actionPath(id), action(i + 1, i, "right"), token, null)
                    .getStatusCode().value()).isEqualTo(200);
        }
        assertThat(resultIds(managerToken)).contains(id.toString());
        jdbc.update("delete from iam.user_roles where user_id=? and store_id=?", transferLearner, MAIN);
        assignRole(transferLearner, "SALES", otherStore);
        String transferredToken = login(userName, OTHER_STORE_CODE);
        assertThat(resultIds(managerToken)).doesNotContain(id.toString());
        assertThat(resultIds(newManagerToken)).contains(id.toString());
        assertThat(resultIds(adminToken)).contains(id.toString());
        assertThat(resultIds(transferredToken)).contains(id.toString());
        assertThat(get("/api/v1/training/sessions/" + id, transferredToken).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void trainingFlagAndClientSuppliedResultsAreRejected() {
        String disabled = createUser("SALES", MAIN, "MAIN", false).token();
        var forbidden = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString()),
                disabled, UUID.randomUUID());
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
        assertThat(forbidden.getBody()).containsEntry("code", "TRAINING_DISABLED");
        var forged = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString(),
                "score", 100), learnerToken, UUID.randomUUID());
        assertThat(forged.getStatusCode().value()).isEqualTo(400);
        assertThat(forged.getBody()).containsEntry("code", "INVALID_REQUEST");
        assertThat(get("/api/v1/training/results?scope=STORE", disabled).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void abandonIsIdempotentAndRecordedRetryStillWorksAfterTrainingIsDisabled() {
        UserSession learner = createUser("SALES", MAIN, "MAIN", true);
        UUID key = UUID.randomUUID();
        UUID id = UUID.fromString((String) post("/api/v1/training/sessions",
                Map.of("scenarioId", scenario.toString()), learner.token(), key)
                .getBody().get("id"));
        var first = post(actionPath(id), action(1, 0, "wrong"), learner.token(), null);
        jdbc.update("update iam.users set training_enabled=false where id=?", learner.userId());
        var startReplay = post("/api/v1/training/sessions", Map.of("scenarioId", scenario.toString()),
                learner.token(), key);
        assertThat(startReplay.getStatusCode().value()).isEqualTo(201);
        assertThat(startReplay.getBody().get("id")).isEqualTo(id.toString());
        var blocked = post(actionPath(id), action(2, 0, "right"), learner.token(), null);
        assertThat(blocked.getStatusCode().value()).isEqualTo(403);
        assertThat(blocked.getBody()).containsEntry("code", "TRAINING_DISABLED");
        var replay = post(actionPath(id), action(1, 0, "wrong"), learner.token(), null);
        assertThat(replay.getBody()).isEqualTo(first.getBody());
        var forgedAbandon = post("/api/v1/training/sessions/" + id + "/abandon",
                Map.of("score", 100), learner.token(), null);
        assertThat(forgedAbandon.getStatusCode().value()).isEqualTo(400);
        var abandoned = post("/api/v1/training/sessions/" + id + "/abandon",
                Map.of(), learner.token(), null);
        assertThat(abandoned.getStatusCode().value()).isEqualTo(200);
        assertThat(abandoned.getBody()).containsEntry("status", "ABANDONED");
        var again = post("/api/v1/training/sessions/" + id + "/abandon",
                Map.of(), learner.token(), null);
        assertThat(again.getBody()).isEqualTo(abandoned.getBody());
        assertThat(jdbc.queryForObject("select count(*) from training.session_results where session_id=?",
                Integer.class, id)).isZero();
        assertThat(get("/api/v1/training/sessions/" + id, learner.token()).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void failedFinalWriteRollsBackSessionActionAndScoreTogether() {
        UUID id = UUID.fromString((String) post("/api/v1/training/sessions",
                Map.of("scenarioId", scenario.toString()), learnerToken, UUID.randomUUID())
                .getBody().get("id"));
        for (int i = 0; i < STAGES.length - 1; i++) {
            assertThat(post(actionPath(id), action(i + 1, i, "right"), learnerToken, null)
                    .getStatusCode().value()).isEqualTo(200);
        }
        // A deliberately inconsistent row makes the final INSERT fail after session/action writes.
        jdbc.update("""
                insert into training.session_results(session_id,organization_id,total_score,
                    passed,grade,error_count,completed_at)
                values(?,?,0,false,'NEEDS_PRACTICE',0,now())
                """, id, ORG);
        var failed = post(actionPath(id), action(STAGES.length, STAGES.length - 1, "right"),
                learnerToken, null);
        assertThat(failed.getStatusCode().is5xxServerError()).isTrue();
        assertThat(actionCount(id)).isEqualTo(STAGES.length - 1);
        assertThat(jdbc.queryForObject("select status from training.sessions where id=?", String.class, id))
                .isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("select step_cursor from training.sessions where id=?", Integer.class, id))
                .isEqualTo(STAGES.length - 1);
        jdbc.update("delete from training.session_results where session_id=?", id);
        var completed = post(actionPath(id), action(STAGES.length, STAGES.length - 1, "right"),
                learnerToken, null);
        assertThat(completed.getStatusCode().value()).isEqualTo(200);
        assertThat(completed.getBody()).containsEntry("status", "COMPLETED");
    }

    @Test
    void concurrentStartAndActionRetriesPersistOnce() {
        UUID key = UUID.randomUUID();
        Map<String, Object> request = Map.of("scenarioId", scenario.toString());
        CountDownLatch begin = new CountDownLatch(1);
        var a = CompletableFuture.supplyAsync(() -> {
            await(begin);
            return post("/api/v1/training/sessions", request, learnerToken, key);
        });
        var b = CompletableFuture.supplyAsync(() -> {
            await(begin);
            return post("/api/v1/training/sessions", request, learnerToken, key);
        });
        begin.countDown();
        var startA = a.join();
        var startB = b.join();
        assertThat(startA.getStatusCode().value()).isEqualTo(201);
        assertThat(startB.getStatusCode().value()).isEqualTo(201);
        assertThat(startA.getBody().get("id")).isEqualTo(startB.getBody().get("id"));
        UUID id = UUID.fromString((String) startA.getBody().get("id"));
        assertThat(jdbc.queryForObject("select count(*) from training.sessions where idempotency_key=?",
                Integer.class, key)).isEqualTo(1);

        CountDownLatch send = new CountDownLatch(1);
        var actionA = CompletableFuture.supplyAsync(() -> {
            await(send);
            return post(actionPath(id), action(1, 0, "wrong"), learnerToken, null);
        });
        var actionB = CompletableFuture.supplyAsync(() -> {
            await(send);
            return post(actionPath(id), action(1, 0, "wrong"), learnerToken, null);
        });
        send.countDown();
        assertThat(actionA.join().getBody()).isEqualTo(actionB.join().getBody());
        assertThat(actionCount(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select error_count from training.sessions where id=?",
                Integer.class, id)).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private Map<String, Object> action(int sequence, int step, String choice) {
        return Map.of("actionSequence", sequence, "actionType", ACTIONS[step],
                "targetKey", TARGETS[step], "choiceKey", choice);
    }

    private String actionPath(UUID id) { return "/api/v1/training/sessions/" + id + "/actions"; }

    private record UserSession(UUID userId, String token) {}

    private UserSession createUser(String role, UUID store, String storeCode, boolean enabled) {
        UUID id = UUID.randomUUID();
        String username = "trn_" + id;
        jdbc.update("""
                insert into iam.users(id,organization_id,username,password_hash,full_name,status,training_enabled)
                values(?,?,?,?,?,'ACTIVE',?)
                """, id, ORG, username, passwords.hash(PASSWORD), "Training fixture", enabled);
        users.add(id);
        assignRole(id, role, store);
        return new UserSession(id, login(username, storeCode));
    }

    private void assignRole(UUID user, String role, UUID store) {
        jdbc.update("""
                insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                select ?,?,r.id,?,now(),? from iam.roles r
                where r.organization_id=? and r.code=?
                """, ORG, user, store, user, ORG, role);
    }

    private String login(String username, String storeCode) {
        var result = rest.postForEntity("/api/v1/auth/login", Map.of("organizationCode", "SIMTIM",
                "storeCode", storeCode, "username", username, "password", PASSWORD), Map.class);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        return (String) result.getBody().get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private org.springframework.http.ResponseEntity<Map> post(String path, Map<String, ?> body,
            String token, UUID key) {
        HttpHeaders headers = headers(token);
        if (key != null) headers.set("Idempotency-Key", key.toString());
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private org.springframework.http.ResponseEntity<Map> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), Map.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private int actionCount(UUID id) {
        return jdbc.queryForObject("select count(*) from training.session_actions where session_id=?",
                Integer.class, id);
    }

    private List<String> resultIds(String token) {
        var response = get("/api/v1/training/results?scope="
                + (token.equals(adminToken) ? "ORGANIZATION"
                    : token.equals(managerToken) || token.equals(newManagerToken) ? "STORE" : "SELF"), token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return ((List<Map<String, Object>>) response.getBody().get("items")).stream()
                .map(item -> (String) item.get("sessionId")).toList();
    }

    private Map<String, Number> operationalCounts() {
        return Map.of(
                "invoices", jdbc.queryForObject("select count(*) from sales.invoices", Long.class),
                "balances", jdbc.queryForObject("select coalesce(sum(on_hand_quantity),0) from inventory.inventory_balances", java.math.BigDecimal.class),
                "movements", jdbc.queryForObject("select count(*) from inventory.stock_movements", Long.class),
                "revenue", jdbc.queryForObject("""
                        select coalesce(sum(grand_total),0) from sales.invoices where status='COMPLETED'
                        """, Long.class));
    }
}
