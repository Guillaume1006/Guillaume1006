-- On-device SQLite (Room). No server, so no auth, no row level security:
-- one app, one user. Times are UTC epoch milliseconds. Ids are text (UUID v4
-- generated in the app) because SQLite has no gen_random_uuid().
-- Gains are whole dB steps, as seen on screen S02 (scale 8 / 0 / -8).

pragma foreign_keys = on;

create table eq_presets (
  id          text primary key,
  name        text not null,
  kind        text not null check (kind in ('builtin', 'custom')),
  -- extra output gain in dB for the "high volume" profile; 0 for the others
  loudness_db integer not null default 0 check (loudness_db between 0 and 12),
  created_at  integer not null,
  updated_at  integer not null
);
create unique index eq_presets_name on eq_presets (name);

create table eq_preset_bands (
  preset_id text    not null references eq_presets(id) on delete cascade,
  band_hz   integer not null check (band_hz in (100, 300, 1000, 3000, 10000)),
  gain_db   integer not null default 0 check (gain_db between -8 and 8),
  primary key (preset_id, band_hz)
);

-- the live setting: what the sliders show now, even if it matches no preset
create table eq_current (
  band_hz    integer primary key check (band_hz in (100, 300, 1000, 3000, 10000)),
  gain_db    integer not null default 0 check (gain_db between -8 and 8),
  updated_at integer not null
);

-- single row
create table app_settings (
  id               integer primary key check (id = 1),
  enabled          integer not null default 1 check (enabled in (0, 1)),
  active_preset_id text references eq_presets(id) on delete set null,
  loudness_db      integer not null default 0 check (loudness_db between 0 and 12),
  updated_at       integer not null
);

-- Built-in profiles: names follow the five seen in the recon (ON, OFF, Bass
-- Boost, High Volume, Vocal). The gain values below are our own starting
-- points, to be tuned by ear. They are not Cardo's values (unknown).
insert into eq_presets (id, name, kind, loudness_db, created_at, updated_at) values
  ('builtin-on',          'On',          'builtin',  0, 0, 0),
  ('builtin-off',         'Off',         'builtin',  0, 0, 0),
  ('builtin-bass-boost',  'Bass Boost',  'builtin',  0, 0, 0),
  ('builtin-high-volume', 'High Volume', 'builtin',  6, 0, 0),
  ('builtin-vocal',       'Vocal',       'builtin',  0, 0, 0);

insert into eq_preset_bands (preset_id, band_hz, gain_db) values
  ('builtin-on', 100, 0), ('builtin-on', 300, 0), ('builtin-on', 1000, 0), ('builtin-on', 3000, 0), ('builtin-on', 10000, 0),
  ('builtin-off', 100, 0), ('builtin-off', 300, 0), ('builtin-off', 1000, 0), ('builtin-off', 3000, 0), ('builtin-off', 10000, 0),
  ('builtin-bass-boost', 100, 6), ('builtin-bass-boost', 300, 3), ('builtin-bass-boost', 1000, 0), ('builtin-bass-boost', 3000, 0), ('builtin-bass-boost', 10000, 0),
  ('builtin-high-volume', 100, 0), ('builtin-high-volume', 300, 0), ('builtin-high-volume', 1000, 0), ('builtin-high-volume', 3000, 0), ('builtin-high-volume', 10000, 0),
  ('builtin-vocal', 100, -2), ('builtin-vocal', 300, 1), ('builtin-vocal', 1000, 3), ('builtin-vocal', 3000, 3), ('builtin-vocal', 10000, 0);

insert into eq_current (band_hz, gain_db, updated_at) values
  (100, 0, 0), (300, 0, 0), (1000, 0, 0), (3000, 0, 0), (10000, 0, 0);

insert into app_settings (id, enabled, active_preset_id, loudness_db, updated_at)
  values (1, 1, 'builtin-on', 0, 0);
