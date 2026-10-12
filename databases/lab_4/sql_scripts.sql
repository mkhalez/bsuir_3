-- ============================================================
-- Лабораторная работа №4
-- Проектирование DML запросов SQL (INSERT / UPDATE)
-- ИС организации учебного процесса. Диалект: PostgreSQL
--
-- Скрипт наполняет базу данными (задания 1-8).
-- Очистка базы (задание 9) вынесена в отдельный файл lab4_clear.sql.
--
-- Идентификаторы в схеме не генерируются автоматически,
-- поэтому id указаны явно.
--
-- ВАЖНО: groups.faculty_id объявлен NOT NULL, поэтому группа не
-- может существовать без факультета. Из-за этого задание 4
-- (факультеты) выполняется первым, до задания 1 (группа).
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- Задание 4. Добавить факультеты КСИС и ФИТУ
-- ------------------------------------------------------------
INSERT INTO faculty (id, name) VALUES (1, 'КСИС');
INSERT INTO faculty (id, name) VALUES (2, 'ФИТУ');

-- ------------------------------------------------------------
-- Задание 1. Добавить новую группу с номером 123456
--   head_group (староста) пока NULL - студентов ещё нет,
--   староста назначается в задании 3.
-- ------------------------------------------------------------
INSERT INTO groups (id, name, head_group, faculty_id)
VALUES (1, '123456', NULL, 1);

-- ------------------------------------------------------------
-- Задание 2. Внести в группу 123456 студентов Иванова и Петрова
-- ------------------------------------------------------------
INSERT INTO students (id, last_name, first_name, group_id)
VALUES (1, 'Иванов', 'Иван', 1);

INSERT INTO students (id, last_name, first_name, group_id)
VALUES (2, 'Петров', 'Пётр', 1);

-- ------------------------------------------------------------
-- Задание 3. Назначить студента Иванова старостой группы
-- ------------------------------------------------------------
UPDATE groups
SET    head_group = (SELECT id FROM students WHERE last_name = 'Иванов' AND group_id = 1)
WHERE  name = '123456';

-- ------------------------------------------------------------
-- Задание 5. Добавить преподавателей Васечкина и Петечкина
--            на факультеты КСИС и ФИТУ соответственно
-- ------------------------------------------------------------
INSERT INTO prepods (id, last_name, first_name, faculty_id)
VALUES (1, 'Васечкин', NULL, (SELECT id FROM faculty WHERE name = 'КСИС'));

INSERT INTO prepods (id, last_name, first_name, faculty_id)
VALUES (2, 'Петечкин', NULL, (SELECT id FROM faculty WHERE name = 'ФИТУ'));

-- ------------------------------------------------------------
-- Задание 6. Назначить преподавателю Васечкину ведение предмета
--            «Введение в специальность» для группы 123456
--   Цепочка: вид нагрузки -> дисциплина -> учебная нагрузка -> занятие
-- ------------------------------------------------------------
INSERT INTO type_study_load (id, name) VALUES (1, 'Лекция');
INSERT INTO type_study_load (id, name) VALUES (2, 'Лабораторная работа');

INSERT INTO subjects (id, name, faculty_id)
VALUES (1, 'Введение в специальность', 1);

INSERT INTO study_load (id, hours, subject_id, type_study_id)
VALUES ('INTRO-LEC-1', 32, 1, 1);

INSERT INTO lessons (group_id, prepod_id, study_id)
VALUES (
    (SELECT id FROM groups   WHERE name = '123456'),
    (SELECT id FROM prepods  WHERE last_name = 'Васечкин'),
    'INTRO-LEC-1'
);

-- ------------------------------------------------------------
-- Задание 7. Поставить студенту Иванову по предмету «Введение в
--            специальность» оценку 10, отметить отсутствие Петрова
--   Оценка val = NULL при is_absent = 'Y' (CHECK на val пропускает NULL).
-- ------------------------------------------------------------
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent)
VALUES (1, DATE '2026-09-14', 10, 1, 'INTRO-LEC-1', 1, 'N');

INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent)
VALUES (2, DATE '2026-09-14', NULL, 2, 'INTRO-LEC-1', 1, 'Y');

-- ------------------------------------------------------------
-- Задание 8. Наполнить базу: минимум 4 предмета, по каждому не менее
--            3 отметок, отметки у минимум 5 студентов
--   Оценки в диапазоне 4..10 (CHECK val > 3 AND val <= 10).
-- ------------------------------------------------------------

-- Ещё три студента группы (всего 5: Иванов, Петров + новые)
INSERT INTO students (id, last_name, first_name, group_id) VALUES (3, 'Сидоров',  'Сергей', 1);
INSERT INTO students (id, last_name, first_name, group_id) VALUES (4, 'Кузнецова', 'Анна',  1);
INSERT INTO students (id, last_name, first_name, group_id) VALUES (5, 'Смирнов',  'Алексей', 1);

