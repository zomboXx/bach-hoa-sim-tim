select
  (select count(*) from inventory.goods_receipts
   where organization_id = '10000000-0000-0000-0000-000000000001'
     and store_id = '10000000-0000-0000-0000-000000000002'),
  (select count(*) from sales.invoices
   where organization_id = '10000000-0000-0000-0000-000000000001'
     and store_id = '10000000-0000-0000-0000-000000000002'
     and status = 'COMPLETED'),
  (select count(*) from sales.payments
   where organization_id = '10000000-0000-0000-0000-000000000001'
     and store_id = '10000000-0000-0000-0000-000000000002'
     and status = 'COMPLETED'),
  (with movement_totals as (
     select organization_id, store_id, product_id, batch_id, sum(quantity_delta) as quantity
     from inventory.stock_movements
     where organization_id = '10000000-0000-0000-0000-000000000001'
       and store_id = '10000000-0000-0000-0000-000000000002'
     group by organization_id, store_id, product_id, batch_id
   )
   select count(*)
   from inventory.inventory_balances balance
   full outer join movement_totals movement
     on movement.organization_id = balance.organization_id
    and movement.store_id = balance.store_id
    and movement.product_id = balance.product_id
    and movement.batch_id = balance.batch_id
   where coalesce(balance.organization_id, movement.organization_id)
       = '10000000-0000-0000-0000-000000000001'
     and coalesce(balance.store_id, movement.store_id)
       = '10000000-0000-0000-0000-000000000002'
     and coalesce(balance.on_hand_quantity, 0) <> coalesce(movement.quantity, 0)),
  (select coalesce(sum(grand_total), 0) from sales.invoices
   where organization_id = '10000000-0000-0000-0000-000000000001'
     and store_id = '10000000-0000-0000-0000-000000000002'
     and status = 'COMPLETED');
