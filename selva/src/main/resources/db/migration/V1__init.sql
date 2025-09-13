CREATE TYPE selva.role AS enum ('USER', 'ADMIN');

CREATE TABLE selva."user"
(
    id                    bigserial PRIMARY KEY,
    username              text                  NOT NULL UNIQUE,
    password              text                  NOT NULL,
    "role"                selva.role            NOT NULL,
    finished_registration boolean DEFAULT FALSE NOT NULL
);

ALTER TABLE selva."user"
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE selva."user"
    FORCE ROW LEVEL SECURITY;

CREATE POLICY user_select ON selva."user" FOR SELECT
    USING (TRUE);
CREATE POLICY user_insert ON selva."user" FOR INSERT
    WITH CHECK (TRUE);
CREATE POLICY user_update ON selva."user" FOR UPDATE
    USING (id = nullif(current_setting('selva.current_user_id', TRUE), '')::bigint);
CREATE POLICY user_delete ON selva."user" FOR DELETE
    USING (id = nullif(current_setting('selva.current_user_id', TRUE), '')::bigint);

CREATE TABLE selva.base_profile
(
    id      bigserial PRIMARY KEY,
    user_id bigint REFERENCES selva."user" (id) ON DELETE CASCADE UNIQUE,
    fields  jsonb NOT NULL
);

ALTER TABLE selva.base_profile
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE selva.base_profile
    FORCE ROW LEVEL SECURITY;

CREATE POLICY base_profile_only_my_records ON selva.base_profile FOR ALL
    USING (base_profile.user_id = nullif(current_setting('selva.current_user_id', TRUE), '')::bigint);

CREATE POLICY base_profile_external_integration_select ON selva.base_profile FOR SELECT
    USING (nullif(current_setting('selva.current_external_integration_id', TRUE), '') IS NOT NULL);

CREATE TABLE selva.file
(
    id        bigserial PRIMARY KEY,
    file_name text  NULL,
    content   bytea NOT NULL
);

CREATE TABLE selva.external_integration
(
    id               bigserial PRIMARY KEY,
    name             text  NOT NULL UNIQUE,
    token            text  NOT NULL UNIQUE,
    logo             bytea NULL,
    profile_template jsonb
);

CREATE TABLE selva.external_profile
(
    id                      bigserial PRIMARY KEY,
    base_profile_id         bigint REFERENCES selva.base_profile (id) ON DELETE CASCADE,
    external_integration_id bigint REFERENCES selva.external_integration (id) ON DELETE CASCADE,
    is_public               boolean DEFAULT FALSE,
    fields                  jsonb NOT NULL
);

ALTER TABLE selva.external_profile
    ENABLE ROW LEVEL SECURITY;
ALTER TABLE selva.external_profile
    FORCE ROW LEVEL SECURITY;

CREATE POLICY external_profile_only_my_records ON selva.external_profile FOR ALL
    USING ((SELECT bp.user_id = nullif(current_setting('selva.current_user_id', TRUE), '')::bigint
            FROM selva.base_profile bp
            WHERE bp.id = external_profile.base_profile_id));

CREATE POLICY external_profile_current_integration ON selva.external_profile FOR SELECT
    USING (external_profile.external_integration_id =
           nullif(current_setting('selva.current_external_integration_id', TRUE), '')::bigint);

CREATE POLICY external_profile_is_public_for_integrations ON selva.external_profile FOR SELECT
    USING ((nullif(current_setting('selva.current_external_integration_id', TRUE), '')::bigint IS NOT NULL) AND is_public);

INSERT INTO selva."user"
VALUES (DEFAULT,
        'admin',
        '$2a$10$DAxtnVrb79kQdojesGqIBOt2ixrXhYCRQMwi9KyRGFzRRUrhysVRi',
        'ADMIN',
        TRUE);