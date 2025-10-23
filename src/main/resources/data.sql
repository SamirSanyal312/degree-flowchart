INSERT INTO course(code,title,credits,core) VALUES
('CSCI-P100','Applied Algorithms',3,0),
('CSCI-P150','Applied Machine Learning',3,0),
('CSCI-P200','Software Engineering 2',3,0),
('CSCI-P250','Advanced Database Concepts',3,0),
('CSCI-P300','Advanced Operating System',3,1),
('CSCI-P101','Software Engineeering 1 ',3,0),
('CSCI-P301','Computer Networks',3,1),
('ENGR-P210','Cloud Computing',3,0),
('CSCI-P221','Security Network Analysis',3,0);

INSERT INTO prereq(id,course_code,requires_code) VALUES
('P150<-P100','CSCI-P150','CSCI-P100'),
('P200<-P150','CSCI-P200','CSCI-P150'),
('P250<-P150','CSCI-P250','CSCI-P150'),
('P300<-P200','CSCI-P300','CSCI-P200'),
('P200<-P101','CSCI-P200','CSCI-P101'),
('P301<-P100','CSCI-P301','CSCI-P100'),
('P210<-P200','ENGR-P210','CSCI-P221');


UPDATE course SET offered_fall   = 1 WHERE offered_fall   IS NULL;
UPDATE course SET offered_spring = 1 WHERE offered_spring IS NULL;

-- Example: make some courses Spring-only / Fall-only (adjust to your liking)
UPDATE course SET offered_fall = 1, offered_spring = 0 WHERE code IN ('CSCI-P200','CSCI-P300');
UPDATE course SET offered_fall = 0, offered_spring = 1 WHERE code IN ('CSCI-P250');
-- Leave others both terms (default 1/1)

/* =========================
   Demo Students (MS, 4 sem plan)
   ========================= */
INSERT OR IGNORE INTO students(username, display_name, start_semester, degree)
VALUES ('student1','MS Student One',1,'MS'),
       ('student2','MS Student Two',3,'MS');

-- Student2 has completed 2 semesters already (read-only past)
-- Use the courses you already seeded in your repo:
-- CSCI-P100 (3), CSCI-P101 (3), CSCI-P150 (3), ENGR-P210 (3) as an example history
INSERT OR IGNORE INTO completion(student_id, course_code, semester_no, grade)
SELECT s.id, 'CSCI-P100', 1, 'A' FROM students s WHERE s.username='student2';
INSERT OR IGNORE INTO completion(student_id, course_code, semester_no, grade)
SELECT s.id, 'CSCI-P101', 1, 'B+' FROM students s WHERE s.username='student2';
INSERT OR IGNORE INTO completion(student_id, course_code, semester_no, grade)
SELECT s.id, 'CSCI-P150', 2, 'A-' FROM students s WHERE s.username='student2';
INSERT OR IGNORE INTO completion(student_id, course_code, semester_no, grade)
SELECT s.id, 'ENGR-P210', 2, 'A' FROM students s WHERE s.username='student2';

/* =========================
   Requirement Groups (safe no-op if earlier added)
   ========================= */
INSERT INTO req_groups(code,name,required_credits,rule_kind,choose_count)
SELECT 'FOUND','Foundations',3,'EXPLICIT_SET',1
WHERE NOT EXISTS (SELECT 1 FROM req_groups WHERE code='FOUND');

INSERT INTO req_groups(code,name,required_credits,rule_kind,choose_count)
SELECT 'SYSTEMS','Systems',3,'EXPLICIT_SET',1
WHERE NOT EXISTS (SELECT 1 FROM req_groups WHERE code='SYSTEMS');

INSERT INTO req_groups(code,name,required_credits,rule_kind,choose_count)
SELECT 'CSCORE','Computer Science Core',15,'PREDICATE',NULL
WHERE NOT EXISTS (SELECT 1 FROM req_groups WHERE code='CSCORE');

INSERT INTO req_groups(code,name,required_credits,rule_kind,choose_count)
SELECT 'CREAT','Creativity',9,'PREDICATE',NULL
WHERE NOT EXISTS (SELECT 1 FROM req_groups WHERE code='CREAT');

-- Optional: if you later add the grad courses (B501/B503/B505/P536/P538), map them to groups:
INSERT INTO req_group_courses(group_id, course_code)
SELECT (SELECT id FROM req_groups WHERE code='FOUND'), c.code
FROM course c
WHERE c.code IN ('CSCI-B501','CSCI-B503','CSCI-B505')
  AND NOT EXISTS (SELECT 1 FROM req_group_courses x WHERE x.group_id=(SELECT id FROM req_groups WHERE code='FOUND') AND x.course_code=c.code);

INSERT INTO req_group_courses(group_id, course_code)
SELECT (SELECT id FROM req_groups WHERE code='SYSTEMS'), c.code
FROM course c
WHERE c.code IN ('CSCI-P536','CSCI-P538')
  AND NOT EXISTS (SELECT 1 FROM req_group_courses x WHERE x.group_id=(SELECT id FROM req_groups WHERE code='SYSTEMS') AND x.course_code=c.code);
