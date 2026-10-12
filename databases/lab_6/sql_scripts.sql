-- ============================================================
-- Лабораторная работа №6
-- Построение SQL запросов: агрегатные функции, группировка,
-- подзапросы
-- ИС организации учебного процесса. Диалект: PostgreSQL
--
-- Запросы работают со схемой из лабораторной №3.
-- Все запросы только читают данные (SELECT).
--
-- Соглашение: «оценка» - это запись в rating с val IS NOT NULL.
-- Запись с is_absent = 'Y' и val = NULL (пропуск занятия)
-- оценкой не считается.
-- ============================================================


-- 1.1. Количество прошедших занятий
--      Отдельной таблицы «проведённых занятий» в схеме нет: занятие - это
--      уникальное сочетание даты, нагрузки, преподавателя и группы,
--      по которому в rating есть хотя бы одна запись (оценка или пропуск).
SELECT COUNT(*) AS lessons_held
FROM (
    SELECT DISTINCT
           r.date,
           r.study_id,
           r.prepods_id,
           s.group_id
    FROM   rating   r
    JOIN   students s ON s.id = r.student_id
) AS held;


-- 1.2. 10 лучших студентов по средней оценке
--      (при равных средних порядок определяется фамилией)
SELECT s.id,
       s.last_name,
       s.first_name,
       ROUND(AVG(r.val), 2) AS avg_mark
FROM   students s
JOIN   rating   r ON r.student_id = s.id
WHERE  r.val IS NOT NULL
GROUP  BY s.id, s.last_name, s.first_name
ORDER  BY avg_mark DESC, s.last_name
LIMIT  10;


-- 1.3. Количество предметов и типов занятий, по которым стоят оценки
SELECT COUNT(DISTINCT sl.subject_id)    AS subjects_count,
       COUNT(DISTINCT sl.type_study_id) AS load_types_count
FROM   rating     r
JOIN   study_load sl ON sl.id = r.study_id
WHERE  r.val IS NOT NULL;


-- 1.4. Список преподавателей, отсортированный по количеству
--      поставленных оценок (по убыванию).
--      LEFT JOIN: преподаватели без оценок тоже выводятся (0).
--      Условие на val стоит в ON, а не в WHERE, чтобы не отбросить их.
SELECT p.id,
       p.last_name,
       p.first_name,
       COUNT(r.id) AS marks_count
FROM   prepods p
LEFT   JOIN rating r ON r.prepods_id = p.id
                    AND r.val IS NOT NULL
GROUP  BY p.id, p.last_name, p.first_name
ORDER  BY marks_count DESC, p.last_name;


-- 1.5. Фамилии студентов, у которых нет ни одной оценки
SELECT s.last_name
FROM   students s
LEFT   JOIN rating r ON r.student_id = s.id
                    AND r.val IS NOT NULL
WHERE  r.id IS NULL
ORDER  BY s.last_name;


-- 1.6. Информация о студенте, средняя оценка которого максимальна
--      (если максимум у нескольких студентов - выводятся все)
WITH avg_marks AS (
    SELECT student_id,
           AVG(val) AS avg_val
    FROM   rating
    WHERE  val IS NOT NULL
    GROUP  BY student_id
)
SELECT s.id,
       s.last_name,
       s.first_name,
       g.name               AS student_group,
       ROUND(a.avg_val, 2)  AS avg_mark
FROM   avg_marks a
JOIN   students  s ON s.id = a.student_id
LEFT   JOIN groups g ON g.id = s.group_id
WHERE  a.avg_val = (SELECT MAX(avg_val) FROM avg_marks);