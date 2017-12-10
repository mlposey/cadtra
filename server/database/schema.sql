CREATE TABLE users (
  id serial PRIMARY KEY,
  -- The api uses 'me' as a path and that could cause problems in the future
  -- if it allows GET requests by user name instead of id.
  -- TODO: This isn't optimal. Fix it.
  name text NOT NULL CHECK (name != 'me'),
  email text UNIQUE NOT NULL,
  avatar text NOT NULL,
  since timestamptz NOT NULL DEFAULT NOW(),
  country text
);

-- This is a bit empty now but could grow quite large, hence its own table.
CREATE TABLE preferences (
  user_id int PRIMARY KEY REFERENCES users(id),
  uses_metric boolean NOT NULL DEFAULT FALSE
);

-- create_user creates a new user with a default set of preferences.
CREATE OR REPLACE FUNCTION create_user(_email TEXT, _name TEXT, _country TEXT)
  RETURNS TABLE (user_id INT, since TIMESTAMPTZ, avatar TEXT)
AS $$
DECLARE
  _user_id INT;
  _since TIMESTAMPTZ;
  _avatar TEXT;
BEGIN
  INSERT INTO users (email, name, country, avatar)
  VALUES (_email, _name, _country, 'localhost:8080/avatars/default.png')
  RETURNING users.id, users.since, users.avatar
  INTO _user_id, _since, _avatar;

  INSERT INTO preferences (user_id, uses_metric)
  VALUES (_user_id, false);

  RETURN QUERY SELECT _user_id, _since, _avatar;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE clubs (
  id serial PRIMARY KEY,
  name text NOT NULL UNIQUE,
  avatar text NOT NULL,
  since timestamptz NOT NULL DEFAULT NOW(),
  owner_id int REFERENCES users(id)
);

CREATE TABLE club_members (
  club_id int NOT NULL REFERENCES clubs(id),
  user_id int NOT NULL REFERENCES users(id),
  role text NOT NULL,
  since timestamptz NOT NULL DEFAULT NOW(),
  PRIMARY KEY (club_id, user_id)
);

CREATE TABLE user_relation_types (
  relation text PRIMARY KEY
);
INSERT INTO user_relation_types (relation) VALUES
  ('friend'), ('blocked');

CREATE TABLE user_relations (
  id serial PRIMARY KEY,
  context text NOT NULL REFERENCES user_relation_types,
  sender_id int NOT NULL REFERENCES users(id),
  receiver_id int NOT NULL REFERENCES users(id),
  has_accepted boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE club_relations (
  id serial PRIMARY KEY,
  sender_id int NOT NULL REFERENCES clubs(id),
  receiver_id int NOT NULL REFERENCES users(id),
  has_accepted boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE logs (
  id serial PRIMARY KEY,
  user_id int NOT NULL REFERENCES users(id),
  started_at timestamptz NOT NULL,
  ended_at timestamptz NOT NULL,
  duration real NOT NULL,
  polyline text NOT NULL,
  distance real NOT NULL,
  split_interval real NOT NULL,
  splits real[] NOT NULL,
  comment text
);