-- Ещё три предмета (вместе с «Введением в специальность» - 4)
INSERT INTO subjects (id, name, faculty_id) VALUES (2, 'Математика',       1);
INSERT INTO subjects (id, name, faculty_id) VALUES (3, 'Программирование', 1);
INSERT INTO subjects (id, name, faculty_id) VALUES (4, 'Базы данных',      2);

-- Учебная нагрузка по новым предметам
INSERT INTO study_load (id, hours, subject_id, type_study_id) VALUES ('MATH-LEC-1', 64, 2, 1);
INSERT INTO study_load (id, hours, subject_id, type_study_id) VALUES ('PROG-LAB-1', 48, 3, 2);
INSERT INTO study_load (id, hours, subject_id, type_study_id) VALUES ('DB-LAB-1',   48, 4, 2);

-- Занятия группы 123456: Васечкин - математика, Петечкин - программирование и БД
INSERT INTO lessons (group_id, prepod_id, study_id) VALUES (1, 1, 'MATH-LEC-1');
INSERT INTO lessons (group_id, prepod_id, study_id) VALUES (1, 2, 'PROG-LAB-1');
INSERT INTO lessons (group_id, prepod_id, study_id) VALUES (1, 2, 'DB-LAB-1');

-- Введение в специальность: отметки остальных студентов
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (3,  DATE '2026-09-14', 8, 3, 'INTRO-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (4,  DATE '2026-09-14', 9, 4, 'INTRO-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (5,  DATE '2026-09-14', 7, 5, 'INTRO-LEC-1', 1, 'N');
-- Петров пропустил занятие 14.09, оценку получил на следующем
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (21, DATE '2026-09-21', 6, 2, 'INTRO-LEC-1', 1, 'N');

-- Математика (преподаватель 1)
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (6,  DATE '2026-09-16',  9, 1, 'MATH-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (7,  DATE '2026-09-16',  5, 2, 'MATH-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (8,  DATE '2026-09-16',  8, 3, 'MATH-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (9,  DATE '2026-09-16', 10, 4, 'MATH-LEC-1', 1, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (10, DATE '2026-09-16',  6, 5, 'MATH-LEC-1', 1, 'N');

-- Программирование (преподаватель 2)
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (11, DATE '2026-09-18', 10, 1, 'PROG-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (12, DATE '2026-09-18',  7, 2, 'PROG-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (13, DATE '2026-09-18',  6, 3, 'PROG-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (14, DATE '2026-09-18',  9, 4, 'PROG-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (15, DATE '2026-09-18',  8, 5, 'PROG-LAB-1', 2, 'N');

-- Базы данных (преподаватель 2)
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (16, DATE '2026-09-21',  8, 1, 'DB-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (17, DATE '2026-09-21',  4, 2, 'DB-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (18, DATE '2026-09-21',  9, 3, 'DB-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (19, DATE '2026-09-21',  7, 4, 'DB-LAB-1', 2, 'N');
INSERT INTO rating (id, date, val, student_id, study_id, prepods_id, is_absent) VALUES (20, DATE '2026-09-21', 10, 5, 'DB-LAB-1', 2, 'N');

COMMIT;

-- ------------------------------------------------------------
-- Проверка задания 8: по каждому предмету число отметок и число
-- студентов, получивших отметки (ожидается >= 3 и >= 5)
-- ------------------------------------------------------------
SELECT s.name                       AS subject,
       COUNT(r.val)                 AS marks,
       COUNT(DISTINCT r.student_id) AS students_with_marks
FROM   subjects s
JOIN   study_load sl ON sl.subject_id = s.id
JOIN   rating r      ON r.study_id = sl.id AND r.val IS NOT NULL
GROUP  BY s.name
ORDER  BY s.name;


-- ============================================================
-- Лабораторная работа №4, задание 9. Очистить базу
-- Удаляет все данные из всех таблиц, сами таблицы остаются.
-- ============================================================

TRUNCATE TABLE exams, schedule, semesters, classrooms, departments,
               rating, lessons, study_load, students, groups,
               prepods, subjects, type_study_load, faculty
CASCADE;

-- Проверка: во всех таблицах должно быть 0 строк
SELECT 'faculty' AS tbl, COUNT(*) FROM faculty
UNION ALL SELECT 'groups',   COUNT(*) FROM groups
UNION ALL SELECT 'students', COUNT(*) FROM students
UNION ALL SELECT 'prepods',  COUNT(*) FROM prepods
UNION ALL SELECT 'subjects', COUNT(*) FROM subjects
UNION ALL SELECT 'study_load', COUNT(*) FROM study_load
UNION ALL SELECT 'lessons',  COUNT(*) FROM lessons
UNION ALL SELECT 'rating',   COUNT(*) FROM rating;
