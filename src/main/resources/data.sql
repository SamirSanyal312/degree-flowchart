INSERT INTO course(code,title,credits,core) VALUES
('CSCI-P100','Applied Algorithms',3,1),
('CSCI-P150','Applied Machine Learning',3,1),
('CSCI-P200','Software Engineering 2',3,1),
('CSCI-P250','Advanced Database Concepts',3,1),
('CSCI-P300','Advanced Operating System',3,0);

INSERT INTO prereq(id,course_code,requires_code) VALUES
('P150<-P100','CSCI-P150','CSCI-P100'),
('P200<-P150','CSCI-P200','CSCI-P150'),
('P250<-P150','CSCI-P250','CSCI-P150'),
('P300<-P200','CSCI-P300','CSCI-P200');