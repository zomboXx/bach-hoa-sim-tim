package vn.simtim.api.catalog.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.ProductPriceRepository;

@Repository
class JdbcProductPriceRepository implements ProductPriceRepository {

    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    JdbcProductPriceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OptionalLong findCurrent(UUID organizationId, UUID storeId, UUID productId, Instant at) {
        var prices = jdbc.queryForList("""
                select sale_price
                from catalog.product_prices
                where organization_id = ? and store_id = ? and product_id = ?
                  and effective_from <= ?
                  and (effective_to is null or effective_to > ?)
                order by effective_from desc
                limit 1
                """, organizationId, storeId, productId, Timestamp.from(at), Timestamp.from(at));
        if (prices.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(((Number) prices.getFirst().get("sale_price")).longValue());
    }

    @Override
    public void setCurrent(UUID organizationId, UUID storeId, UUID productId,
                           long salePrice, Instant effectiveFrom) {
        // ProductService saves through JPA before this JDBC write. Flush first so the
        // product foreign key is visible to PostgreSQL inside the same transaction.
        entityManager.flush();
        Instant at = effectiveFrom.truncatedTo(ChronoUnit.MICROS);
        Optional<CurrentPrice> current = jdbc.query("""
                select id, sale_price, effective_from
                from catalog.product_prices
                where organization_id = ? and store_id = ? and product_id = ?
                  and effective_from <= ?
                  and (effective_to is null or effective_to > ?)
                order by effective_from desc
                limit 1
                for update
                """, (rs, row) -> new CurrentPrice(
                        rs.getObject("id", UUID.class),
                        rs.getLong("sale_price"),
                        rs.getTimestamp("effective_from").toInstant()),
                organizationId, storeId, productId, Timestamp.from(at), Timestamp.from(at))
                .stream().findFirst();

        if (current.isPresent() && current.get().salePrice() == salePrice) {
            return;
        }

        if (current.isPresent() && !current.get().effectiveFrom().isBefore(at)) {
            jdbc.update("update catalog.product_prices set sale_price = ? where id = ?",
                    salePrice, current.get().id());
            return;
        }

        current.ifPresent(price -> jdbc.update(
                "update catalog.product_prices set effective_to = ? where id = ?",
                Timestamp.from(at), price.id()));

        Instant nextEffectiveFrom = jdbc.query("""
                select min(effective_from) as effective_from
                from catalog.product_prices
                where organization_id = ? and store_id = ? and product_id = ?
                  and effective_from > ?
                """, rs -> rs.next() && rs.getTimestamp("effective_from") != null
                        ? rs.getTimestamp("effective_from").toInstant()
                        : null,
                organizationId, storeId, productId, Timestamp.from(at));

        jdbc.update("""
                insert into catalog.product_prices(
                    id, organization_id, store_id, product_id, sale_price, effective_from, effective_to)
                values (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), organizationId, storeId, productId, salePrice,
                Timestamp.from(at), nextEffectiveFrom == null ? null : Timestamp.from(nextEffectiveFrom));
    }

    @Override
    public void deleteByProduct(UUID organizationId, UUID productId) {
        jdbc.update("delete from catalog.product_prices where organization_id = ? and product_id = ?",
                organizationId, productId);
    }

    private record CurrentPrice(UUID id, long salePrice, Instant effectiveFrom) {}
}
