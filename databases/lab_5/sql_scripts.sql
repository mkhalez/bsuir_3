-- ============================================================
-- Лабораторная работа №5
-- Построение простых SQL запросов
-- ИС организации учебного процесса. Диалект: PostgreSQL
--
-- Запросы работают со схемой из лабораторной №3.
-- Все запросы только читают данные (SELECT).
-- ============================================================


-- ============================================================
-- 1. ОДНОТАБЛИЧНЫЕ ЗАПРОСЫ
-- ============================================================

-- 1.1. Все данные обо всех преподавателях
SELECT *
FROM   prepods;


-- 1.2. Фамилия и имя студентов первой группы (id = 1),
--      сортировка по фамилии в алфавитном порядке
SELECT last_name,
       first_name
FROM   students
WHERE  group_id = 1
ORDER  BY last_name ASC;


-- 1.3. Данные о студентах, фамилии которых начинаются на букву «О»
SELECT *
FROM   students
WHERE  last_name LIKE 'О%';


-- ============================================================
-- 2. МНОГОТАБЛИЧНЫЕ ЗАПРОСЫ
-- ============================================================

-- 2.1. Все оценки, которые поставил преподаватель Проволоцкий В.Е.
--      в октябре 2020 года
--      (в таблице prepods хранятся только фамилия и имя,
--       поэтому преподаватель отбирается по фамилии)
SELECT r.id,
       r.date,
       r.val,
       r.student_id,
       r.study_id
FROM   rating r
JOIN   prepods p ON p.id = r.prepods_id
WHERE  p.last_name = 'Проволоцкий'
  AND  r.val IS NOT NULL
  AND  r.date >= DATE '2020-10-01'
  AND  r.date <  DATE '2020-11-01'
ORDER  BY r.date;


-- 2.2. Все студенты, которые отсутствовали на лекциях
--      (DISTINCT: студент выводится один раз, сколько бы лекций ни пропустил)
SELECT DISTINCT
       s.id,
       s.last_name,
       s.first_name
FROM   rating r
JOIN   students        s   ON s.id   = r.student_id
JOIN   study_load      sl  ON sl.id  = r.study_id
JOIN   type_study_load tsl ON tsl.id = sl.type_study_id
WHERE  r.is_absent = 'Y'
  AND  tsl.name ILIKE 'лекц%'
ORDER  BY s.last_name, s.first_name;


-- 2.3. Информация об оценке: факультет, группа, ФИО студента,
--      ФИО преподавателя и дата выставления
SELECT r.id          AS rating_id,
       r.val         AS mark,
       f.name        AS faculty,
       g.name        AS student_group,
       s.last_name   AS student_last_name,
       s.first_name  AS student_first_name,
       p.last_name   AS prepod_last_name,
       p.first_name  AS prepod_first_name,
       r.date        AS mark_date
FROM   rating   r
JOIN   students s ON s.id = r.student_id
JOIN   groups   g ON g.id = s.group_id
JOIN   faculty  f ON f.id = g.faculty_id
JOIN   prepods  p ON p.id = r.prepods_id
WHERE  r.val IS NOT NULL
ORDER  BY r.date, g.name, s.last_name;


-- 2.4. Занятия, во время которых оценки выставлялись не основными
--      преподавателями (то есть теми, кто не включён в план: в таблице
--      LESSONS нет записи «группа студента + преподаватель + нагрузка»)
SELECT r.id          AS rating_id,
       r.date        AS mark_date,
       g.name        AS student_group,
       sb.name       AS subject,
       tsl.name      AS load_type,
       p.last_name   AS prepod_last_name,
       s.last_name   AS student_last_name
FROM   rating   r
JOIN   students        s   ON s.id   = r.student_id
JOIN   prepods         p   ON p.id   = r.prepods_id
JOIN   study_load      sl  ON sl.id  = r.study_id
JOIN   subjects        sb  ON sb.id  = sl.subject_id
JOIN   type_study_load tsl ON tsl.id = sl.type_study_id
LEFT   JOIN groups     g   ON g.id   = s.group_id
WHERE  NOT EXISTS (
           SELECT 1
           FROM   lessons l
           WHERE  l.group_id  = s.group_id
             AND  l.prepod_id = r.prepods_id
             AND  l.study_id  = r.study_id
       )
ORDER  BY r.date;


-- 2.5. Список преподавателей (по фамилиям, по возрастанию) и номера
--      групп, в которых они ставили оценки
--      (INNER JOIN: только преподаватели, которые ставили оценки)
SELECT DISTINCT
       p.last_name,
       p.first_name,
       g.name AS student_group
FROM   prepods  p
JOIN   rating   r ON r.prepods_id = p.id
JOIN   students s ON s.id = r.student_id
JOIN   groups   g ON g.id = s.group_id
ORDER  BY p.last_name ASC, g.name;


-- 2.6. Список ВСЕХ преподавателей (по фамилиям, по возрастанию) и номера
--      групп, в которых они ставили оценки
--      (LEFT JOIN: преподаватели без оценок тоже выводятся, группа = NULL)
SELECT DISTINCT
       p.last_name,
       p.first_name,
       g.name AS student_group
FROM   prepods  p
LEFT   JOIN rating   r ON r.prepods_id = p.id
LEFT   JOIN students s ON s.id = r.student_id
LEFT   JOIN groups   g ON g.id = s.group_id
ORDER  BY p.last_name ASC, g.name;


-- 2.7. Коды и названия факультетов, в которых нет групп
SELECT f.id,
       f.name
FROM   faculty f
LEFT   JOIN groups g ON g.faculty_id = f.id
WHERE  g.id IS NULL
ORDER  BY f.id;