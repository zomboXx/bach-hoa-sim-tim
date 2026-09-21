package vn.simtim;

import jakarta.persistence.EntityManager;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSeed implements ApplicationRunner {
  final EntityManager em;
  final PasswordEncoder encoder;

  @Value("${app.seed-demo}")
  boolean seed;

  @Value("${app.demo-password}")
  String password;

  DemoSeed(EntityManager em, PasswordEncoder e) {
    this.em = em;
    encoder = e;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!seed) return;
    boolean existingStore = em.find(Model.Account.class, "QL001") != null;
    String[][] users = {
      {"NV001", "Lan Nguyễn", "sales"},
      {"KHO001", "Minh Trần", "stock"},
      {"QL001", "An Phạm", "manager"},
      {"ADMIN001", "Chủ cửa hàng", "admin"},
      {"KT001", "Thu Hà", "accountant"}
    };
    for (var u : users) {
      if (em.find(Model.Account.class, u[0]) != null) continue;
      var a = new Model.Account();
      a.id = u[0];
      a.name = u[1];
      a.role = u[2];
      a.password = encoder.encode(password);
      em.persist(a);
    }
    if (existingStore) return;
    String[] cats = {
      "Sữa & đồ uống", "Bánh kẹo", "Thực phẩm khô", "Chăm sóc nhà cửa", "Thực phẩm tươi"
    };
    for (int i = 0; i < cats.length; i++) {
      var c = new Model.Category();
      c.id = "DM0" + (i + 1);
      c.name = cats[i];
      em.persist(c);
    }
    em.flush();
    String[] names = {
      "Sữa tươi ít đường",
      "Bánh quy bơ",
      "Trà đào thanh mát",
      "Mì rau củ",
      "Nước giặt dịu nhẹ",
      "Trứng gà tươi"
    };
    String[] emoji = {"🥛", "🍪", "🍑", "🍜", "🫧", "🥚"};
    int[] ci = {0, 1, 0, 2, 3, 4};
    long[] prices = {8500, 32000, 16000, 6500, 89000, 28000};
    for (int i = 0; i < names.length; i++) {
      var p = new Model.Product();
      p.id = "SP00" + (i + 1);
      p.name = names[i];
      p.barcode = "89346731200" + (i + 1);
      p.category = cats[ci[i]];
      p.unit = i == 4 ? "Túi" : i == 1 || i == 3 ? "Gói" : "Hộp";
      p.price = prices[i];
      p.emoji = emoji[i];
      em.persist(p);
    }
    em.flush();
    int[] qs = {48, 32, 24, 80, 7, 12, 6}, days = {3, 90, 30, 120, 200, 6, -2};
    for (int i = 0; i < qs.length; i++) {
      var b = new Model.Batch();
      b.id = "LO0" + (i + 1);
      b.productId = "SP00" + (i == 6 ? 1 : i + 1);
      b.quantity = qs[i];
      b.expiry = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(days[i]);
      em.persist(b);
    }
    String[] sn = {"Phân phối An Nhiên", "Thực phẩm Vườn Nhà"};
    for (int i = 0; i < 2; i++) {
      var s = new Model.Supplier();
      s.id = "NCC0" + (i + 1);
      s.name = sn[i];
      s.phone = "090123456" + i;
      em.persist(s);
    }
    var p = new Model.Promotion();
    p.id = "KM01";
    p.productId = "SP001";
    p.percent = 10;
    p.end = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3);
    em.persist(p);
  }
}
