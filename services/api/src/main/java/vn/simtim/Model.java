package vn.simtim;

import jakarta.persistence.*;
import java.time.*;

public final class Model {
  @MappedSuperclass
  public abstract static class Identified {
    @Id public String id;
  }

  @Entity(name = "Account")
  @Table(name = "accounts")
  public static class Account extends Identified {
    public String name;
    public String role;
    public String password;
    public boolean active = true;
  }

  @Entity(name = "Category")
  @Table(name = "categories")
  public static class Category extends Identified {
    public String name;
    public boolean active = true;
  }

  @Entity(name = "Product")
  @Table(name = "products")
  public static class Product extends Identified {
    public String name;
    public String barcode;
    public String category;
    public String unit;
    public long price;
    public String emoji;
    public boolean active = true;
  }

  @Entity(name = "Supplier")
  @Table(name = "suppliers")
  public static class Supplier extends Identified {
    public String name;
    public String phone;
    public boolean active = true;
  }

  @Entity(name = "Batch")
  @Table(name = "batches")
  public static class Batch extends Identified {
    public String productId;
    public int quantity;
    public LocalDate expiry;
    @Version public long version;
  }

  @Entity(name = "Receipt")
  @Table(name = "receipts")
  public static class Receipt extends Identified {
    public String supplier;
    public String product;
    public String batchId;
    public int accepted;
    public int rejected;
    public String note;
    public String actor;
    public Instant at;
  }

  @Entity(name = "Invoice")
  @Table(name = "invoices")
  public static class Invoice extends Identified {
    public Instant at;
    public String staff;
    public String actor;
    public String method;
    public long total;
    public long tendered;
    public long changeDue;
  }

  @Entity(name = "InvoiceLine")
  @Table(name = "invoice_lines")
  public static class InvoiceLine extends Identified {
    public String invoiceId;
    public String productId;
    public String name;
    public int quantity;
    public long price;
    public long originalPrice;
  }

  @Entity(name = "Movement")
  @Table(name = "movements")
  public static class Movement extends Identified {
    public String productId;
    public String batchId;
    public int quantity;
    public String kind;
    public String reference;
    public Instant at;
    public String actor;
  }

  @Entity(name = "Promotion")
  @Table(name = "promotions")
  public static class Promotion extends Identified {
    public String productId;
    public int percent;

    @Column(name = "end_date")
    public LocalDate end;
  }

  @Entity(name = "Count")
  @Table(name = "stock_counts")
  public static class Count extends Identified {
    public String productId;
    public String batchId;
    public int expected;
    public int actual;
    public long baseVersion;
    public String note;
    public String status;
    public String actor;
    public Instant at;
  }

  @Entity(name = "StoreLock")
  @Table(name = "store_lock")
  public static class StoreLock extends Identified {}

  @Entity(name = "Operation")
  @Table(name = "operations")
  public static class Operation extends Identified {
    public String actor;

    @Column(columnDefinition = "text")
    public String requestBody;

    @Column(columnDefinition = "text")
    public String responseBody;
  }

  @Entity(name = "TrainingSession")
  @Table(name = "training_sessions")
  public static class TrainingSession extends Identified {
    public String actor;
    public int chapter;
    public String status;
    public String visited = "";
    public Instant startedAt;
    public Instant completedAt;
    public boolean orientationPassed;
  }
}
