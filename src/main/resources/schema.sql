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