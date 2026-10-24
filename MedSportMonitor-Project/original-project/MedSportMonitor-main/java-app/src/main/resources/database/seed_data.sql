-- MedSport Monitor — Comprehensive Demo Seed Data v2.0
-- 4 Teams | 4 Coaches | 3 Doctors | 22 Athletes
-- All passwords: Demo1234!
-- BCrypt marker recognised by PasswordUtils.verify() as "Demo1234!"

PRAGMA foreign_keys = ON;

-- ============================================================
-- USERS
-- ============================================================

-- Coaches
INSERT OR IGNORE INTO users VALUES
    ('user_coach_001', 'coach@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'COACH',   'Carlos Méndez',         datetime('now','-90 days'), datetime('now','-1 day')),
    ('user_coach_002', 'coach2@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'COACH',   'Roberto Iglesias',       datetime('now','-85 days'), datetime('now','-3 hours')),
    ('user_coach_003', 'coach3@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'COACH',   'Sofia Navarro',          datetime('now','-80 days'), datetime('now','-2 hours')),
    ('user_coach_004', 'coach4@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'COACH',   'Javier Fuentes',         datetime('now','-75 days'), datetime('now','-5 hours'));

-- Medical staff
INSERT OR IGNORE INTO users VALUES
    ('user_doctor_001', 'doctor@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'MEDICAL', 'Dra. Laura Vidal',      datetime('now','-90 days'), datetime('now','-2 hours')),
    ('user_doctor_002', 'doctor2@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'MEDICAL', 'Dr. Miguel Santos',     datetime('now','-85 days'), datetime('now','-1 day')),
    ('user_doctor_003', 'doctor3@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'MEDICAL', 'Dra. Carmen Flores',    datetime('now','-80 days'), datetime('now','-4 hours'));

-- Athletes Team 001 (Athletics)
INSERT OR IGNORE INTO users VALUES
    ('user_ath_001', 'athlete1@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Marco Torres',           datetime('now','-85 days'), datetime('now')),
    ('user_ath_002', 'athlete2@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Ana Serrano',            datetime('now','-80 days'), datetime('now','-3 hours')),
    ('user_ath_003', 'athlete3@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'David Ruiz',             datetime('now','-75 days'), datetime('now','-1 day')),
    ('user_ath_004', 'athlete4@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Lucia Morales',          datetime('now','-70 days'), datetime('now','-2 hours')),
    ('user_ath_005', 'athlete5@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Pablo Jimenez',          datetime('now','-65 days'), datetime('now','-6 hours')),
    ('user_ath_006', 'athlete6@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Elena Castro',           datetime('now','-60 days'), datetime('now','-1 day'));

-- Athletes Team 002 (Football)
INSERT OR IGNORE INTO users VALUES
    ('user_ath_007', 'athlete7@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Sergio Romero',          datetime('now','-85 days'), datetime('now','-2 hours')),
    ('user_ath_008', 'athlete8@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Natalia Perez',          datetime('now','-82 days'), datetime('now','-4 hours')),
    ('user_ath_009', 'athlete9@demo.com',  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Alvaro Delgado',         datetime('now','-78 days'), datetime('now','-1 day')),
    ('user_ath_010', 'athlete10@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Isabel Fernandez',       datetime('now','-74 days'), datetime('now','-3 hours')),
    ('user_ath_011', 'athlete11@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Raul Martin',            datetime('now','-70 days'), datetime('now')),
    ('user_ath_012', 'athlete12@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Cristina Lopez',         datetime('now','-66 days'), datetime('now','-5 hours'));

-- Athletes Team 003 (Swimming)
INSERT OR IGNORE INTO users VALUES
    ('user_ath_013', 'athlete13@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Alejandro Vega',         datetime('now','-80 days'), datetime('now','-1 hour')),
    ('user_ath_014', 'athlete14@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Patricia Molina',        datetime('now','-76 days'), datetime('now','-2 hours')),
    ('user_ath_015', 'athlete15@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Fernando Ortiz',         datetime('now','-72 days'), datetime('now','-3 hours')),
    ('user_ath_016', 'athlete16@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Beatriz Herrera',        datetime('now','-68 days'), datetime('now','-6 hours')),
    ('user_ath_017', 'athlete17@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Tomas Ramos',            datetime('now','-64 days'), datetime('now','-1 day'));

-- Athletes Team 004 (Cycling)
INSERT OR IGNORE INTO users VALUES
    ('user_ath_018', 'athlete18@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Andres Guerrero',        datetime('now','-78 days'), datetime('now','-2 hours')),
    ('user_ath_019', 'athlete19@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Marta Blanco',           datetime('now','-74 days'), datetime('now','-4 hours')),
    ('user_ath_020', 'athlete20@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Victor Sanz',            datetime('now','-70 days'), datetime('now','-1 day')),
    ('user_ath_021', 'athlete21@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Rosa Medina',            datetime('now','-66 days'), datetime('now','-3 hours')),
    ('user_ath_022', 'athlete22@demo.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMaJobMkdvH6xBQFqiOcR.y6QW', 'ATHLETE', 'Hugo Rios',              datetime('now','-62 days'), datetime('now','-5 hours'));

-- ============================================================
-- PROFILES
-- ============================================================

INSERT OR IGNORE INTO coach_profiles VALUES
    ('user_coach_001', 'Athletics',  12, 'Club Atletico Universidad'),
    ('user_coach_002', 'Football',    9, 'Academias Futbol Norte'),
    ('user_coach_003', 'Swimming',   15, 'Centro Acuatico Elite Madrid'),
    ('user_coach_004', 'Cycling',     7, 'Escuela Ciclismo Montana Espana');

INSERT OR IGNORE INTO medical_profiles VALUES
    ('user_doctor_001', 'Sports Medicine',  'CL-28-04521', 'Hospital Universitario UEM'),
    ('user_doctor_002', 'Cardiology',       'CL-28-07834', 'Clinica Cardio Sport'),
    ('user_doctor_003', 'Physical Therapy', 'CL-28-09912', 'Centro Rehabilitacion Deportiva');

-- Team 001 athletes (Athletics)
INSERT OR IGNORE INTO athlete_profiles VALUES
    ('user_ath_001', '1998-03-15', 'M', 'Athletics', 178.0, 72.5,  194, 52, NULL),
    ('user_ath_002', '2000-07-22', 'F', 'Athletics', 165.0, 58.0,  188, 58, NULL),
    ('user_ath_003', '1997-11-08', 'M', 'Athletics', 182.0, 78.0,  191, 55, NULL),
    ('user_ath_004', '2001-04-18', 'F', 'Athletics', 168.0, 56.5,  190, 60, NULL),
    ('user_ath_005', '1999-09-30', 'M', 'Athletics', 175.0, 70.0,  195, 50, NULL),
    ('user_ath_006', '2002-12-05', 'F', 'Athletics', 163.0, 54.0,  186, 62, NULL);

-- Team 002 athletes (Football)
INSERT OR IGNORE INTO athlete_profiles VALUES
    ('user_ath_007', '1996-06-12', 'M', 'Football', 180.0, 76.0,  192, 54, NULL),
    ('user_ath_008', '2001-02-28', 'F', 'Football', 167.0, 62.0,  187, 59, NULL),
    ('user_ath_009', '1999-08-14', 'M', 'Football', 183.0, 80.0,  190, 56, NULL),
    ('user_ath_010', '2000-11-03', 'F', 'Football', 170.0, 65.0,  185, 61, NULL),
    ('user_ath_011', '1997-05-20', 'M', 'Football', 176.0, 74.0,  193, 53, NULL),
    ('user_ath_012', '2002-08-09', 'F', 'Football', 164.0, 60.0,  184, 63, NULL);

-- Team 003 athletes (Swimming)
INSERT OR IGNORE INTO athlete_profiles VALUES
    ('user_ath_013', '1999-03-07', 'M', 'Swimming', 185.0, 82.0,  189, 48, NULL),
    ('user_ath_014', '2001-10-25', 'F', 'Swimming', 172.0, 68.0,  183, 56, NULL),
    ('user_ath_015', '2000-06-18', 'M', 'Swimming', 188.0, 85.0,  191, 46, NULL),
    ('user_ath_016', '2003-01-11', 'F', 'Swimming', 166.0, 60.0,  185, 58, NULL),
    ('user_ath_017', '1998-09-29', 'M', 'Swimming', 181.0, 78.0,  190, 50, NULL);

-- Team 004 athletes (Cycling)
INSERT OR IGNORE INTO athlete_profiles VALUES
    ('user_ath_018', '1995-07-04', 'M', 'Cycling', 177.0, 68.0,  187, 46, NULL),
    ('user_ath_019', '1998-03-22', 'F', 'Cycling', 163.0, 57.0,  182, 54, NULL),
    ('user_ath_020', '1993-11-16', 'M', 'Cycling', 179.0, 71.0,  183, 44, NULL),
    ('user_ath_021', '2000-05-08', 'F', 'Cycling', 161.0, 55.0,  186, 57, NULL),
    ('user_ath_022', '1996-12-31', 'M', 'Cycling', 174.0, 66.0,  188, 48, NULL);

-- ============================================================
-- TEAMS
-- ============================================================

INSERT OR IGNORE INTO teams VALUES
    ('team_001', 'Atletico Biomedico', 'Athletics', 'UNIVERSITY',
     'High-performance athletics — Universidad Europea de Madrid',
     NULL, '#0A84FF', '#00D4FF', 'user_coach_001', 'ATL001',
     190, 93.0, 95.0, 800.0, 'Call 112. Keep athlete still.',
     datetime('now','-90 days'));

INSERT OR IGNORE INTO teams VALUES
    ('team_002', 'FC Rendimiento Norte', 'Football', 'PROFESSIONAL',
     'Football club — Liga Nacional semiprofesional',
     NULL, '#00C853', '#69F0AE', 'user_coach_002', 'FCR002',
     193, 93.0, 95.0, 900.0, 'Call 112. AED if needed.',
     datetime('now','-85 days'));

INSERT OR IGNORE INTO teams VALUES
    ('team_003', 'Natacion Elite MAD', 'Swimming', 'PROFESSIONAL',
     'Centro acuatico elite — competicion nacional',
     NULL, '#D32F2F', '#FF6659', 'user_coach_003', 'NAT003',
     192, 94.0, 95.0, 750.0, 'Call 112. Remove from pool. Recovery position.',
     datetime('now','-80 days'));

INSERT OR IGNORE INTO teams VALUES
    ('team_004', 'Ciclismo Montana ESP', 'Cycling', 'AMATEUR',
     'Equipo ciclismo de montana — MTB nacional',
     NULL, '#FF9800', '#FFB74D', 'user_coach_004', 'CIC004',
     190, 92.0, 95.0, 1000.0, 'Call 112. Do not remove helmet.',
     datetime('now','-75 days'));

-- ============================================================
-- TEAM MEMBERS
-- ============================================================

INSERT OR IGNORE INTO team_members VALUES
    ('team_001','user_coach_001',  'COACH',   datetime('now','-90 days'),1),
    ('team_001','user_doctor_001', 'MEDICAL', datetime('now','-88 days'),1),
    ('team_001','user_doctor_003', 'MEDICAL', datetime('now','-70 days'),1),
    ('team_001','user_ath_001',    'ATHLETE', datetime('now','-85 days'),1),
    ('team_001','user_ath_002',    'ATHLETE', datetime('now','-80 days'),1),
    ('team_001','user_ath_003',    'ATHLETE', datetime('now','-75 days'),1),
    ('team_001','user_ath_004',    'ATHLETE', datetime('now','-70 days'),1),
    ('team_001','user_ath_005',    'ATHLETE', datetime('now','-65 days'),1),
    ('team_001','user_ath_006',    'ATHLETE', datetime('now','-60 days'),1),
    ('team_002','user_coach_002',  'COACH',   datetime('now','-85 days'),1),
    ('team_002','user_doctor_001', 'MEDICAL', datetime('now','-83 days'),1),
    ('team_002','user_doctor_002', 'MEDICAL', datetime('now','-80 days'),1),
    ('team_002','user_ath_007',    'ATHLETE', datetime('now','-85 days'),1),
    ('team_002','user_ath_008',    'ATHLETE', datetime('now','-82 days'),1),
    ('team_002','user_ath_009',    'ATHLETE', datetime('now','-78 days'),1),
    ('team_002','user_ath_010',    'ATHLETE', datetime('now','-74 days'),1),
    ('team_002','user_ath_011',    'ATHLETE', datetime('now','-70 days'),1),
    ('team_002','user_ath_012',    'ATHLETE', datetime('now','-66 days'),1),
    ('team_003','user_coach_003',  'COACH',   datetime('now','-80 days'),1),
    ('team_003','user_doctor_002', 'MEDICAL', datetime('now','-78 days'),1),
    ('team_003','user_doctor_003', 'MEDICAL', datetime('now','-75 days'),1),
    ('team_003','user_ath_013',    'ATHLETE', datetime('now','-80 days'),1),
    ('team_003','user_ath_014',    'ATHLETE', datetime('now','-76 days'),1),
    ('team_003','user_ath_015',    'ATHLETE', datetime('now','-72 days'),1),
    ('team_003','user_ath_016',    'ATHLETE', datetime('now','-68 days'),1),
    ('team_003','user_ath_017',    'ATHLETE', datetime('now','-64 days'),1),
    ('team_004','user_coach_004',  'COACH',   datetime('now','-75 days'),1),
    ('team_004','user_doctor_003', 'MEDICAL', datetime('now','-72 days'),1),
    ('team_004','user_ath_018',    'ATHLETE', datetime('now','-78 days'),1),
    ('team_004','user_ath_019',    'ATHLETE', datetime('now','-74 days'),1),
    ('team_004','user_ath_020',    'ATHLETE', datetime('now','-70 days'),1),
    ('team_004','user_ath_021',    'ATHLETE', datetime('now','-66 days'),1),
    ('team_004','user_ath_022',    'ATHLETE', datetime('now','-62 days'),1);

-- ============================================================
-- TRAINING SESSIONS
-- ============================================================

-- Team 001 (Athletics)
INSERT OR IGNORE INTO training_sessions VALUES
    ('sess_001','team_001','user_coach_001',datetime('now','-14 days','+09:00:00'),datetime('now','-14 days','+10:15:00'),'RUNNING', 2,3,'Aerobic base 65-75% HRmax','COMPLETED'),
    ('sess_002','team_001','user_coach_001',datetime('now','-11 days','+07:30:00'),datetime('now','-11 days','+08:45:00'),'INTERVAL',3,5,'6x800m intervals 90s recovery','COMPLETED'),
    ('sess_003','team_001','user_coach_001',datetime('now','-8 days', '+09:00:00'),datetime('now','-8 days', '+10:00:00'),'RUNNING', 2,3,'Recovery run low intensity','COMPLETED'),
    ('sess_004','team_001','user_coach_001',datetime('now','-5 days', '+07:00:00'),datetime('now','-5 days', '+08:30:00'),'TEMPO',   3,4,'Tempo run 85-90% HRmax','COMPLETED'),
    ('sess_005','team_001','user_coach_001',datetime('now','-2 days', '+08:00:00'),datetime('now','-2 days', '+09:20:00'),'RUNNING', 2,4,'Mixed warmup Z3 sprints','COMPLETED'),
    ('sess_006','team_001','user_coach_001',datetime('now','-18 days','+07:00:00'),datetime('now','-18 days','+08:45:00'),'INTERVAL',3,5,'8x400m speed work','COMPLETED'),
    ('sess_007','team_001','user_coach_001',datetime('now','-21 days','+08:00:00'),datetime('now','-21 days','+09:30:00'),'RUNNING', 1,3,'Long slow distance 16km','COMPLETED');

-- Team 002 (Football)
INSERT OR IGNORE INTO training_sessions VALUES
    ('sess_010','team_002','user_coach_002',datetime('now','-13 days','+10:00:00'),datetime('now','-13 days','+11:30:00'),'RUNNING', 2,3,'Aerobic conditioning circuit','COMPLETED'),
    ('sess_011','team_002','user_coach_002',datetime('now','-10 days','+08:00:00'),datetime('now','-10 days','+09:45:00'),'INTERVAL',3,5,'High-intensity pressing drills','COMPLETED'),
    ('sess_012','team_002','user_coach_002',datetime('now','-7 days', '+10:00:00'),datetime('now','-7 days', '+11:15:00'),'RUNNING', 2,3,'Tactical run low intensity','COMPLETED'),
    ('sess_013','team_002','user_coach_002',datetime('now','-4 days', '+08:30:00'),datetime('now','-4 days', '+10:00:00'),'TEMPO',   3,4,'Pre-match intensity 80% HRmax','COMPLETED'),
    ('sess_014','team_002','user_coach_002',datetime('now','-1 day',  '+09:00:00'),datetime('now','-1 day',  '+10:15:00'),'RUNNING', 1,2,'Recovery post-match','COMPLETED');

-- Team 003 (Swimming)
INSERT OR IGNORE INTO training_sessions VALUES
    ('sess_020','team_003','user_coach_003',datetime('now','-12 days','+06:30:00'),datetime('now','-12 days','+08:00:00'),'RUNNING', 2,3,'Dryland warmup 4km freestyle','COMPLETED'),
    ('sess_021','team_003','user_coach_003',datetime('now','-9 days', '+06:30:00'),datetime('now','-9 days', '+08:15:00'),'INTERVAL',3,5,'10x100m sprint sets','COMPLETED'),
    ('sess_022','team_003','user_coach_003',datetime('now','-6 days', '+07:00:00'),datetime('now','-6 days', '+08:30:00'),'TEMPO',   3,4,'Threshold 2x1500m 90% effort','COMPLETED'),
    ('sess_023','team_003','user_coach_003',datetime('now','-3 days', '+06:30:00'),datetime('now','-3 days', '+08:00:00'),'RUNNING', 1,2,'Recovery swim 2km cool-down','COMPLETED');

-- Team 004 (Cycling)
INSERT OR IGNORE INTO training_sessions VALUES
    ('sess_030','team_004','user_coach_004',datetime('now','-15 days','+08:00:00'),datetime('now','-15 days','+11:00:00'),'RUNNING', 2,3,'3hr base endurance 65-70% FTP','COMPLETED'),
    ('sess_031','team_004','user_coach_004',datetime('now','-12 days','+07:00:00'),datetime('now','-12 days','+09:30:00'),'INTERVAL',4,5,'4x8min VO2max intervals','COMPLETED'),
    ('sess_032','team_004','user_coach_004',datetime('now','-8 days', '+08:00:00'),datetime('now','-8 days', '+10:00:00'),'TEMPO',   3,4,'2hr threshold 80-85% FTP','COMPLETED'),
    ('sess_033','team_004','user_coach_004',datetime('now','-4 days', '+07:30:00'),datetime('now','-4 days', '+09:30:00'),'RUNNING', 2,3,'MTB technical hill repeats','COMPLETED');

-- ============================================================
-- SESSION PARTICIPANTS
-- ============================================================

INSERT OR IGNORE INTO session_participants VALUES
    ('sess_001','user_ath_001',datetime('now','-14 days','+09:02:00'),datetime('now','-14 days','+10:14:00')),
    ('sess_001','user_ath_002',datetime('now','-14 days','+09:01:00'),datetime('now','-14 days','+10:15:00')),
    ('sess_001','user_ath_003',datetime('now','-14 days','+09:03:00'),datetime('now','-14 days','+10:12:00')),
    ('sess_001','user_ath_004',datetime('now','-14 days','+09:00:00'),datetime('now','-14 days','+10:14:00')),
    ('sess_001','user_ath_005',datetime('now','-14 days','+09:04:00'),datetime('now','-14 days','+10:13:00')),
    ('sess_001','user_ath_006',datetime('now','-14 days','+09:02:00'),datetime('now','-14 days','+10:10:00')),
    ('sess_002','user_ath_001',datetime('now','-11 days','+07:31:00'),datetime('now','-11 days','+08:44:00')),
    ('sess_002','user_ath_002',datetime('now','-11 days','+07:32:00'),datetime('now','-11 days','+08:45:00')),
    ('sess_002','user_ath_003',datetime('now','-11 days','+07:30:00'),datetime('now','-11 days','+08:42:00')),
    ('sess_002','user_ath_004',datetime('now','-11 days','+07:33:00'),datetime('now','-11 days','+08:43:00')),
    ('sess_002','user_ath_005',datetime('now','-11 days','+07:30:00'),datetime('now','-11 days','+08:44:00')),
    ('sess_003','user_ath_001',datetime('now','-8 days', '+09:01:00'),datetime('now','-8 days', '+09:59:00')),
    ('sess_003','user_ath_003',datetime('now','-8 days', '+09:02:00'),datetime('now','-8 days', '+10:00:00')),
    ('sess_003','user_ath_004',datetime('now','-8 days', '+09:00:00'),datetime('now','-8 days', '+09:58:00')),
    ('sess_003','user_ath_006',datetime('now','-8 days', '+09:01:00'),datetime('now','-8 days', '+09:57:00')),
    ('sess_004','user_ath_001',datetime('now','-5 days', '+07:01:00'),datetime('now','-5 days', '+08:29:00')),
    ('sess_004','user_ath_002',datetime('now','-5 days', '+07:02:00'),datetime('now','-5 days', '+08:30:00')),
    ('sess_004','user_ath_003',datetime('now','-5 days', '+07:00:00'),datetime('now','-5 days', '+08:28:00')),
    ('sess_004','user_ath_005',datetime('now','-5 days', '+07:01:00'),datetime('now','-5 days', '+08:27:00')),
    ('sess_005','user_ath_001',datetime('now','-2 days', '+08:01:00'),datetime('now','-2 days', '+09:19:00')),
    ('sess_005','user_ath_002',datetime('now','-2 days', '+08:00:00'),datetime('now','-2 days', '+09:20:00')),
    ('sess_005','user_ath_003',datetime('now','-2 days', '+08:02:00'),datetime('now','-2 days', '+09:18:00')),
    ('sess_005','user_ath_004',datetime('now','-2 days', '+08:00:00'),datetime('now','-2 days', '+09:17:00')),
    ('sess_005','user_ath_005',datetime('now','-2 days', '+08:01:00'),datetime('now','-2 days', '+09:19:00')),
    ('sess_005','user_ath_006',datetime('now','-2 days', '+08:03:00'),datetime('now','-2 days', '+09:15:00')),
    ('sess_006','user_ath_001',datetime('now','-18 days','+07:01:00'),datetime('now','-18 days','+08:44:00')),
    ('sess_006','user_ath_002',datetime('now','-18 days','+07:00:00'),datetime('now','-18 days','+08:43:00')),
    ('sess_006','user_ath_003',datetime('now','-18 days','+07:02:00'),datetime('now','-18 days','+08:44:00')),
    ('sess_007','user_ath_001',datetime('now','-21 days','+08:01:00'),datetime('now','-21 days','+09:28:00')),
    ('sess_007','user_ath_003',datetime('now','-21 days','+08:00:00'),datetime('now','-21 days','+09:30:00')),
    ('sess_007','user_ath_005',datetime('now','-21 days','+08:02:00'),datetime('now','-21 days','+09:25:00')),
    ('sess_010','user_ath_007',datetime('now','-13 days','+10:01:00'),datetime('now','-13 days','+11:29:00')),
    ('sess_010','user_ath_008',datetime('now','-13 days','+10:00:00'),datetime('now','-13 days','+11:30:00')),
    ('sess_010','user_ath_009',datetime('now','-13 days','+10:02:00'),datetime('now','-13 days','+11:28:00')),
    ('sess_010','user_ath_010',datetime('now','-13 days','+10:01:00'),datetime('now','-13 days','+11:27:00')),
    ('sess_010','user_ath_011',datetime('now','-13 days','+10:00:00'),datetime('now','-13 days','+11:30:00')),
    ('sess_010','user_ath_012',datetime('now','-13 days','+10:03:00'),datetime('now','-13 days','+11:26:00')),
    ('sess_011','user_ath_007',datetime('now','-10 days','+08:01:00'),datetime('now','-10 days','+09:44:00')),
    ('sess_011','user_ath_009',datetime('now','-10 days','+08:00:00'),datetime('now','-10 days','+09:43:00')),
    ('sess_011','user_ath_011',datetime('now','-10 days','+08:01:00'),datetime('now','-10 days','+09:45:00')),
    ('sess_012','user_ath_007',datetime('now','-7 days', '+10:01:00'),datetime('now','-7 days', '+11:14:00')),
    ('sess_012','user_ath_008',datetime('now','-7 days', '+10:00:00'),datetime('now','-7 days', '+11:15:00')),
    ('sess_012','user_ath_010',datetime('now','-7 days', '+10:02:00'),datetime('now','-7 days', '+11:13:00')),
    ('sess_012','user_ath_012',datetime('now','-7 days', '+10:01:00'),datetime('now','-7 days', '+11:12:00')),
    ('sess_013','user_ath_007',datetime('now','-4 days', '+08:31:00'),datetime('now','-4 days', '+09:59:00')),
    ('sess_013','user_ath_009',datetime('now','-4 days', '+08:30:00'),datetime('now','-4 days', '+09:58:00')),
    ('sess_013','user_ath_011',datetime('now','-4 days', '+08:31:00'),datetime('now','-4 days', '+10:00:00')),
    ('sess_014','user_ath_007',datetime('now','-1 day',  '+09:01:00'),datetime('now','-1 day',  '+10:14:00')),
    ('sess_014','user_ath_008',datetime('now','-1 day',  '+09:00:00'),datetime('now','-1 day',  '+10:15:00')),
    ('sess_014','user_ath_009',datetime('now','-1 day',  '+09:01:00'),datetime('now','-1 day',  '+10:13:00')),
    ('sess_014','user_ath_010',datetime('now','-1 day',  '+09:02:00'),datetime('now','-1 day',  '+10:12:00')),
    ('sess_014','user_ath_011',datetime('now','-1 day',  '+09:00:00'),datetime('now','-1 day',  '+10:14:00')),
    ('sess_014','user_ath_012',datetime('now','-1 day',  '+09:01:00'),datetime('now','-1 day',  '+10:11:00')),
    ('sess_020','user_ath_013',datetime('now','-12 days','+06:31:00'),datetime('now','-12 days','+07:59:00')),
    ('sess_020','user_ath_014',datetime('now','-12 days','+06:30:00'),datetime('now','-12 days','+08:00:00')),
    ('sess_020','user_ath_015',datetime('now','-12 days','+06:32:00'),datetime('now','-12 days','+07:58:00')),
    ('sess_020','user_ath_016',datetime('now','-12 days','+06:30:00'),datetime('now','-12 days','+07:57:00')),
    ('sess_020','user_ath_017',datetime('now','-12 days','+06:31:00'),datetime('now','-12 days','+07:58:00')),
    ('sess_021','user_ath_013',datetime('now','-9 days', '+06:31:00'),datetime('now','-9 days', '+08:14:00')),
    ('sess_021','user_ath_015',datetime('now','-9 days', '+06:30:00'),datetime('now','-9 days', '+08:15:00')),
    ('sess_021','user_ath_017',datetime('now','-9 days', '+06:31:00'),datetime('now','-9 days', '+08:13:00')),
    ('sess_022','user_ath_013',datetime('now','-6 days', '+07:01:00'),datetime('now','-6 days', '+08:29:00')),
    ('sess_022','user_ath_014',datetime('now','-6 days', '+07:00:00'),datetime('now','-6 days', '+08:30:00')),
    ('sess_022','user_ath_015',datetime('now','-6 days', '+07:01:00'),datetime('now','-6 days', '+08:28:00')),
    ('sess_022','user_ath_016',datetime('now','-6 days', '+07:02:00'),datetime('now','-6 days', '+08:27:00')),
    ('sess_022','user_ath_017',datetime('now','-6 days', '+07:00:00'),datetime('now','-6 days', '+08:30:00')),
    ('sess_023','user_ath_013',datetime('now','-3 days', '+06:31:00'),datetime('now','-3 days', '+07:59:00')),
    ('sess_023','user_ath_014',datetime('now','-3 days', '+06:30:00'),datetime('now','-3 days', '+08:00:00')),
    ('sess_023','user_ath_016',datetime('now','-3 days', '+06:31:00'),datetime('now','-3 days', '+07:58:00')),
    ('sess_030','user_ath_018',datetime('now','-15 days','+08:01:00'),datetime('now','-15 days','+10:59:00')),
    ('sess_030','user_ath_019',datetime('now','-15 days','+08:00:00'),datetime('now','-15 days','+11:00:00')),
    ('sess_030','user_ath_020',datetime('now','-15 days','+08:02:00'),datetime('now','-15 days','+10:58:00')),
    ('sess_030','user_ath_021',datetime('now','-15 days','+08:01:00'),datetime('now','-15 days','+10:57:00')),
    ('sess_030','user_ath_022',datetime('now','-15 days','+08:00:00'),datetime('now','-15 days','+11:00:00')),
    ('sess_031','user_ath_018',datetime('now','-12 days','+07:01:00'),datetime('now','-12 days','+09:29:00')),
    ('sess_031','user_ath_020',datetime('now','-12 days','+07:00:00'),datetime('now','-12 days','+09:28:00')),
    ('sess_031','user_ath_022',datetime('now','-12 days','+07:01:00'),datetime('now','-12 days','+09:30:00')),
    ('sess_032','user_ath_018',datetime('now','-8 days', '+08:01:00'),datetime('now','-8 days', '+09:59:00')),
    ('sess_032','user_ath_019',datetime('now','-8 days', '+08:00:00'),datetime('now','-8 days', '+10:00:00')),
    ('sess_032','user_ath_020',datetime('now','-8 days', '+08:02:00'),datetime('now','-8 days', '+09:58:00')),
    ('sess_032','user_ath_021',datetime('now','-8 days', '+08:01:00'),datetime('now','-8 days', '+09:57:00')),
    ('sess_032','user_ath_022',datetime('now','-8 days', '+08:00:00'),datetime('now','-8 days', '+09:58:00')),
    ('sess_033','user_ath_018',datetime('now','-4 days', '+07:31:00'),datetime('now','-4 days', '+09:29:00')),
    ('sess_033','user_ath_019',datetime('now','-4 days', '+07:30:00'),datetime('now','-4 days', '+09:28:00')),
    ('sess_033','user_ath_020',datetime('now','-4 days', '+07:31:00'),datetime('now','-4 days', '+09:30:00')),
    ('sess_033','user_ath_021',datetime('now','-4 days', '+07:30:00'),datetime('now','-4 days', '+09:27:00')),
    ('sess_033','user_ath_022',datetime('now','-4 days', '+07:32:00'),datetime('now','-4 days', '+09:28:00'));

-- ============================================================
-- SENSOR READINGS
-- ============================================================

INSERT OR IGNORE INTO sensor_readings(athlete_id,session_id,timestamp,sensor_type,value,unit,quality) VALUES
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:00:00'),'hr',162.0,'bpm',0.98),
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:05:00'),'hr',178.0,'bpm',0.98),
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:10:00'),'hr',186.0,'bpm',0.97),
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:15:00'),'hr',183.0,'bpm',0.97),
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:00:00'),'spo2',97.0,'%',0.99),
    ('user_ath_001','sess_002',datetime('now','-11 days','+08:10:00'),'spo2',95.5,'%',0.99),
    ('user_ath_002','sess_002',datetime('now','-11 days','+08:00:00'),'hr',168.0,'bpm',0.98),
    ('user_ath_002','sess_002',datetime('now','-11 days','+08:05:00'),'hr',182.0,'bpm',0.98),
    ('user_ath_002','sess_002',datetime('now','-11 days','+08:15:00'),'hr',197.0,'bpm',0.95),
    ('user_ath_002','sess_002',datetime('now','-11 days','+08:10:00'),'spo2',94.5,'%',0.99),
    ('user_ath_003','sess_002',datetime('now','-11 days','+08:00:00'),'hr',155.0,'bpm',0.98),
    ('user_ath_003','sess_002',datetime('now','-11 days','+08:10:00'),'hr',175.0,'bpm',0.98),
    ('user_ath_003','sess_002',datetime('now','-11 days','+08:15:00'),'hr',185.0,'bpm',0.97),
    ('user_ath_004','sess_002',datetime('now','-11 days','+08:00:00'),'hr',158.0,'bpm',0.98),
    ('user_ath_004','sess_002',datetime('now','-11 days','+08:10:00'),'hr',174.0,'bpm',0.98),
    ('user_ath_005','sess_002',datetime('now','-11 days','+08:00:00'),'hr',152.0,'bpm',0.98),
    ('user_ath_005','sess_002',datetime('now','-11 days','+08:10:00'),'hr',178.0,'bpm',0.98),
    ('user_ath_005','sess_002',datetime('now','-11 days','+08:20:00'),'hr',189.0,'bpm',0.97),
    ('user_ath_001','sess_004',datetime('now','-5 days','+07:15:00'),'gps_lat',40.4168,'deg',0.99),
    ('user_ath_001','sess_004',datetime('now','-5 days','+07:15:00'),'gps_lon',-3.7038,'deg',0.99),
    ('user_ath_001','sess_004',datetime('now','-5 days','+07:15:00'),'gps_speed',16.2,'km/h',0.99),
    ('user_ath_001','sess_004',datetime('now','-5 days','+07:30:00'),'gps_speed',17.8,'km/h',0.99),
    ('user_ath_001','sess_004',datetime('now','-5 days','+07:45:00'),'gps_speed',19.8,'km/h',0.98),
    ('user_ath_003','sess_004',datetime('now','-5 days','+07:15:00'),'gps_speed',18.5,'km/h',0.99),
    ('user_ath_003','sess_004',datetime('now','-5 days','+07:45:00'),'gps_speed',21.2,'km/h',0.98),
    ('user_ath_007','sess_011',datetime('now','-10 days','+08:20:00'),'hr',174.0,'bpm',0.98),
    ('user_ath_007','sess_011',datetime('now','-10 days','+08:35:00'),'hr',183.0,'bpm',0.97),
    ('user_ath_007','sess_011',datetime('now','-10 days','+08:20:00'),'spo2',96.5,'%',0.99),
    ('user_ath_009','sess_011',datetime('now','-10 days','+08:20:00'),'hr',178.0,'bpm',0.98),
    ('user_ath_009','sess_011',datetime('now','-10 days','+08:35:00'),'hr',191.0,'bpm',0.96),
    ('user_ath_011','sess_011',datetime('now','-10 days','+08:20:00'),'hr',169.0,'bpm',0.98),
    ('user_ath_011','sess_011',datetime('now','-10 days','+08:35:00'),'hr',182.0,'bpm',0.97),
    ('user_ath_013','sess_021',datetime('now','-9 days','+07:00:00'),'hr',148.0,'bpm',0.97),
    ('user_ath_013','sess_021',datetime('now','-9 days','+07:15:00'),'hr',168.0,'bpm',0.97),
    ('user_ath_013','sess_021',datetime('now','-9 days','+07:30:00'),'hr',180.0,'bpm',0.96),
    ('user_ath_015','sess_021',datetime('now','-9 days','+07:00:00'),'hr',152.0,'bpm',0.97),
    ('user_ath_015','sess_021',datetime('now','-9 days','+07:15:00'),'hr',172.0,'bpm',0.97),
    ('user_ath_015','sess_021',datetime('now','-9 days','+07:30:00'),'hr',182.0,'bpm',0.96),
    ('user_ath_018','sess_031',datetime('now','-12 days','+07:30:00'),'hr',158.0,'bpm',0.98),
    ('user_ath_018','sess_031',datetime('now','-12 days','+07:40:00'),'hr',172.0,'bpm',0.98),
    ('user_ath_018','sess_031',datetime('now','-12 days','+07:50:00'),'hr',181.0,'bpm',0.97),
    ('user_ath_018','sess_031',datetime('now','-12 days','+07:30:00'),'gps_speed',38.2,'km/h',0.99),
    ('user_ath_018','sess_031',datetime('now','-12 days','+07:40:00'),'gps_speed',41.6,'km/h',0.99),
    ('user_ath_020','sess_031',datetime('now','-12 days','+07:30:00'),'hr',162.0,'bpm',0.98),
    ('user_ath_020','sess_031',datetime('now','-12 days','+07:45:00'),'hr',174.0,'bpm',0.97),
    ('user_ath_022','sess_031',datetime('now','-12 days','+07:30:00'),'hr',155.0,'bpm',0.98),
    ('user_ath_022','sess_031',datetime('now','-12 days','+07:40:00'),'hr',169.0,'bpm',0.97);

-- ============================================================
-- ALERTS
-- ============================================================

INSERT OR IGNORE INTO alerts VALUES
    ('alert_001','user_ath_002','team_001','sess_002',datetime('now','-11 days','+08:20:00'),'HR_ABOVE_MAX','CRITICAL','heart_rate',197.0,178.6,1,'user_doctor_001',datetime('now','-11 days','+08:22:00')),
    ('alert_002','user_ath_003','team_001','sess_004',datetime('now','-5 days', '+08:05:00'),'SPO2_LOW','WARNING','spo2',92.1,93.0,1,'user_doctor_001',datetime('now','-5 days','+08:10:00')),
    ('alert_003','user_ath_001','team_001','sess_005',datetime('now','-2 days', '+09:10:00'),'HR_ABOVE_MAX','WARNING','heart_rate',188.5,184.3,0,NULL,NULL),
    ('alert_004','user_ath_005','team_001','sess_002',datetime('now','-11 days','+08:25:00'),'HR_ABOVE_MAX','WARNING','heart_rate',193.0,185.25,0,NULL,NULL),
    ('alert_005','user_ath_009','team_002','sess_011',datetime('now','-10 days','+08:40:00'),'HR_ABOVE_MAX','CRITICAL','heart_rate',193.0,180.5,1,'user_doctor_001',datetime('now','-10 days','+08:42:00')),
    ('alert_006','user_ath_013','team_003','sess_022',datetime('now','-6 days', '+08:00:00'),'SPO2_LOW','WARNING','spo2',91.8,94.0,1,'user_doctor_002',datetime('now','-6 days','+08:05:00')),
    ('alert_007','user_ath_018','team_004','sess_031',datetime('now','-12 days','+07:55:00'),'HR_ABOVE_MAX','WARNING','heart_rate',184.0,177.65,1,'user_doctor_003',datetime('now','-12 days','+08:00:00')),
    ('alert_008','user_ath_004','team_001','sess_005',datetime('now','-2 days', '+08:50:00'),'SPO2_LOW','ATTENTION','spo2',93.5,95.0,0,NULL,NULL),
    ('alert_009','user_ath_016','team_003','sess_021',datetime('now','-9 days', '+07:45:00'),'HR_ABOVE_MAX','ATTENTION','heart_rate',182.0,175.75,0,NULL,NULL);

-- ============================================================
-- MEDICAL RECORDS
-- ============================================================

INSERT OR IGNORE INTO medical_records VALUES
    ('mr_001','user_ath_001','user_doctor_001','blood_type','A+',NULL,datetime('now','-85 days'),datetime('now','-85 days')),
    ('mr_002','user_ath_001','user_doctor_001','emergency_contact','Maria Torres +34 612 345 678 (Mother)',NULL,datetime('now','-85 days'),datetime('now','-85 days')),
    ('mr_003','user_ath_002','user_doctor_001','blood_type','O-',NULL,datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_004','user_ath_002','user_doctor_001','allergy','Penicillin','HIGH',datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_005','user_ath_002','user_doctor_001','emergency_contact','Luis Serrano +34 698 765 432 (Father)',NULL,datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_006','user_ath_002','user_doctor_001','pathology','Mild exercise-induced asthma 2019 controlled',NULL,datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_007','user_ath_003','user_doctor_001','blood_type','B+',NULL,datetime('now','-75 days'),datetime('now','-75 days')),
    ('mr_008','user_ath_003','user_doctor_001','emergency_contact','Carmen Ruiz +34 677 123 456 (Partner)',NULL,datetime('now','-75 days'),datetime('now','-75 days')),
    ('mr_009','user_ath_004','user_doctor_001','blood_type','AB+',NULL,datetime('now','-70 days'),datetime('now','-70 days')),
    ('mr_010','user_ath_004','user_doctor_001','emergency_contact','Pedro Morales +34 655 432 100 (Father)',NULL,datetime('now','-70 days'),datetime('now','-70 days')),
    ('mr_011','user_ath_005','user_doctor_003','blood_type','A-',NULL,datetime('now','-65 days'),datetime('now','-65 days')),
    ('mr_012','user_ath_005','user_doctor_003','allergy','Ibuprofen','MEDIUM',datetime('now','-65 days'),datetime('now','-65 days')),
    ('mr_013','user_ath_005','user_doctor_003','medication','Salbutamol inhaler PRN','LOW',datetime('now','-65 days'),datetime('now','-65 days')),
    ('mr_014','user_ath_006','user_doctor_003','blood_type','O+',NULL,datetime('now','-60 days'),datetime('now','-60 days')),
    ('mr_020','user_ath_007','user_doctor_001','blood_type','A+',NULL,datetime('now','-85 days'),datetime('now','-85 days')),
    ('mr_021','user_ath_007','user_doctor_001','pathology','Left knee patellofemoral syndrome managed',NULL,datetime('now','-85 days'),datetime('now','-85 days')),
    ('mr_022','user_ath_007','user_doctor_001','emergency_contact','Julia Romero +34 612 001 122 (Spouse)',NULL,datetime('now','-85 days'),datetime('now','-85 days')),
    ('mr_023','user_ath_009','user_doctor_002','blood_type','B-',NULL,datetime('now','-78 days'),datetime('now','-78 days')),
    ('mr_024','user_ath_009','user_doctor_002','pathology','HCM screening NEGATIVE 2024',NULL,datetime('now','-78 days'),datetime('now','-78 days')),
    ('mr_025','user_ath_011','user_doctor_002','blood_type','O+',NULL,datetime('now','-70 days'),datetime('now','-70 days')),
    ('mr_030','user_ath_013','user_doctor_002','blood_type','A+',NULL,datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_031','user_ath_013','user_doctor_002','pathology','Exercise-induced laryngospasm 2022 resolved',NULL,datetime('now','-80 days'),datetime('now','-80 days')),
    ('mr_032','user_ath_015','user_doctor_002','blood_type','AB-',NULL,datetime('now','-72 days'),datetime('now','-72 days')),
    ('mr_033','user_ath_015','user_doctor_002','allergy','Chlorine sensitivity eyes','LOW',datetime('now','-72 days'),datetime('now','-72 days')),
    ('mr_040','user_ath_018','user_doctor_003','blood_type','O+',NULL,datetime('now','-78 days'),datetime('now','-78 days')),
    ('mr_041','user_ath_018','user_doctor_003','pathology','L4-L5 disc protrusion 2021 asymptomatic',NULL,datetime('now','-78 days'),datetime('now','-78 days')),
    ('mr_042','user_ath_020','user_doctor_003','blood_type','A+',NULL,datetime('now','-70 days'),datetime('now','-70 days')),
    ('mr_043','user_ath_020','user_doctor_003','medication','Omeprazole 20mg daily GERD','LOW',datetime('now','-70 days'),datetime('now','-70 days'));

-- ============================================================
-- SOAP NOTES
-- ============================================================

INSERT OR IGNORE INTO soap_notes VALUES
    ('soap_001','user_ath_002','user_doctor_001','sess_002',date('now','-11 days'),
     'Slight chest tightness during intervals. No pain, resolved after cool-down.',
     'HR peaked at 197 bpm. SpO2 at 95%. Respiratory rate elevated, baseline within 8 min.',
     'Transient exercise-induced bronchospasm. Not cardiac. Monitor next high-intensity.',
     'Reduce intensity to 90% HRmax. Pre-session bronchodilator. Re-evaluate in 2 weeks.',
     NULL,1,datetime('now','-11 days','+09:00:00')),
    ('soap_002','user_ath_003','user_doctor_001','sess_004',date('now','-5 days'),
     'No symptoms. Athlete felt strong throughout.',
     'SpO2 92.1% at peak effort km 8. Recovered to 96% within 3 min at reduced pace.',
     'Transient desaturation at sustained high intensity. Likely hyperventilation.',
     'Monitor SpO2 tempo sessions. No restriction. Controlled breathing education.',
     NULL,1,datetime('now','-5 days','+09:30:00')),
    ('soap_003','user_ath_001','user_doctor_001','sess_005',date('now','-2 days'),
     'General fatigue, poor sleep this week. Minor tightness left calf.',
     'HR 188 bpm 97% HRmax. Calf Grade I tightness no strain. HRV readiness LOW.',
     'Functional overreaching. Calf at low injury risk.',
     'Mandatory 2-day recovery. No training above Z2. Ice calf 15 min twice daily.',
     NULL,1,datetime('now','-2 days','+10:00:00')),
    ('soap_004','user_ath_009','user_doctor_001','sess_011',date('now','-10 days'),
     'No specific complaints. Athlete felt breathless at peak intensity.',
     'HR 193 bpm during drill. SpO2 94% peak. ECG normal sinus rhythm no ectopy.',
     'HR exceeded threshold likely due to high caffeine pre-session. ECG normal.',
     'Remind pre-session nutrition protocol. Retest controlled conditions. No restriction.',
     NULL,1,datetime('now','-10 days','+10:00:00')),
    ('soap_005','user_ath_013','user_doctor_002','sess_022',date('now','-6 days'),
     'Tingling in fingers post-session. SpO2 alert. Mild headache. No chest pain.',
     'SpO2 91.8% intra-session recovered to 97% within 5 min. BP 118/72. Lungs clear.',
     'Hypoxemia secondary to hyperventilation during sustained swimming. No lung pathology.',
     'Breathing re-education. Reduce pace 5% next threshold session. Recheck SpO2 trend.',
     NULL,1,datetime('now','-6 days','+09:00:00')),
    ('soap_006','user_ath_007','user_doctor_001','sess_010',date('now','-13 days'),
     'Left knee clicking on stairs and during change-of-direction drills. Pain 2/10.',
     'Patellofemoral grinding positive. VMO strength deficit. Mild medial warmth left knee.',
     'Patellofemoral pain syndrome flare. Consistent with history. Low risk straight running.',
     'No cutting drills 7 days. VMO strengthening 3x12 single-leg press. Anti-inflam PRN.',
     NULL,1,datetime('now','-13 days','+12:00:00')),
    ('soap_007','user_ath_020','user_doctor_003','sess_032',date('now','-8 days'),
     'Mild low-back stiffness during 2hr threshold ride. Resolves post-session.',
     'Lumbar flexion -10deg. Paraspinal tenderness L3-L5. Neurological intact.',
     'Chronic L4-L5 disc protrusion aggravated by aero position. Grade 1.',
     'Raise bars 1cm. Core activation pre-session. Physio twice weekly. Reassess 3 weeks.',
     NULL,1,datetime('now','-8 days','+11:00:00')),
    ('soap_008','user_ath_004','user_doctor_003','sess_005',date('now','-2 days'),
     'Shin splints developing medial tibia bilaterally. No other complaints.',
     'Periosteal tenderness medial tibia. Pain 3/10. No stress fracture. Normal density.',
     'Medial tibial stress syndrome. Common in athletes increasing volume.',
     'Reduce training 20% this week. Massage. Calcium Vitamin D. Assess in 10 days.',
     NULL,1,datetime('now','-2 days','+11:00:00'));

-- ============================================================
-- MEDICAL RESTRICTIONS
-- ============================================================

INSERT OR IGNORE INTO medical_restrictions VALUES
    ('restr_001','user_ath_002','user_doctor_001','HR_LIMITED',178,
     'HR cap 178 bpm during high-intensity. No intervals above Z4. Full Z1-Z3 permitted.',
     date('now','-11 days'),date('now','+17 days'),'active'),
    ('restr_002','user_ath_007','user_doctor_001','PARTIAL',NULL,
     'No change-of-direction or lateral cutting 7 days. Straight running gym permitted. Max 80% HRmax.',
     date('now','-13 days'),date('now','+4 days'),'active'),
    ('restr_003','user_ath_001','user_doctor_001','HR_LIMITED',165,
     'Recovery period. No training above Z2. No intervals. Minimum 48h rest structured training.',
     date('now','-2 days'),date('now','+3 days'),'active'),
    ('restr_004','user_ath_003','user_doctor_001','PARTIAL',NULL,
     'SpO2 monitoring mandatory all tempo sessions. Reduce pace 5% when SpO2 below 94%.',
     date('now','-5 days'),date('now','+9 days'),'active'),
    ('restr_005','user_ath_018','user_doctor_003','HR_LIMITED',172,
     'Aero position max 90 min per session. Upright riding recommended. No full-gas climbs.',
     date('now','-8 days'),date('now','+6 days'),'active'),
    ('restr_006','user_ath_009','user_doctor_002','FULL',NULL,
     'Full restriction 48h post HR spike. Medical clearance before any activity.',
     date('now','-10 days'),date('now','-8 days'),'resolved');

-- ============================================================
-- CHAT MESSAGES
-- ============================================================

INSERT OR IGNORE INTO chat_messages VALUES
    ('chat_001','team_001','user_coach_001','Great effort today. Marco excellent pacing on intervals.',datetime('now','-11 days','+09:00:00'),1),
    ('chat_002','team_001','user_ath_001','Thanks coach! Felt strong after the first 3 reps.',datetime('now','-11 days','+09:05:00'),1),
    ('chat_003','team_001','user_doctor_001','Ana please check in with me before next interval session.',datetime('now','-10 days','+10:00:00'),1),
    ('chat_004','team_001','user_ath_002','Of course. I will come by at 17:00.',datetime('now','-10 days','+10:15:00'),1),
    ('chat_005','team_001','user_coach_001','Reminder: tempo run tomorrow 07:00. Track kit required.',datetime('now','-6 days','+20:00:00'),1),
    ('chat_006','team_001','user_ath_003','Ready. Legs feel good after recovery run.',datetime('now','-6 days','+20:30:00'),1),
    ('chat_007','team_001','user_coach_001','Session review uploaded. Check your individual stats.',datetime('now','-2 days','+10:00:00'),0),
    ('chat_008','team_001','user_ath_004','My shins are a bit sore. Is that normal?',datetime('now','-2 days','+10:30:00'),0),
    ('chat_009','team_001','user_doctor_003','Lucia come see me after training.',datetime('now','-2 days','+10:45:00'),0),
    ('chat_010','team_001','user_ath_005','Coach can I do extra 5km tomorrow morning?',datetime('now','-1 day','+19:00:00'),0),
    ('chat_011','team_001','user_coach_001','Pablo no extra volume this week. Trust the plan.',datetime('now','-1 day','+19:15:00'),0),
    ('chat_020','team_002','user_coach_002','Good intensity today. Sergio and Alvaro stay after.',datetime('now','-10 days','+09:50:00'),1),
    ('chat_021','team_002','user_ath_007','Ready coach.',datetime('now','-10 days','+09:55:00'),1),
    ('chat_022','team_002','user_doctor_001','Alvaro I need your HR data from yesterday before Friday.',datetime('now','-9 days','+11:00:00'),1),
    ('chat_023','team_002','user_ath_009','Sharing it now.',datetime('now','-9 days','+11:10:00'),1),
    ('chat_024','team_002','user_coach_002','Match day Thursday. Activation session 09:00. Light.',datetime('now','-5 days','+20:00:00'),1),
    ('chat_025','team_002','user_ath_011','Feeling 100%. Ready for Thursday.',datetime('now','-5 days','+20:30:00'),0),
    ('chat_030','team_003','user_coach_003','National qualifier in 3 weeks. Taper starts Monday.',datetime('now','-7 days','+08:00:00'),1),
    ('chat_031','team_003','user_ath_013','Finally! We have been building for this.',datetime('now','-7 days','+08:15:00'),1),
    ('chat_032','team_003','user_doctor_002','Alejandro your SpO2 episode needs follow-up.',datetime('now','-6 days','+09:00:00'),1),
    ('chat_033','team_003','user_ath_013','Booked for Wednesday 15:00.',datetime('now','-6 days','+09:30:00'),1),
    ('chat_034','team_003','user_coach_003','Fernando excellent 1500m splits today.',datetime('now','-3 days','+08:30:00'),0),
    ('chat_040','team_004','user_coach_004','MTB stage race in 4 weeks. Volume peaks this week then taper.',datetime('now','-8 days','+07:30:00'),1),
    ('chat_041','team_004','user_ath_018','Bike fit adjustment helped my back a lot today.',datetime('now','-8 days','+11:00:00'),1),
    ('chat_042','team_004','user_doctor_003','Andres back check-in Thursday. Bring training log.',datetime('now','-7 days','+09:00:00'),1),
    ('chat_043','team_004','user_ath_020','Coach my knees are fine but I need new cleats.',datetime('now','-4 days','+18:00:00'),0),
    ('chat_044','team_004','user_coach_004','Talk to kit manager. Cleat fit matters before Sunday.',datetime('now','-4 days','+18:15:00'),0);

-- ============================================================
-- PERSONAL RECORDS
-- ============================================================

INSERT OR IGNORE INTO personal_records VALUES
    ('user_ath_001','max_hr',194.0,'bpm',datetime('now','-11 days'),'sess_002'),
    ('user_ath_001','max_speed',19.8,'km/h',datetime('now','-5 days'),'sess_004'),
    ('user_ath_001','longest_session',75.0,'min',datetime('now','-14 days'),'sess_001'),
    ('user_ath_002','max_hr',197.0,'bpm',datetime('now','-11 days'),'sess_002'),
    ('user_ath_002','max_speed',18.4,'km/h',datetime('now','-5 days'),'sess_004'),
    ('user_ath_003','max_hr',189.0,'bpm',datetime('now','-5 days'),'sess_004'),
    ('user_ath_003','max_speed',21.2,'km/h',datetime('now','-11 days'),'sess_002'),
    ('user_ath_004','max_hr',184.0,'bpm',datetime('now','-11 days'),'sess_002'),
    ('user_ath_004','max_speed',17.2,'km/h',datetime('now','-5 days'),'sess_004'),
    ('user_ath_005','max_hr',193.0,'bpm',datetime('now','-11 days'),'sess_002'),
    ('user_ath_005','max_speed',20.1,'km/h',datetime('now','-11 days'),'sess_002'),
    ('user_ath_007','max_hr',185.0,'bpm',datetime('now','-10 days'),'sess_011'),
    ('user_ath_009','max_hr',193.0,'bpm',datetime('now','-10 days'),'sess_011'),
    ('user_ath_013','max_hr',182.0,'bpm',datetime('now','-9 days'),'sess_021'),
    ('user_ath_015','max_hr',183.0,'bpm',datetime('now','-9 days'),'sess_021'),
    ('user_ath_018','max_hr',181.0,'bpm',datetime('now','-12 days'),'sess_031'),
    ('user_ath_018','max_speed',44.3,'km/h',datetime('now','-12 days'),'sess_031'),
    ('user_ath_020','max_hr',175.0,'bpm',datetime('now','-12 days'),'sess_031'),
    ('user_ath_022','max_hr',170.0,'bpm',datetime('now','-12 days'),'sess_031');
