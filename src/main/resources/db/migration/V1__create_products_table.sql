-- Table du catalogue des produits vendus sur le site.
CREATE TABLE products (
    id          VARCHAR(64)    NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(2000)  NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    available   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_products PRIMARY KEY (id)
);

