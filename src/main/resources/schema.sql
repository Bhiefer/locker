PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS locations (
                                         id          INTEGER PRIMARY KEY,
                                         code        TEXT NOT NULL UNIQUE,
                                         name        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
                                        id          INTEGER PRIMARY KEY,
                                        sku         TEXT NOT NULL UNIQUE,
                                        name        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_variations (
                                                  id               INTEGER PRIMARY KEY,
                                                  product_id       INTEGER NOT NULL REFERENCES products(id),
    location_id      INTEGER NOT NULL REFERENCES locations(id),
    code             TEXT NOT NULL UNIQUE,
    woo_variation_id INTEGER,
    UNIQUE(product_id, location_id)
    );

CREATE TABLE IF NOT EXISTS boxes (
                                     id                    INTEGER PRIMARY KEY,
                                     location_id           INTEGER NOT NULL REFERENCES locations(id),
    product_variation_id  INTEGER NOT NULL REFERENCES product_variations(id),
    box_no                INTEGER NOT NULL,
    state                 TEXT NOT NULL CHECK (state IN ('EMPTY','AVAILABLE','RESERVED','OUT_OF_SERVICE')),
    last_change_at        TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(location_id, box_no)
    );

CREATE INDEX IF NOT EXISTS idx_boxes_by_variation_state ON boxes(product_variation_id, state);

CREATE TABLE IF NOT EXISTS orders (
                                      id             INTEGER PRIMARY KEY,
                                      woo_order_id   INTEGER NOT NULL UNIQUE,
                                      location_id    INTEGER NOT NULL REFERENCES locations(id),
    status         TEXT NOT NULL CHECK (status IN ('PAID_RESERVED','PICKED_UP','EXPIRED','CANCELLED')),
    reserved_at    TEXT NOT NULL DEFAULT (datetime('now')),
    expires_at     TEXT NOT NULL,
    picked_up_at   TEXT
    );

CREATE INDEX IF NOT EXISTS idx_orders_by_status_expires ON orders(status, expires_at);

CREATE TABLE IF NOT EXISTS allocations (
                                           id              INTEGER PRIMARY KEY,
                                           order_id        INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    box_id          INTEGER NOT NULL REFERENCES boxes(id),
    pin_code        TEXT NOT NULL, -- "0000".."9999"
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts    INTEGER NOT NULL DEFAULT 5,
    reserved_at     TEXT NOT NULL DEFAULT (datetime('now')),
    opened_at       TEXT,
    UNIQUE(order_id, box_id)
    );

CREATE INDEX IF NOT EXISTS idx_allocations_by_order ON allocations(order_id);
CREATE INDEX IF NOT EXISTS idx_allocations_by_box ON allocations(box_id);
