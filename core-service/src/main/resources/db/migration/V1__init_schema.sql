CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE pantry_item (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             VARCHAR(200)   NOT NULL,
    quantity         NUMERIC(12, 3) NOT NULL,
    unit             VARCHAR(30)    NOT NULL,
    category         VARCHAR(50),
    category_source  VARCHAR(10),
    expiry_date      DATE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pantry_item_category_source_chk
        CHECK (category_source IS NULL OR category_source IN ('user', 'ai'))
);

CREATE INDEX idx_pantry_item_user_id            ON pantry_item (user_id);
CREATE INDEX idx_pantry_item_user_id_expiry     ON pantry_item (user_id, expiry_date);

CREATE TABLE recipe (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title              VARCHAR(200) NOT NULL,
    instructions       TEXT         NOT NULL,
    cook_time_minutes  INTEGER,
    tags               TEXT[],
    source             VARCHAR(20),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT recipe_cook_time_chk
        CHECK (cook_time_minutes IS NULL OR cook_time_minutes >= 0),
    CONSTRAINT recipe_source_chk
        CHECK (source IS NULL OR source IN ('user', 'imported'))
);

CREATE INDEX idx_recipe_user_id ON recipe (user_id);

CREATE TABLE recipe_ingredient (
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT         NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    name        VARCHAR(200)   NOT NULL,
    quantity    NUMERIC(12, 3),
    unit        VARCHAR(30),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipe_ingredient_recipe_id ON recipe_ingredient (recipe_id);

CREATE TABLE shopping_list (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_shopping_list_user_id ON shopping_list (user_id);

CREATE TABLE shopping_list_item (
    id                BIGSERIAL PRIMARY KEY,
    shopping_list_id  BIGINT         NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
    name              VARCHAR(200)   NOT NULL,
    quantity          NUMERIC(12, 3),
    unit              VARCHAR(30),
    is_checked        BOOLEAN        NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_shopping_list_item_list_id ON shopping_list_item (shopping_list_id);
