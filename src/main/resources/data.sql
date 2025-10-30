/* ==========================================================
   DEGREE PLANNER — SEED DATA (SQLite)
   This file is safe to re-run (INSERT OR IGNORE).
   ========================================================== */

/* -------------------------
   Students (demo accounts)
   ------------------------- */
INSERT OR IGNORE INTO students(id, username, display_name, start_semester, degree)
VALUES
  (1, 'student1', 'Alice Student (MS CS)', 1, 'MS'),
  (2, 'student2', 'Bob Student (MS CS)',   3, 'MS');

/* --------------------------------
   Courses (23 total, 3 credits ea)
   core = 1 if we want to label "Core"
   offered_fall / offered_spring:
     1 = offered, 0 = not offered, NULL = unknown (we normalize below)
   -------------------------------- */

-- Undergrad-ish / intro sequence used for sample prereq chain
INSERT OR IGNORE INTO course(code, title, credits, core) VALUES
 ('CSCI-P100','Applied Algorithms',3,0),
 ('CSCI-P101','Software Engineering 1',3,0),
 ('CSCI-P150','Applied Machine Learning',3,0),
 ('CSCI-P200','Software Engineering 2',3,0),
 ('CSCI-P221','Security Network Analysis',3,0),
 ('CSCI-P250','Advanced Database Concepts',3,0),
 ('CSCI-P300','Advanced Operating System',3,1),
 ('CSCI-P301','Computer Networks',3,1),
 ('ENGR-P210','Cloud Computing',3,0);

-- Graduate Foundations options (choose 1)
INSERT OR IGNORE INTO course(code, title, credits, core) VALUES
 ('CSCI-B501','Theory of Computing',3,1),
 ('CSCI-B503','Algorithm Design and Analysis',3,1),
 ('CSCI-B505','Applied Algorithms (Graduate)',3,1);

-- Graduate Systems options (choose 1)
INSERT OR IGNORE INTO course(code, title, credits, core) VALUES
 ('CSCI-P536','Advanced Operating Systems',3,1),
 ('CSCI-P538','Computer Networks (Graduate)',3,1),
 ('CSCI-B547','Distributed Systems',3,1);

-- Grad electives / core pool
INSERT OR IGNORE INTO course(code, title, credits, core) VALUES
 ('CSCI-B546','Cloud Computing (Graduate)',3,0),
 ('CSCI-B561','Advanced Database Systems',3,1),
 ('CSCI-B555','Machine Learning (Graduate)',3,0),
 ('CSCI-B565','Data Mining',3,0),
 ('CSCI-B644','Computer Security',3,0),
 ('CSCI-B657','Computer Vision',3,0),
 ('INFO-I523','Big Data Applications',3,0),
 ('INFO-I513','User Experience Design',3,0);

-- Normalize offering flags: default to offered both terms unless set
UPDATE course SET offered_fall   = 1 WHERE offered_fall   IS NULL;
UPDATE course SET offered_spring = 1 WHERE offered_spring IS NULL;

-- Optional flavor: keep a few term-specific (tweak as you like)
-- Distributed Systems mostly Fall; Adv DB mostly Spring
UPDATE course SET offered_fall=1, offered_spring=0 WHERE code IN ('CSCI-B547');
UPDATE course SET offered_fall=0, offered_spring=1 WHERE code IN ('CSCI-B561');

/* -------------
   Prerequisites
   ------------- */
-- Simple chain for the intro sequence
INSERT OR IGNORE INTO prereq(id, course_code, requires_code) VALUES
 ('P150<-P100','CSCI-P150','CSCI-P100'),
 ('P200<-P150','CSCI-P200','CSCI-P150'),
 ('P250<-P150','CSCI-P250','CSCI-P150'),
 ('P301<-P200','CSCI-P301','CSCI-P200'),
 ('P300<-P200','CSCI-P300','CSCI-P200');

-- Grad systems depend on Algorithms (B503)
INSERT OR IGNORE INTO prereq(id, course_code, requires_code) VALUES
 ('P536<-B503','CSCI-P536','CSCI-B503'),
 ('P538<-B503','CSCI-P538','CSCI-B503');

-- Distributed Systems after Adv OS
INSERT OR IGNORE INTO prereq(id, course_code, requires_code) VALUES
 ('B547<-P536','CSCI-B547','CSCI-P536');

-- Advanced DB after DB concepts; ML/Data Mining/Security/Vision after Algorithms
INSERT OR IGNORE INTO prereq(id, course_code, requires_code) VALUES
 ('B561<-P250','CSCI-B561','CSCI-P250'),
 ('B555<-B503','CSCI-B555','CSCI-B503'),
 ('B565<-B503','CSCI-B565','CSCI-B503'),
 ('B644<-B503','CSCI-B644','CSCI-B503'),
 ('B657<-B503','CSCI-B657','CSCI-B503');

/* ---------------------------
   Degree buckets (MS program)
   --------------------------- */
INSERT OR IGNORE INTO degree_group(code, name, min_credits, max_credits, choose_n) VALUES
 ('FOUNDATIONS','Foundations',3,3,1),
 ('SYSTEMS','Systems',3,3,1),
 ('CORE','CS Core',15,15,NULL),
 ('CREATIVITY','Creativity',9,9,NULL);

-- Foundations (choose one)
INSERT OR IGNORE INTO degree_group_course(group_code, course_code) VALUES
 ('FOUNDATIONS','CSCI-B501'),
 ('FOUNDATIONS','CSCI-B503'),
 ('FOUNDATIONS','CSCI-B505');

-- Systems (choose one)
INSERT OR IGNORE INTO degree_group_course(group_code, course_code) VALUES
 ('SYSTEMS','CSCI-P536'),
 ('SYSTEMS','CSCI-P538'),
 ('SYSTEMS','CSCI-B547');     -- allow Distributed Systems as a Systems pick

-- CS Core pool (15 cr) — include strong 500-level selection
INSERT OR IGNORE INTO degree_group_course(group_code, course_code) VALUES
 ('CORE','CSCI-P200'),
 ('CORE','CSCI-P250'),
 ('CORE','CSCI-P301'),
 ('CORE','CSCI-B546'),
 ('CORE','CSCI-B561'),
 ('CORE','CSCI-B555'),
 ('CORE','CSCI-B565'),
 ('CORE','CSCI-B644'),
 ('CORE','CSCI-B657'),
 -- If you prefer Systems to count ONLY in Systems, remove the next two:
 ('CORE','CSCI-P536'),
 ('CORE','CSCI-P538');

-- Creativity (9 cr) — Luddy 500+ outside strict core
INSERT OR IGNORE INTO degree_group_course(group_code, course_code) VALUES
 ('CREATIVITY','ENGR-P210'),
 ('CREATIVITY','INFO-I523'),
 ('CREATIVITY','INFO-I513'),
 ('CREATIVITY','CSCI-B546');  -- allow Cloud to count here as well

/* -------------------------------------------
   Sample completion history for student2 (Sem 1–2)
   ------------------------------------------- */
-- student2 has finished 12 credits across 2 semesters
-- Foundations + Systems + 2 core examples
INSERT OR IGNORE INTO completion(student_id, course_code) VALUES
 (2,'CSCI-B503'),  -- Foundations (3)
 (2,'CSCI-P536'),  -- Systems     (3)
 (2,'CSCI-P200'),  -- Core        (3)
 (2,'CSCI-B561');  -- Core        (3)

-- No pre-planned future semesters by default
-- (semester_plan remains empty; student will plan Sem 3+4 in the UI)
