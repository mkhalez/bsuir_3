-- ============================================================
-- Лабораторная работа №3
-- Проектирование схемы базы данных в SQL (DDL)
-- ИС организации учебного процесса. Диалект: PostgreSQL
--
-- Соответствие типов Oracle -> PostgreSQL:
--   NUMBER        -> INTEGER
--   NUMBER(10,0)  -> NUMERIC(10,0)
--   VARCHAR2(n)   -> VARCHAR(n)
--   CHAR(n)       -> CHAR(n)
--   DATE          -> DATE
--
-- Порядок: сначала справочники без внешних ключей, затем зависимые
-- таблицы. Циклическая связь GROUPS <-> STUDENTS (староста группы)
-- замыкается в конце через ALTER TABLE.
-- ============================================================

-- Для повторного запуска (раскомментировать при необходимости):
-- DROP TABLE IF EXISTS rating, lessons, study_load, students, groups,
--                      prepods, subjects, type_study_load, faculty CASCADE;


-- ------------------------------------------------------------
-- 1. FACULTY - справочник факультетов
-- ------------------------------------------------------------
CREATE TABLE faculty
(
    id   INTEGER     NOT NULL,
    name VARCHAR(30) NOT NULL,
    CONSTRAINT pk_faculty      PRIMARY KEY (id),
    CONSTRAINT uq_faculty_name UNIQUE (name)
);

-- ------------------------------------------------------------
-- 2. TYPE_STUDY_LOAD - справочник видов учебной нагрузки
-- ------------------------------------------------------------
CREATE TABLE type_study_load
(
    id   NUMERIC(10,0) NOT NULL,
    name VARCHAR(120)  NOT NULL,
    CONSTRAINT pk_type_study_load      PRIMARY KEY (id),
    CONSTRAINT uq_type_study_load_name UNIQUE (name)
);

-- ------------------------------------------------------------
-- 3. SUBJECTS - справочник дисциплин
-- ------------------------------------------------------------
CREATE TABLE subjects
(
    id         INTEGER      NOT NULL,
    name       VARCHAR(120) NOT NULL,
    faculty_id INTEGER,
    CONSTRAINT pk_subjects         PRIMARY KEY (id),
    CONSTRAINT fk_faculty_subjects FOREIGN KEY (faculty_id)
        REFERENCES faculty (id)
);

-- ------------------------------------------------------------
-- 4. PREPODS - справочник преподавателей
-- ------------------------------------------------------------
CREATE TABLE prepods
(
    id         INTEGER      NOT NULL,
    last_name  VARCHAR(120) NOT NULL,
    first_name VARCHAR(120),
    faculty_id INTEGER      NOT NULL,
    CONSTRAINT pk_prepods        PRIMARY KEY (id),
    CONSTRAINT fk_study_prepods  FOREIGN KEY (faculty_id)
        REFERENCES faculty (id)
);

-- ------------------------------------------------------------
-- 5. STUDY_LOAD - учебная нагрузка
-- ------------------------------------------------------------
CREATE TABLE study_load
(
    id            CHAR(20)      NOT NULL,
    hours         INTEGER,
    subject_id    INTEGER       NOT NULL,
    type_study_id NUMERIC(10,0) NOT NULL,
    CONSTRAINT pk_study_load    PRIMARY KEY (id),
    CONSTRAINT chkStudyLoad     CHECK (hours >= 0),
    CONSTRAINT fk_subject_study FOREIGN KEY (subject_id)
        REFERENCES subjects (id),
    CONSTRAINT fk_type_study    FOREIGN KEY (type_study_id)
        REFERENCES type_study_load (id)
);

-- ------------------------------------------------------------
-- 6. GROUPS - справочник групп
--    Внешний ключ head_group (староста) добавляется ниже,
--    после создания таблицы STUDENTS.
-- ------------------------------------------------------------
CREATE TABLE groups
(
    id         INTEGER     NOT NULL,
    name       VARCHAR(10) NOT NULL,
    head_group INTEGER,
    faculty_id INTEGER     NOT NULL,
    CONSTRAINT pk_group         PRIMARY KEY (id),
    CONSTRAINT fk_faculty_group FOREIGN KEY (faculty_id)
        REFERENCES faculty (id)
);

-- ------------------------------------------------------------
-- 7. STUDENTS - справочник студентов
-- ------------------------------------------------------------
CREATE TABLE students
(
    id         INTEGER     NOT NULL,
    last_name  VARCHAR(60) NOT NULL,
    first_name VARCHAR(30),
    group_id   INTEGER,
    CONSTRAINT pk_students PRIMARY KEY (id)
);

-- Замыкаем циклическую связь GROUPS <-> STUDENTS
ALTER TABLE students
    ADD CONSTRAINT fk_group_of_students FOREIGN KEY (group_id)
        REFERENCES groups (id);

ALTER TABLE groups
    ADD CONSTRAINT fk_head_of_group FOREIGN KEY (head_group)
        REFERENCES students (id);

