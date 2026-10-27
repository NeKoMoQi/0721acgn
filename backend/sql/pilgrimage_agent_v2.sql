-- PostgreSQL: merge this migration before deploying the v2 agent. No existing data is removed.
BEGIN;
CREATE SCHEMA IF NOT EXISTS acgn0721_gis;
CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_pilgrimage_alias (
    kind varchar(16) NOT NULL CHECK (kind IN ('work','city')),
    alias varchar(80) NOT NULL,
    canonical varchar(120) NOT NULL,
    PRIMARY KEY (kind, alias, canonical)
);
CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_pilgrimage_spot_info (
    point_id bigint PRIMARY KEY,
    access_status varchar(16) NOT NULL DEFAULT 'unknown' CHECK (access_status IN ('unknown','public','restricted','closed')),
    photo_note varchar(1000) NOT NULL DEFAULT '',
    source_url varchar(2000) NOT NULL DEFAULT '',
    verified_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);
COMMENT ON COLUMN acgn0721_gis.tb_pilgrimage_spot_info.point_id IS 'Logical reference to tb_xunlipoint.id; no coordinate or original point record is duplicated.';
CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_pilgrimage_route (
    id uuid PRIMARY KEY,
    owner_id varchar(128) NOT NULL,
    title varchar(100) NOT NULL,
    plan jsonb NOT NULL CHECK (jsonb_typeof(plan) = 'object'),
    plan_hash varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (owner_id, plan_hash)
);
CREATE INDEX IF NOT EXISTS pilgrimage_route_owner_updated ON acgn0721_gis.tb_pilgrimage_route(owner_id,updated_at DESC);
CREATE TABLE IF NOT EXISTS acgn0721_gis.tb_pilgrimage_session (
    id uuid PRIMARY KEY,
    owner_id varchar(128) NOT NULL,
    mode varchar(8) NOT NULL CHECK (mode IN ('route','chat')),
    payload jsonb NOT NULL,
    version bigint NOT NULL DEFAULT 1,
    expires_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS pilgrimage_session_expiry ON acgn0721_gis.tb_pilgrimage_session(expires_at);
INSERT INTO acgn0721_gis.tb_pilgrimage_alias(kind,alias,canonical) VALUES
 ('work','京吹','上低音号'),('work','響け','上低音号'),('work','氷菓','冰菓'),
 ('work','けいおん','轻音'),('work','K-ON','轻音'),('work','たまこ','玉子'),
 ('city','東京','东京'),('city','鎌倉','镰仓'),('city','京都市','京都'),('city','宇治市','宇治')
ON CONFLICT DO NOTHING;
COMMIT;
