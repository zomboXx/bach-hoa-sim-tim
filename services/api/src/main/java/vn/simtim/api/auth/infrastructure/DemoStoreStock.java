package vn.simtim.api.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Extra demo stock, written in the DemoAccounts transaction and never replenished on restart. */
class DemoStoreStock {
    private final JdbcTemplate jdbc;

    DemoStoreStock(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private record Product(String sku, String name, String barcode, int quantity,
                           long unitCost, long salePrice, int expiryDays) {}

    private static final List<Product> PRODUCTS = List.of(
            new Product("ST-MILK-01", "Sữa tươi ít đường 180 ml", "8934673120010", 120, 5500, 8000, 90),
            new Product("ST-COOKIE-01", "Bánh quy bơ 200 g", "8934673120027", 60, 24000, 32000, 180),
            new Product("ST-NOODLE-01", "Mì ăn liền 75 g", "ST-NOODLE-01", 150, 3500, 5000, 180),
            new Product("ST-OIL-01", "Dầu ăn 1 lít", "ST-OIL-01", 40, 36000, 48000, 365),
            new Product("ST-SUGAR-01", "Đường trắng 1 kg", "ST-SUGAR-01", 80, 17000, 23000, 0),
            new Product("ST-SALT-01", "Muối iốt 500 g", "001000000008", 60, 4000, 6000, 0),
            new Product("ST-WATER-01", "Nước suối 500 ml", "ST-WATER-01", 200, 3000, 5000, 365),
            new Product("ST-COFFEE-01", "Cà phê rang xay 250 g", "ST-COFFEE-01", 45, 45000, 60000, 180));

    void seed(UUID org, UUID store, UUID actor) {
        var today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID receipt = id(org, store, "receipt", "pos-expansion-v1");
        // A separate receipt preserves the original rice/apple receipt and existing sales history.
        jdbc.update("""
                insert into inventory.goods_receipts
                    (id,organization_id,store_id,supplier_id,status,received_at,confirmed_by,
                     client_operation_id,idempotency_key,payload_hash)
                values(?,?,?,'10000000-0000-0000-0000-000000000061','CONFIRMED',now(),?,?,?,'\\x00'::bytea)
                on conflict do nothing
                """, receipt, org, store, actor,
                id(org, store, "receipt-operation", "pos-expansion-v1"),
                id(org, store, "receipt-key", "pos-expansion-v1"));
        for (var product : PRODUCTS) {
            jdbc.update("""
                    insert into catalog.products
                        (id,organization_id,category_id,base_unit_id,sku,name,tracks_expiry,status)
                    values(?,?,'10000000-0000-0000-0000-000000000021',
                           '10000000-0000-0000-0000-000000000031',?,?,?,'ACTIVE')
                    on conflict do nothing
                    """, id(org, store, "product", product.sku()), org,
                    product.sku(), product.name(), product.expiryDays() > 0);
            UUID productId = jdbc.queryForObject(
                    "select id from catalog.products where organization_id=? and lower(sku)=lower(?)",
                    UUID.class, org, product.sku());
            jdbc.update("""
                    insert into catalog.product_barcodes(id,organization_id,product_id,barcode,is_primary)
                    values(?,?,?,?,true) on conflict do nothing
                    """, id(org, store, "barcode", product.sku()), org, productId, product.barcode());
            jdbc.update("""
                    insert into catalog.product_prices(id,organization_id,store_id,product_id,sale_price,effective_from)
                    values(?,?,?,?,?,'2020-01-01T00:00:00Z') on conflict do nothing
                    """, id(org, store, "price", product.sku()), org, store, productId, product.salePrice());
            UUID line = id(org, store, "receipt-line", product.sku());
            UUID batch = id(org, store, "batch", product.sku());
            var expiry = product.expiryDays() == 0 ? null : java.sql.Date.valueOf(today.plusDays(product.expiryDays()));
            jdbc.update("""
                    insert into inventory.goods_receipt_lines
                        (id,receipt_id,organization_id,product_id,expected_quantity,delivered_quantity,
                         accepted_quantity,rejected_quantity,unit_cost,expiry_date)
                    values(?,?,?,?,?,?,?,0,?,?) on conflict do nothing
                    """, line, receipt, org, productId, product.quantity(), product.quantity(),
                    product.quantity(), product.unitCost(), expiry);
            jdbc.update("""
                    insert into inventory.product_batches
                        (id,organization_id,store_id,product_id,receipt_line_id,batch_number,
                         received_date,expiry_date,status)
                    values(?,?,?,?,?,?,?,?,'AVAILABLE') on conflict do nothing
                    """, batch, org, store, productId, line, "DEMO-" + product.sku(),
                    java.sql.Date.valueOf(today), expiry);
            jdbc.update("""
                    insert into inventory.inventory_balances
                        (id,organization_id,store_id,product_id,batch_id,on_hand_quantity)
                    values(?,?,?,?,?,?) on conflict do nothing
                    """, id(org, store, "balance", product.sku()), org, store, productId, batch, product.quantity());
            jdbc.update("""
                    insert into inventory.stock_movements
                        (id,organization_id,store_id,product_id,batch_id,movement_type,
                         quantity_delta,reference_id,reference_type,occurred_at,recorded_by)
                    values(?,?,?,?,?,'RECEIPT',?,?,'GOODS_RECEIPT',now(),?) on conflict do nothing
                    """, id(org, store, "movement", product.sku()), org, store, productId, batch,
                    product.quantity(), line, actor);
        }
    }

    private static UUID id(UUID org, UUID store, String kind, String sku) {
        return UUID.nameUUIDFromBytes(("simtim-demo-stock-v1:" + org + ":" + store + ":" + kind + ":" + sku)
                .getBytes(StandardCharsets.UTF_8));
    }
}