-- ------------------------------------------------------------
-- 8. LESSONS - занятия (группа + преподаватель + нагрузка)
-- ------------------------------------------------------------
CREATE TABLE lessons
(
    group_id  INTEGER  NOT NULL,
    prepod_id INTEGER  NOT NULL,
    study_id  CHAR(20) NOT NULL,
    CONSTRAINT pk_lessons          PRIMARY KEY (group_id, prepod_id, study_id),
    CONSTRAINT fk_group_lessons    FOREIGN KEY (group_id)
        REFERENCES groups (id),
    CONSTRAINT fk_prepods_lessons  FOREIGN KEY (prepod_id)
        REFERENCES prepods (id),
    CONSTRAINT fk_lessons_study_load FOREIGN KEY (study_id)
        REFERENCES study_load (id)
);

-- ------------------------------------------------------------
-- 9. RATING - отметки студентов
-- ------------------------------------------------------------
CREATE TABLE rating
(
    id          INTEGER  NOT NULL,
    date        DATE     NOT NULL,
    val         INTEGER,
    student_id  INTEGER  NOT NULL,
    study_id    CHAR(20) NOT NULL,
    prepods_id  INTEGER  NOT NULL,
    is_absent   CHAR(1)  DEFAULT 'N',
    CONSTRAINT pk_rating          PRIMARY KEY (id),
    CONSTRAINT chkValue           CHECK (val > 3 AND val <= 10),
    CONSTRAINT fk_student_rating  FOREIGN KEY (student_id)
        REFERENCES students (id),
    CONSTRAINT fk_study_lessons   FOREIGN KEY (study_id)
        REFERENCES study_load (id),
    CONSTRAINT fk_prepod_rating   FOREIGN KEY (prepods_id)
        REFERENCES prepods (id)
);


-- ============================================================
-- Комментарии к таблицам и столбцам (поле Comments из схемы)
-- ============================================================
COMMENT ON TABLE faculty         IS 'Справочник факультетов';
COMMENT ON TABLE type_study_load IS 'Виды учебной нагрузки';
COMMENT ON TABLE subjects        IS 'Справочник дисциплин';
COMMENT ON TABLE prepods         IS 'Справочник преподавателей';
COMMENT ON TABLE study_load      IS 'Учебная нагрузка';
COMMENT ON TABLE groups          IS 'Информация о группах';
COMMENT ON TABLE students        IS 'Справочник студентов';
COMMENT ON TABLE lessons         IS 'Нагрузка преподавателя';
COMMENT ON TABLE rating          IS 'Отметки, полученные студентами';

COMMENT ON COLUMN faculty.id   IS 'Первичный ключ';
COMMENT ON COLUMN faculty.name IS 'Название факультета';

COMMENT ON COLUMN type_study_load.id   IS 'Первичный ключ';
COMMENT ON COLUMN type_study_load.name IS 'Тип учебной нагрузки';

COMMENT ON COLUMN subjects.id         IS 'Первичный ключ';
COMMENT ON COLUMN subjects.name       IS 'Название дисциплины';
COMMENT ON COLUMN subjects.faculty_id IS 'Факультет, на который распределена нагрузка';

COMMENT ON COLUMN prepods.id         IS 'Первичный ключ';
COMMENT ON COLUMN prepods.last_name  IS 'Фамилия преподавателя';
COMMENT ON COLUMN prepods.first_name IS 'Имя преподавателя';
COMMENT ON COLUMN prepods.faculty_id IS 'Факультет, на котором работает преподаватель';

COMMENT ON COLUMN study_load.id            IS 'Первичный ключ';
COMMENT ON COLUMN study_load.hours         IS 'Количество планируемых часов на дисциплину';
COMMENT ON COLUMN study_load.subject_id    IS 'Предмет';
COMMENT ON COLUMN study_load.type_study_id IS 'Тип нагрузки';

COMMENT ON COLUMN groups.id         IS 'Первичный ключ';
COMMENT ON COLUMN groups.name       IS 'Номер группы';
COMMENT ON COLUMN groups.head_group IS 'Староста группы';
COMMENT ON COLUMN groups.faculty_id IS 'Факультет, к которому принадлежит группа';

COMMENT ON COLUMN students.id         IS 'Первичный ключ';
COMMENT ON COLUMN students.last_name  IS 'Фамилия студента';
COMMENT ON COLUMN students.first_name IS 'Имя студента';
COMMENT ON COLUMN students.group_id   IS 'Группа, в которой обучается студент';

COMMENT ON COLUMN lessons.group_id  IS 'Группа';
COMMENT ON COLUMN lessons.prepod_id IS 'Преподаватель';
COMMENT ON COLUMN lessons.study_id  IS 'Учебная нагрузка';

COMMENT ON COLUMN rating.id         IS 'Первичный ключ';
COMMENT ON COLUMN rating.date       IS 'Дата получения отметки';
COMMENT ON COLUMN rating.val        IS 'Полученная отметка';
COMMENT ON COLUMN rating.student_id IS 'Студент';
COMMENT ON COLUMN rating.study_id   IS 'Учебный предмет';
COMMENT ON COLUMN rating.prepods_id IS 'Преподаватель';
COMMENT ON COLUMN rating.is_absent  IS 'Отсутствовал ли студент на занятии';


