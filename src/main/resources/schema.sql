-- Drop if you are iterating locally
DROP TABLE IF EXISTS prereq;
DROP TABLE IF EXISTS course;

CREATE TABLE course (
  code    TEXT PRIMARY KEY,        -- e.g., 'CSCI-P100'
  title   TEXT NOT NULL,
  credits INTEGER NOT NULL,
  core    INTEGER NOT NULL         -- 0/1 since SQLite has no boolean
);

CREATE TABLE prereq (
  id            TEXT PRIMARY KEY,  -- e.g., 'P200<-P150'
  course_code   TEXT NOT NULL,     -- target course
  requires_code TEXT NOT NULL,     -- prerequisite course
  FOREIGN KEY (course_code)   REFERENCES course(code),
  FOREIGN KEY (requires_code) REFERENCES course(code)
);

/* =========================
   Students & Profiles
   ========================= */
CREATE TABLE IF NOT EXISTS students (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  display_name TEXT,
  start_semester INTEGER NOT NULL,      -- 1..4 for MS (or 1..8 for UG)
  degree TEXT NOT NULL DEFAULT 'MS'     -- 'MS' or 'UG'
);

-- Courses already exist (course: code,title,credits,core)
-- Add offering flags to honor Fall/Spring scheduling.
ALTER TABLE course ADD COLUMN offered_fall INTEGER DEFAULT 1;   -- 1=true, 0=false
ALTER TABLE course ADD COLUMN offered_spring INTEGER DEFAULT 1; -- 1=true, 0=false

/* =========================
   Student history & plan
   ========================= */

-- Completed courses with the semester they were taken (1..4 for MS, 1..8 for UG)
CREATE TABLE IF NOT EXISTS completion (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL,
  course_code TEXT NOT NULL,
  semester_no INTEGER NOT NULL,     -- relative semester index within the program
  grade TEXT,
  FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Planned selections for each upcoming semester (server enforces 3..9 credits rule)
CREATE TABLE IF NOT EXISTS semester_plan (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL,
  semester_no INTEGER NOT NULL,     -- 1..4 (MS)
  course_code TEXT NOT NULL,
  UNIQUE(student_id, semester_no, course_code),
  FOREIGN KEY (student_id) REFERENCES students(id)
);

/* =========================
   Indices
   ========================= */
CREATE INDEX IF NOT EXISTS idx_completion_student ON completion(student_id);
CREATE INDEX IF NOT EXISTS idx_plan_student_sem ON semester_plan(student_id, semester_no);
CREATE INDEX IF NOT EXISTS idx_course_offering_f ON course(offered_fall);
CREATE INDEX IF NOT EXISTS idx_course_offering_s ON course(offered_spring);

/* =========================
   Requirement Groups (from earlier step)
   ========================= */
CREATE TABLE IF NOT EXISTS req_groups (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  required_credits INTEGER NOT NULL,
  rule_kind TEXT NOT NULL,            -- 'EXPLICIT_SET' | 'PREDICATE'
  choose_count INTEGER
);

CREATE TABLE IF NOT EXISTS req_group_courses (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  group_id INTEGER NOT NULL,
  course_code TEXT NOT NULL,
  FOREIGN KEY (group_id) REFERENCES req_groups(id)
);

CREATE INDEX IF NOT EXISTS idx_req_group_courses_gid ON req_group_courses(group_id);
CREATE INDEX IF NOT EXISTS idx_req_group_courses_code ON req_group_courses(course_code);

-- Degree requirement buckets (program-agnostic)
create table if not exists degree_group (
  code           text primary key,           -- e.g., FOUNDATIONS, SYSTEMS, CORE, CREATIVITY
  name           text not null,              -- display name
  min_credits    integer not null default 0, -- required credits
  max_credits    integer,                    -- optional cap
  choose_n       integer                     -- optional: choose N courses (alternative way to model)
);

-- Mapping from bucket to courses
create table if not exists degree_group_course (
  group_code   text not null references degree_group(code) on delete cascade,
  course_code  text not null references course(code) on delete cascade,
  primary key (group_code, course_code)
);