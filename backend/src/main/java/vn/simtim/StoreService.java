package vn.simtim;

import static vn.simtim.Model.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StoreService {
  @PersistenceContext EntityManager em;
  private final ObjectMapper json;

  public StoreService(ObjectMapper json) {
    this.json = json;
  }

  static String id(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }

  static LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
  }

  static void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  <T> T get(Class<T> type, String id) {
    T row = em.find(type, id);
    if (row == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu: " + id);
    return row;
  }

  <T> List<T> all(Class<T> type, String entity) {
    return em.createQuery("from " + entity, type).getResultList();
  }

  void lock() {
    get(StoreLock.class, "STORE01");
    em.find(StoreLock.class, "STORE01", LockModeType.PESSIMISTIC_WRITE);
  }

  Object command(String actor, String key, Object request, Supplier<Object> work) {
    require(key != null && key.matches("[A-Za-z0-9_-]{8,80}"), "Thiếu mã thao tác hợp lệ.");
    lock();
    try {
      String body = json.writeValueAsString(request);
      String operationId = actor + ":" + key;
      Operation existing = em.find(Operation.class, operationId);
      if (existing != null) {
        require(existing.requestBody.equals(body), "Mã thao tác đã được dùng với nội dung khác.");
        return json.readValue(existing.responseBody, Object.class);
      }
      Object result = work.get();
      Operation op = new Operation();
      op.id = operationId;
      op.actor = actor;
      op.requestBody = body;
      op.responseBody = json.writeValueAsString(result);
      em.persist(op);
      em.flush();
      return result;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Object> state(String actor, String role) {
    boolean management = Set.of("admin", "manager").contains(role);
    boolean finance = role.equals("accountant");
    boolean warehouse = role.equals("stock");
    List<Map<String, Object>> invoices = new ArrayList<>();
    for (Invoice i : all(Invoice.class, "Invoice"))
      if (management || finance || (role.equals("sales") && i.actor.equals(actor))) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", i.id);
        row.put("at", i.at);
        row.put("staff", i.staff);
        row.put("method", i.method);
        row.put("total", i.total);
        row.put("tendered", i.tendered);
        row.put("changeDue", i.changeDue);
        row.put(
            "lines",
            em.createQuery("from InvoiceLine where invoiceId=:id", InvoiceLine.class)
                .setParameter("id", i.id)
                .getResultList());
        invoices.add(row);
      }
    invoices.sort(Comparator.comparing(r -> r.get("at").toString(), Comparator.reverseOrder()));
    Map<String, Object> s = new LinkedHashMap<>();
    s.put("products", finance ? List.of() : all(Product.class, "Product"));
    s.put("categories", finance ? List.of() : all(Category.class, "Category"));
    s.put("suppliers", management || warehouse ? all(Model.Supplier.class, "Supplier") : List.of());
    s.put("batches", finance ? List.of() : all(Batch.class, "Batch"));
    s.put("invoices", invoices);
    s.put(
        "movements",
        management || warehouse
            ? em.createQuery("from Movement order by at desc", Movement.class).getResultList()
            : List.of());
    s.put("promotions", finance ? List.of() : all(Promotion.class, "Promotion"));
    s.put("counts", management ? all(Count.class, "Count") : List.of());
    s.put("receipts", management || warehouse ? all(Receipt.class, "Receipt") : List.of());
    return s;
  }

  public Object catalog(String kind, String id, Map<String, String> data, boolean deactivate) {
    lock();
    require(data.values().stream().noneMatch(Objects::isNull), "Trường dữ liệu không được null.");
    String name = data.getOrDefault("name", "").trim();
    if (!deactivate)
      require(!name.isBlank() && name.length() <= 120, "Tên phải có từ 1 đến 120 ký tự.");
    if (kind.equals("products")) {
      Product p = id == null ? new Product() : get(Product.class, id);
      if (deactivate) {
        p.active = false;
        return p;
      }
      String barcode = data.getOrDefault("barcode", "").trim();
      require(barcode.matches("[0-9]{8,14}"), "Mã vạch cần 8–14 chữ số.");
      long price;
      try {
        price = Long.parseLong(data.getOrDefault("price", "0"));
      } catch (NumberFormatException e) {
        price = 0;
      }
      require(price > 0 && price <= 100000000, "Giá bán phải là số nguyên từ 1 đến 100.000.000đ.");
      String category = data.getOrDefault("category", "");
      require(
          em.createQuery(
                      "select count(c) from Category c where c.name=:name and c.active=true",
                      Long.class)
                  .setParameter("name", category)
                  .getSingleResult()
              > 0,
          "Danh mục không tồn tại hoặc đã ngừng dùng.");
      require(
          em.createQuery(
                      "select count(p) from Product p where p.barcode=:barcode and p.id<>:id",
                      Long.class)
                  .setParameter("barcode", barcode)
                  .setParameter("id", id == null ? "" : id)
                  .getSingleResult()
              == 0,
          "Mã vạch đã tồn tại.");
      String unit = data.getOrDefault("unit", "").trim();
      require(!unit.isBlank() && unit.length() <= 40, "Đơn vị tính không hợp lệ.");
      p.name = name;
      p.barcode = barcode;
      p.category = category;
      p.price = price;
      p.unit = unit;
      p.emoji = "📦";
      if (id == null) {
        p.id = id("SP");
        em.persist(p);
      }
      return p;
    }
    if (kind.equals("suppliers")) {
      Model.Supplier s = id == null ? new Model.Supplier() : get(Model.Supplier.class, id);
      if (deactivate) {
        s.active = false;
        return s;
      }
      String phone = data.getOrDefault("phone", "").trim();
      require(phone.matches("[+0-9 ()-]{8,20}"), "Số điện thoại không hợp lệ.");
      s.name = name;
      s.phone = phone;
      if (id == null) {
        s.id = id("NCC");
        em.persist(s);
      }
      return s;
    }
    require(kind.equals("categories"), "Loại danh mục không hợp lệ.");
    Category c = id == null ? new Category() : get(Category.class, id);
    if (id != null)
      require(
          em.createQuery("select count(p) from Product p where p.category=:name", Long.class)
                  .setParameter("name", c.name)
                  .getSingleResult()
              == 0,
          "Danh mục đang được sản phẩm sử dụng; chuyển sản phẩm sang danh mục khác trước.");
    if (deactivate) {
      c.active = false;
      return c;
    }
    require(
        em.createQuery(
                    "select count(c) from Category c where c.name=:name and c.id<>:id", Long.class)
                .setParameter("name", name)
                .setParameter("id", id == null ? "" : id)
                .getSingleResult()
            == 0,
        "Tên danh mục đã tồn tại.");
    c.name = name;
    if (id == null) {
      c.id = id("DM");
      em.persist(c);
    }
    return c;
  }

  void movement(String actor, Batch b, int amount, String kind, String reference) {
    Movement m = new Movement();
    m.id = id("BD");
    m.actor = actor;
    m.at = Instant.now();
    m.productId = b.productId;
    m.batchId = b.id;
    m.quantity = amount;
    m.kind = kind;
    m.reference = reference;
    em.persist(m);
  }

  public Object sell(String actor, String key, ApiController.Sale request) {
    return command(
        actor,
        key,
        request,
        () -> {
          require(
              Set.of("Tiền mặt", "Chuyển khoản").contains(request.method()),
              "Phương thức không hợp lệ. Không xử lý thanh toán thật.");
          require(
              request.lines().stream().map(ApiController.Line::id).distinct().count()
                  == request.lines().size(),
              "Sản phẩm trong giỏ bị trùng.");
          Invoice i = new Invoice();
          i.id = id("HD");
          i.at = Instant.now();
          i.actor = actor;
          i.staff = get(Account.class, actor).name;
          i.method = request.method();
          List<InvoiceLine> lines = new ArrayList<>();
          List<Runnable> deductions = new ArrayList<>();
          for (ApiController.Line row : request.lines()) {
            Product p = get(Product.class, row.id());
            require(p.active, "Sản phẩm đã ngừng bán.");
            List<Batch> batches =
                em.createQuery(
                        "from Batch where productId=:id and expiry>=:today and quantity>0 order by"
                            + " expiry,id",
                        Batch.class)
                    .setParameter("id", p.id)
                    .setParameter("today", today())
                    .getResultList();
            require(
                batches.stream().mapToLong(b -> b.quantity).sum() >= row.quantity(),
                "Không đủ tồn còn hạn: " + p.name);
            int remaining = row.quantity();
            for (Batch b : batches) {
              int take = Math.min(remaining, b.quantity);
              remaining -= take;
              if (take > 0)
                deductions.add(
                    () -> {
                      b.quantity -= take;
                      movement(actor, b, -take, "Bán hàng", i.id);
                    });
            }
            int percent =
                em.createQuery(
                        "from Promotion where productId=:id and end>=:today", Promotion.class)
                    .setParameter("id", p.id)
                    .setParameter("today", today())
                    .getResultStream()
                    .map(x -> x.percent)
                    .findFirst()
                    .orElse(0);
            InvoiceLine line = new InvoiceLine();
            line.id = id("CT");
            line.invoiceId = i.id;
            line.productId = p.id;
            line.name = p.name;
            line.quantity = row.quantity();
            line.originalPrice = p.price;
            line.price = Math.round(p.price * (100 - percent) / 100.0);
            lines.add(line);
            i.total = Math.addExact(i.total, Math.multiplyExact(line.price, row.quantity()));
          }
          i.tendered = request.method().equals("Tiền mặt") ? request.tendered() : i.total;
          require(
              i.tendered >= i.total,
              "Số tiền khách đưa chưa đủ hoặc giá đã thay đổi. Hãy tải lại giỏ hàng.");
          i.changeDue = i.tendered - i.total;
          em.persist(i);
          em.flush();
          lines.forEach(em::persist);
          deductions.forEach(Runnable::run);
          return Map.of("id", i.id, "total", i.total, "changeDue", i.changeDue);
        });
  }

  public Object receive(String actor, String key, ApiController.Receiving r) {
    return command(
        actor,
        key,
        r,
        () -> {
          require(get(Product.class, r.product()).active, "Sản phẩm đã ngừng dùng.");
          require(get(Model.Supplier.class, r.supplier()).active, "Nhà cung cấp đã ngừng dùng.");
          require(!r.expiry().isBefore(today()), "Không nhận lô hết hạn.");
          require(r.accepted() <= r.delivered(), "Số nhận không được vượt số giao.");
          require(r.accepted() == r.delivered() || !r.note().isBlank(), "Cần lý do từ chối hàng.");
          require(em.find(Batch.class, r.lot()) == null, "Mã lô đã tồn tại.");
          Batch b = new Batch();
          b.id = r.lot();
          b.productId = r.product();
          b.quantity = r.accepted();
          b.expiry = r.expiry();
          em.persist(b);
          em.flush();
          Receipt row = new Receipt();
          row.id = id("NH");
          row.supplier = r.supplier();
          row.product = r.product();
          row.batchId = b.id;
          row.accepted = r.accepted();
          row.rejected = r.delivered() - r.accepted();
          row.note = r.note();
          row.actor = actor;
          row.at = Instant.now();
          em.persist(row);
          movement(actor, b, r.accepted(), "Nhận hàng", row.id);
          return Map.of("id", row.id);
        });
  }

  public Object count(String actor, String key, ApiController.CountRequest r) {
    return command(
        actor,
        key,
        r,
        () -> {
          Batch b = get(Batch.class, r.batchId());
          Count c = new Count();
          c.id = id("KK");
          c.actor = actor;
          c.at = Instant.now();
          c.batchId = b.id;
          c.productId = b.productId;
          c.expected = r.expected();
          c.actual = r.actual();
          c.baseVersion = r.baseVersion();
          c.note = r.note();
          c.status =
              b.version == r.baseVersion() && b.quantity == r.expected() ? "REVIEW" : "CONFLICT";
          em.persist(c);
          return Map.of("id", c.id, "status", c.status);
        });
  }

  public Object approve(String actor, String id) {
    lock();
    Count c = get(Count.class, id);
    require(c.status.equals("REVIEW"), "Phiếu không ở trạng thái chờ duyệt.");
    Batch b = get(Batch.class, c.batchId);
    if (b.version != c.baseVersion || b.quantity != c.expected) {
      c.status = "CONFLICT";
      return Map.of("status", c.status);
    }
    movement(actor, b, c.actual - b.quantity, "Kiểm kê", c.id);
    b.quantity = c.actual;
    c.status = "APPROVED";
    return Map.of("status", c.status);
  }

  public Object promote(ApiController.Discount r) {
    lock();
    require(get(Product.class, r.productId()).active, "Sản phẩm đã ngừng bán.");
    require(!r.end().isBefore(today()), "Ngày kết thúc đã qua.");
    Promotion p =
        em.createQuery("from Promotion where productId=:id", Promotion.class)
            .setParameter("id", r.productId())
            .getResultStream()
            .findFirst()
            .orElseGet(
                () -> {
                  Promotion n = new Promotion();
                  n.id = id("KM");
                  n.productId = r.productId();
                  em.persist(n);
                  return n;
                });
    p.percent = r.percent();
    p.end = r.end();
    return p;
  }

  public static final List<String> STATIONS =
      List.of("mentor", "checkout", "shelves", "chilled", "receiving", "carts");

  public Object startTraining(String actor, int chapter) {
    require(chapter == 0, "Nội dung này đang được cập nhật, vui lòng thử lại sau");
    TrainingSession s = new TrainingSession();
    s.id = id("TRAIN");
    s.actor = actor;
    s.chapter = 0;
    s.status = "ACTIVE";
    s.startedAt = Instant.now();
    em.persist(s);
    return s;
  }

  public Object trainingEvent(String actor, String id, String station, boolean complete) {
    TrainingSession s = em.find(TrainingSession.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (s == null || !s.actor.equals(actor))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên đào tạo.");
    Set<String> visited = new LinkedHashSet<>(Arrays.asList(s.visited.split(",")));
    visited.remove("");
    if (complete) {
      require(visited.containsAll(STATIONS), "Hãy khám phá đủ 6 điểm trong cửa hàng.");
      require(s.orientationPassed, "Hãy hoàn thành phần xác nhận cuối buổi cùng mentor.");
      s.status = "COMPLETED";
      if (s.completedAt == null) s.completedAt = Instant.now();
    } else {
      require(STATIONS.contains(station), "Điểm tương tác không hợp lệ.");
      visited.add(station);
      s.visited = String.join(",", visited);
    }
    return s;
  }

  public Object orientation(String actor, String id, ApiController.Orientation answers) {
    TrainingSession s = em.find(TrainingSession.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (s == null || !s.actor.equals(actor))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên đào tạo.");
    require(
        new HashSet<>(Arrays.asList(s.visited.split(","))).containsAll(STATIONS),
        "Hãy khám phá đủ 6 khu vực trước.");
    require(
        answers.welcome().equals("checkout")
            && answers.sensitive().equals("manager")
            && answers.delivery().equals("receiving"),
        "Chưa đúng. Hãy xem lại khu vực và quyền duyệt của quản lý.");
    s.orientationPassed = true;
    return Map.of("orientationPassed", true);
  }
}
