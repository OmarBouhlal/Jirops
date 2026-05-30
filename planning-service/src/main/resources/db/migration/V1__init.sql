-- planning-service/src/main/resources/db/migration/V1__init.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE sprint_status AS ENUM ('PLANNING', 'ACTIVE', 'CLOSED');

CREATE TABLE sprints (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  project_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  goal TEXT,
  start_date DATE,
  end_date DATE,
  status sprint_status NOT NULL DEFAULT 'PLANNING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sprint_tasks (
  sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
  task_id UUID NOT NULL,
  PRIMARY KEY (sprint_id, task_id)
);
