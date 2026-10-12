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
-- Лабораторная работа №3 - расширение схемы (5 новых сущностей)
-- Диалект: PostgreSQL
-- Выполнять ПОСЛЕ основного скрипта (faculty ... rating).
--
-- Новые таблицы:
--   1. departments - кафедры
--   2. classrooms  - аудитории
--   3. semesters   - учебные семестры
--   4. schedule    - расписание занятий
--   5. exams       - экзамены и зачёты
--
-- Для повторного запуска (раскомментировать при необходимости):
-- ALTER TABLE prepods DROP COLUMN IF EXISTS department_id;
-- DROP TABLE IF EXISTS exams, schedule, semesters, classrooms, departments CASCADE;
-- ============================================================


-- ------------------------------------------------------------
-- 1. DEPARTMENTS - кафедры (входят в состав факультета)
-- ------------------------------------------------------------
CREATE TABLE departments
(
    id         INTEGER      NOT NULL,
    name       VARCHAR(120) NOT NULL,
    faculty_id INTEGER      NOT NULL,
    CONSTRAINT pk_departments         PRIMARY KEY (id),
    CONSTRAINT uq_departments_name    UNIQUE (faculty_id, name),
    CONSTRAINT fk_faculty_departments FOREIGN KEY (faculty_id)
        REFERENCES faculty (id)
);

-- Преподаватель принадлежит кафедре (связь с существующей таблицей)
ALTER TABLE prepods
    ADD COLUMN department_id INTEGER;

ALTER TABLE prepods
    ADD CONSTRAINT fk_department_prepods FOREIGN KEY (department_id)
        REFERENCES departments (id);

-- ------------------------------------------------------------
-- 2. CLASSROOMS - аудитории
-- ------------------------------------------------------------
CREATE TABLE classrooms
(
    id       INTEGER     NOT NULL,
    building VARCHAR(10) NOT NULL,
    room_no  VARCHAR(10) NOT NULL,
    capacity INTEGER     NOT NULL,
    CONSTRAINT pk_classrooms      PRIMARY KEY (id),
    CONSTRAINT uq_classrooms_room UNIQUE (building, room_no),
    CONSTRAINT chkClassroomCap    CHECK (capacity > 0)
);

-- ------------------------------------------------------------
-- 3. SEMESTERS - учебные семестры
-- ------------------------------------------------------------
CREATE TABLE semesters
(
    id         INTEGER     NOT NULL,
    name       VARCHAR(30) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    CONSTRAINT pk_semesters      PRIMARY KEY (id),
    CONSTRAINT uq_semesters_name UNIQUE (name),
    CONSTRAINT chkSemesterDates  CHECK (end_date > start_date)
);

-- ------------------------------------------------------------
-- 4. SCHEDULE - расписание занятий
--    Занятие берётся из LESSONS (группа + преподаватель + нагрузка),
--    к нему добавляются аудитория, семестр, день недели и номер пары.
-- ------------------------------------------------------------
CREATE TABLE schedule
(
    id           INTEGER  NOT NULL,
    group_id     INTEGER  NOT NULL,
    prepod_id    INTEGER  NOT NULL,
    study_id     CHAR(20) NOT NULL,
    classroom_id INTEGER  NOT NULL,
    semester_id  INTEGER  NOT NULL,
    weekday      INTEGER  NOT NULL,
    pair_no      INTEGER  NOT NULL,
    CONSTRAINT pk_schedule           PRIMARY KEY (id),
    CONSTRAINT chkScheduleWeekday    CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chkSchedulePair       CHECK (pair_no BETWEEN 1 AND 8),
    CONSTRAINT fk_lessons_schedule   FOREIGN KEY (group_id, prepod_id, study_id)
        REFERENCES lessons (group_id, prepod_id, study_id),
    CONSTRAINT fk_classroom_schedule FOREIGN KEY (classroom_id)
        REFERENCES classrooms (id),
    CONSTRAINT fk_semester_schedule  FOREIGN KEY (semester_id)
        REFERENCES semesters (id),
    -- в одно время нет двух занятий у группы / преподавателя / в аудитории
    CONSTRAINT uq_schedule_group     UNIQUE (semester_id, weekday, pair_no, group_id),
    CONSTRAINT uq_schedule_prepod    UNIQUE (semester_id, weekday, pair_no, prepod_id),
    CONSTRAINT uq_schedule_room      UNIQUE (semester_id, weekday, pair_no, classroom_id)
);

-- ------------------------------------------------------------
-- 5. EXAMS - экзамены и зачёты группы по нагрузке
-- ------------------------------------------------------------
CREATE TABLE exams
(
    id           INTEGER     NOT NULL,
    group_id     INTEGER     NOT NULL,
    prepod_id    INTEGER     NOT NULL,
    study_id     CHAR(20)    NOT NULL,
    classroom_id INTEGER     NOT NULL,
    semester_id  INTEGER     NOT NULL,
    exam_date    DATE        NOT NULL,
    exam_form    VARCHAR(10) NOT NULL DEFAULT 'EXAM',
    CONSTRAINT pk_exams           PRIMARY KEY (id),
    CONSTRAINT chkExamForm        CHECK (exam_form IN ('EXAM', 'CREDIT')),
    CONSTRAINT uq_exams           UNIQUE (group_id, study_id, semester_id, exam_form),
    CONSTRAINT fk_lessons_exams   FOREIGN KEY (group_id, prepod_id, study_id)
        REFERENCES lessons (group_id, prepod_id, study_id),
    CONSTRAINT fk_classroom_exams FOREIGN KEY (classroom_id)
        REFERENCES classrooms (id),
    CONSTRAINT fk_semester_exams  FOREIGN KEY (semester_id)
        REFERENCES semesters (id)
);

