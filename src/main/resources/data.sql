-- Datos de ejemplo del inventario. Idempotente: se puede correr en cada arranque.
-- (application.yml -> spring.sql.init.mode=always). Requiere el constraint
-- unico uk_product_sku que crea Hibernate a partir de la entidad Product.

INSERT INTO inventory_product
    (sku, name, category, unit, unit_price, stock, min_stock, active, created_at, updated_at, version)
VALUES
    ('HAR-000',  'Harina 0000',            'Insumos',   'kg',     980.00, 120,  40, true, now(), now(), 0),
    ('AZU-REF',  'Azucar refinada',        'Insumos',   'kg',     870.00,  15,  25, true, now(), now(), 0),
    ('LEV-FRE',  'Levadura fresca',        'Insumos',   'kg',    2100.00,   8,  10, true, now(), now(), 0),
    ('MAN-PAN',  'Manteca pan',            'Insumos',   'kg',    3450.00,  30,  12, true, now(), now(), 0),
    ('CAF-GRA',  'Cafe en grano',          'Cafeteria', 'kg',    9800.00,  22,   8, true, now(), now(), 0),
    ('MED-DOC',  'Medialunas (docena)',    'Producto',  'docena', 4200.00,  40,  15, true, now(), now(), 0),
    ('PAN-MIG',  'Pan de miga',            'Producto',  'unidad', 2600.00,   6,  10, true, now(), now(), 0)
ON CONFLICT (sku) DO NOTHING;
