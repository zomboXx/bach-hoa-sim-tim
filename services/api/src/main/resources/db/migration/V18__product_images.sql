-- Add image columns to products for catalog image upload
ALTER TABLE catalog.products
    ADD COLUMN IF NOT EXISTS image_url text,
    ADD COLUMN IF NOT EXISTS image_data bytea,
    ADD COLUMN IF NOT EXISTS image_content_type varchar(100);
