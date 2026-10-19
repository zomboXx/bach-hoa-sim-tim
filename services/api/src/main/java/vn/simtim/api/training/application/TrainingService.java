package vn.simtim.api.training.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.training.domain.TrainingFailure;
import vn.simtim.api.training.domain.TrainingOracle;

@Service
public class TrainingService {
    public record Choice(boolean correct, String feedback) {}
    public record Step(int order, String stage, String actionType, String targetKey,
            Map<String, Choice> choices) {}
    private record Session(UUID id, UUID organizationId, UUID storeId, UUID learnerId,
            UUID scenarioId, int scenarioVersion, int attemptNumber, String status,
            int stepCursor, int errorCount, String stepsSnapshot,
            OffsetDateTime startedAt, OffsetDateTime endedAt) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Clock clock;

    public TrainingService(JdbcTemplate jdbc, ObjectMapper mapper, Clock clock) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> scenarios(SessionPrincipal actor) {
        requireTraining(actor);
        return jdbc.query("""
                select id,code,scenario_version,name,summary from training.scenarios
                where organization_id=? and status='ACTIVE' order by code,scenario_version desc
                """, (rs, n) -> map("id", rs.getObject("id", UUID.class),
                        "code", rs.getString("code"), "version", rs.getInt("scenario_version"),
                        "name", rs.getString("name"), "summary", rs.getString("summary")),
                actor.organizationId());
    }

    @Transactional
    public Map<String, Object> start(SessionPrincipal actor, UUID key, JsonNode body) {
        fields(body, Set.of("scenarioId"));
        UUID scenarioId = uuid(body, "scenarioId");
        if (key == null) throw failure(400, "INVALID_REQUEST", "Idempotency-Key required");
        // Lock the learner row to serialize both replay and attempt allocation.
        jdbc.queryForList("select id from iam.users where organization_id=? and id=? for update",
                actor.organizationId(), actor.userId());
        var existing = jdbc.query("""
                select * from training.sessions where organization_id=? and idempotency_key=?
                """, this::sessionRow, actor.organizationId(), key).stream().findFirst();
        if (existing.isPresent()) {
            Session old = existing.get();
            if (!old.learnerId().equals(actor.userId()) || !old.storeId().equals(actor.storeId())
                    || !old.scenarioId().equals(scenarioId)) {
                throw failure(409, "START_REPLAY_MISMATCH", "Idempotency key belongs to another start request");
            }
            return startResponse(old);
        }
        requireTraining(actor);
        var scenario = jdbc.query("""
                select scenario_version from training.scenarios
                where organization_id=? and id=? and status='ACTIVE'
                """, (rs, n) -> rs.getInt(1), actor.organizationId(), scenarioId)
                .stream().findFirst().orElseThrow(() -> failure(404, "NOT_FOUND", "Active scenario not found"));
        List<Step> steps = loadSteps(actor.organizationId(), scenarioId);
        if (steps.isEmpty()) throw failure(409, "SCENARIO_NOT_READY", "Scenario has no steps");
        int attempt = jdbc.queryForObject("""
                select coalesce(max(attempt_number),0)+1 from training.sessions
                where scenario_id=? and learner_user_id=?
                """, Integer.class, scenarioId, actor.userId());
        UUID id = UUID.randomUUID();
        OffsetDateTime now = now();
        int inserted = jdbc.update("""
                insert into training.sessions(id,organization_id,store_id,learner_user_id,
                    scenario_id,scenario_version,attempt_number,idempotency_key,status,
                    steps_snapshot,started_at)
                values(?,?,?,?,?,?,?,?,'IN_PROGRESS',?::jsonb,?)
                on conflict (organization_id,idempotency_key) do nothing
                """, id, actor.organizationId(), actor.storeId(), actor.userId(), scenarioId,
                scenario, attempt, key, json(steps), now);
        if (inserted == 0) {
            Session winner = jdbc.query("""
                    select * from training.sessions where organization_id=? and idempotency_key=?
                    """, this::sessionRow, actor.organizationId(), key).getFirst();
            if (!winner.learnerId().equals(actor.userId()) || !winner.storeId().equals(actor.storeId())
                    || !winner.scenarioId().equals(scenarioId)) {
                throw failure(409, "START_REPLAY_MISMATCH", "Idempotency key belongs to another start request");
            }
            return startResponse(winner);
        }
        return map("id", id, "scenarioId", scenarioId, "scenarioVersion", scenario,
                "attemptNumber", attempt, "status", "IN_PROGRESS", "nextActionSequence", 1,
                "startedAt", now);
    }

    @Transactional
    public Map<String, Object> action(SessionPrincipal actor, UUID id, JsonNode body) {
        fields(body, Set.of("actionSequence", "actionType", "targetKey", "choiceKey"));
        int sequence = positiveInt(body, "actionSequence");
        String actionType = bounded(body, "actionType", 60);
        String targetKey = bounded(body, "targetKey", 100);
        String choiceKey = optionalBounded(body, "choiceKey", 100);
        Session session = ownSession(actor, id, true);

        var recorded = jdbc.query("""
                select action_type,target_key,choice_key,response_data::text from training.session_actions
                where organization_id=? and session_id=? and action_sequence=?
                """, (rs, n) -> map("actionType", rs.getString(1), "targetKey", rs.getString(2),
                        "choiceKey", rs.getString(3), "response", rs.getString(4)),
                actor.organizationId(), id, sequence).stream().findFirst();
        if (recorded.isPresent()) {
            var previous = recorded.get();
            if (!Objects.equals(actionType, previous.get("actionType"))
                    || !Objects.equals(targetKey, previous.get("targetKey"))
                    || !Objects.equals(choiceKey, previous.get("choiceKey"))) {
                throw failure(409, "ACTION_REPLAY_MISMATCH", "Action sequence already has another payload");
            }
            return readMap((String) previous.get("response"));
        }
        if (!session.status().equals("IN_PROGRESS"))
            throw failure(409, "SESSION_CLOSED", "Training session is closed");
        requireTraining(actor);
        if (!session.storeId().equals(actor.storeId()))
            throw failure(403, "FORBIDDEN", "Training session belongs to another store");
        int nextSequence = jdbc.queryForObject("""
                select coalesce(max(action_sequence),0)+1 from training.session_actions where session_id=?
                """, Integer.class, id);
        if (sequence != nextSequence)
            throw failure(409, "ACTION_OUT_OF_ORDER", "Action sequence must be the next number");

        List<Step> steps = readSteps(session.stepsSnapshot());
        Step step = steps.get(session.stepCursor());
        if (!step.actionType().equals(actionType) || !step.targetKey().equals(targetKey))
            throw failure(422, "ACTION_NOT_ALLOWED", "Action does not match the current scenario step");
        Choice choice = step.choices().get(choiceKey == null ? "_default" : choiceKey);
        if (choice == null)
            throw failure(400, "INVALID_ACTION", "Unknown choice for the current step");
        int cursor = session.stepCursor() + (choice.correct() ? 1 : 0);
        int errors = session.errorCount() + (choice.correct() ? 0 : 1);
        boolean complete = cursor == steps.size();
        OffsetDateTime now = now();
        String status = complete ? "COMPLETED" : "IN_PROGRESS";
        Map<String, Object> response = map("actionSequence", sequence,
                "evaluationStatus", choice.correct() ? "CORRECT" : "INCORRECT",
                "feedback", choice.feedback(), "status", status,
                "nextActionSequence", sequence + 1);
        TrainingOracle.Result result = complete ? TrainingOracle.complete(steps.size(), errors) : null;
        if (result != null) {
            response.put("result", map("totalScore", result.totalScore(), "passed", result.passed(),
                    "grade", result.grade(), "errorCount", result.errorCount(), "completedAt", now));
        }
        jdbc.update("""
                update training.sessions set step_cursor=?,error_count=?,status=?,ended_at=?
                where organization_id=? and id=?
                """, cursor, errors, status, complete ? now : null, actor.organizationId(), id);
        jdbc.update("""
                insert into training.session_actions(id,organization_id,session_id,action_sequence,
                    step_order,action_type,target_key,choice_key,evaluation_status,feedback_text,
                    response_data,occurred_at)
                values(?,?,?,?,?,?,?,?,?,?,?::jsonb,?)
                """, UUID.randomUUID(), actor.organizationId(), id, sequence, step.order(),
                actionType, targetKey, choiceKey, choice.correct() ? "CORRECT" : "INCORRECT",
                choice.feedback(), json(response), now);
        if (result != null) {
            jdbc.update("""
                    insert into training.session_results(session_id,organization_id,total_score,
                        passed,grade,error_count,completed_at) values(?,?,?,?,?,?,?)
                    """, id, actor.organizationId(), result.totalScore(), result.passed(),
                    result.grade(), result.errorCount(), now);
        }
        return response;
    }

    @Transactional
    public Map<String, Object> abandon(SessionPrincipal actor, UUID id) {
        Session session = ownSession(actor, id, true);
        if (session.status().equals("COMPLETED"))
            throw failure(409, "SESSION_CLOSED", "Completed training cannot be abandoned");
        if (session.status().equals("ABANDONED"))
            return map("id", id, "status", "ABANDONED", "endedAt", session.endedAt());
        OffsetDateTime now = now();
        jdbc.update("""
                update training.sessions set status='ABANDONED',ended_at=?
                where organization_id=? and id=?
                """, now, actor.organizationId(), id);
        return map("id", id, "status", "ABANDONED", "endedAt", now);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> session(SessionPrincipal actor, UUID id) {
        Session session = ownSession(actor, id, false);
        List<Map<String, Object>> actions = jdbc.query("""
                select action_sequence,action_type,target_key,choice_key,evaluation_status,
                    feedback_text,occurred_at from training.session_actions
                where organization_id=? and session_id=? order by action_sequence
                """, (rs, n) -> map("actionSequence", rs.getInt(1), "actionType", rs.getString(2),
                        "targetKey", rs.getString(3), "choiceKey", rs.getString(4),
                        "evaluationStatus", rs.getString(5), "feedback", rs.getString(6),
                        "occurredAt", rs.getObject(7, OffsetDateTime.class)),
                actor.organizationId(), id);
        Map<String, Object> body = map("id", id, "scenarioId", session.scenarioId(),
                "scenarioVersion", session.scenarioVersion(), "attemptNumber", session.attemptNumber(),
                "status", session.status(), "nextActionSequence", actions.size() + 1,
                "startedAt", session.startedAt(), "endedAt", session.endedAt(), "actions", actions);
        if (session.status().equals("COMPLETED")) body.put("result", result(id, actor.organizationId()));
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> results(SessionPrincipal actor, String scope, UUID learnerId,
            int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw failure(400, "INVALID_REQUEST", "Invalid page or size");
        String condition;
        List<Object> params = new ArrayList<>();
        params.add(actor.organizationId());
        switch (scope) {
            case "SELF" -> {
                if (learnerId != null && !learnerId.equals(actor.userId()))
                    throw failure(403, "FORBIDDEN", "Cannot read another learner's results");
                condition = " and s.learner_user_id=?";
                params.add(actor.userId());
            }
            case "STORE" -> {
                if (!actor.roles().contains("MANAGER")
                        || !actor.permissions().contains("training.results.read.store"))
                    throw failure(403, "FORBIDDEN", "Store result permission required");
                condition = """
                         and exists (select 1 from iam.user_roles ur
                            where ur.organization_id=s.organization_id
                              and ur.user_id=s.learner_user_id and ur.store_id=?)
                        """;
                params.add(actor.storeId());
                if (learnerId != null) { condition += " and s.learner_user_id=?"; params.add(learnerId); }
            }
            case "ORGANIZATION" -> {
                if (!actor.roles().contains("ADMIN")
                        || !actor.permissions().contains("training.results.read.organization"))
                    throw failure(403, "FORBIDDEN", "Organization result permission required");
                condition = learnerId == null ? "" : " and s.learner_user_id=?";
                if (learnerId != null) params.add(learnerId);
            }
            default -> throw failure(400, "INVALID_REQUEST", "Unknown result scope");
        }
        String from = """
                from training.session_results r
                join training.sessions s on s.id=r.session_id and s.organization_id=r.organization_id
                join training.scenarios c on c.id=s.scenario_id and c.organization_id=s.organization_id
                where s.organization_id=?
                """ + condition;
        long total = jdbc.queryForObject("select count(*) " + from, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add(page * size);
        var items = jdbc.query("""
                select s.id,c.code,s.scenario_version,s.learner_user_id,s.store_id,
                    s.attempt_number,r.total_score,r.passed,r.grade,r.completed_at
                """ + from + " order by r.completed_at desc,s.id limit ? offset ?",
                (rs, n) -> map("sessionId", rs.getObject(1, UUID.class), "scenarioCode", rs.getString(2),
                        "scenarioVersion", rs.getInt(3), "learnerId", rs.getObject(4, UUID.class),
                        "storeId", rs.getObject(5, UUID.class), "attemptNumber", rs.getInt(6),
                        "totalScore", rs.getInt(7), "passed", rs.getBoolean(8),
                        "grade", rs.getString(9), "completedAt", rs.getObject(10, OffsetDateTime.class)),
                pageParams.toArray());
        return map("items", items, "page", page, "size", size, "total", total);
    }

    private Map<String, Object> result(UUID sessionId, UUID orgId) {
        return jdbc.query("""
                select total_score,passed,grade,error_count,completed_at
                from training.session_results where organization_id=? and session_id=?
                """, (rs, n) -> map("totalScore", rs.getInt(1), "passed", rs.getBoolean(2),
                        "grade", rs.getString(3), "errorCount", rs.getInt(4),
                        "completedAt", rs.getObject(5, OffsetDateTime.class)), orgId, sessionId)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("Completed session has no result"));
    }

    private Session ownSession(SessionPrincipal actor, UUID id, boolean lock) {
        String sql = "select * from training.sessions where organization_id=? and id=? and learner_user_id=?"
                + (lock ? " for update" : "");
        return jdbc.query(sql, this::sessionRow, actor.organizationId(), id, actor.userId())
                .stream().findFirst().orElseThrow(() -> failure(404, "NOT_FOUND", "Training session not found"));
    }

    private Session sessionRow(ResultSet rs, int n) throws SQLException {
        return new Session(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                rs.getObject("store_id", UUID.class), rs.getObject("learner_user_id", UUID.class),
                rs.getObject("scenario_id", UUID.class), rs.getInt("scenario_version"),
                rs.getInt("attempt_number"), rs.getString("status"), rs.getInt("step_cursor"),
                rs.getInt("error_count"), rs.getString("steps_snapshot"),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("ended_at", OffsetDateTime.class));
    }

    private List<Step> loadSteps(UUID orgId, UUID scenarioId) {
        Map<Integer, MutableStep> steps = new LinkedHashMap<>();
        jdbc.query("""
                select st.step_order,st.stage,st.action_type,st.target_key,
                    ch.choice_key,ch.is_correct,ch.feedback_text
                from training.scenario_steps st
                left join training.scenario_choices ch on ch.organization_id=st.organization_id
                    and ch.scenario_id=st.scenario_id and ch.scenario_step_id=st.id
                where st.organization_id=? and st.scenario_id=?
                order by st.step_order,ch.choice_key
                """, rs -> {
            int order = rs.getInt(1);
            var step = steps.computeIfAbsent(order, ignored -> new MutableStep(order,
                    rsString(rs, 2), rsString(rs, 3), rsString(rs, 4)));
            String choiceKey = rs.getString(5);
            if (choiceKey != null) step.choices.put(choiceKey, new Choice(rs.getBoolean(6), rs.getString(7)));
        }, orgId, scenarioId);
        List<Step> snapshot = new ArrayList<>();
        int expected = 1;
        for (MutableStep step : steps.values()) {
            long correct = step.choices.values().stream().filter(Choice::correct).count();
            if (step.order != expected++ || correct != 1)
                throw failure(409, "SCENARIO_NOT_READY", "Scenario steps must be ordered with one correct choice");
            snapshot.add(new Step(step.order, step.stage, step.actionType,
                    step.targetKey, Map.copyOf(step.choices)));
        }
        return snapshot;
    }

    private static class MutableStep {
        final int order;
        final String stage, actionType, targetKey;
        final Map<String, Choice> choices = new HashMap<>();
        MutableStep(int order, String stage, String actionType, String targetKey) {
            this.order = order; this.stage = stage; this.actionType = actionType; this.targetKey = targetKey;
        }
    }

    private static String rsString(ResultSet rs, int column) {
        try { return rs.getString(column); }
        catch (SQLException e) { throw new IllegalStateException(e); }
    }

    private Map<String, Object> startResponse(Session s) {
        int next = jdbc.queryForObject("""
                select coalesce(max(action_sequence),0)+1 from training.session_actions where session_id=?
                """, Integer.class, s.id());
        return map("id", s.id(), "scenarioId", s.scenarioId(), "scenarioVersion", s.scenarioVersion(),
                "attemptNumber", s.attemptNumber(), "status", s.status(), "nextActionSequence", next,
                "startedAt", s.startedAt());
    }

    private static void requireTraining(SessionPrincipal actor) {
        if (!actor.trainingEnabled())
            throw failure(403, "TRAINING_DISABLED", "Training is disabled for this user");
    }

    private static void fields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) throw failure(400, "INVALID_REQUEST", "Object body required");
        body.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name))
                throw failure(400, "INVALID_REQUEST", "Unexpected field: " + name);
        });
    }

    private static UUID uuid(JsonNode body, String key) {
        try { return UUID.fromString(body.path(key).asText()); }
        catch (IllegalArgumentException e) { throw failure(400, "INVALID_REQUEST", "Invalid " + key); }
    }

    private static int positiveInt(JsonNode body, String key) {
        JsonNode node = body.path(key);
        if (!node.isIntegralNumber() || !node.canConvertToInt() || node.asInt() <= 0)
            throw failure(400, "INVALID_REQUEST", "Invalid " + key);
        return node.asInt();
    }

    private static String bounded(JsonNode body, String key, int length) {
        JsonNode node = body.path(key);
        if (!node.isTextual() || node.asText().isBlank() || node.asText().length() > length)
            throw failure(400, "INVALID_REQUEST", "Invalid " + key);
        return node.asText();
    }

    private static String optionalBounded(JsonNode body, String key, int length) {
        JsonNode node = body.path(key);
        return node.isMissingNode() || node.isNull() ? null : bounded(body, key, length);
    }

    private static TrainingFailure failure(int status, String code, String message) {
        return new TrainingFailure(status, code, message);
    }

    private OffsetDateTime now() {
        // PostgreSQL stores microseconds; keep first response and replay byte-equivalent.
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private List<Step> readSteps(String value) {
        try { return mapper.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private Map<String, Object> readMap(String value) {
        try { return mapper.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private static Map<String, Object> map(Object... entries) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) result.put((String) entries[i], entries[i + 1]);
        return result;
    }
}
