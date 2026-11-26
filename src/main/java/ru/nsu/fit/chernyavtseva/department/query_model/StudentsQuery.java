package ru.nsu.fit.chernyavtseva.department.query_model;

import ru.nsu.fit.chernyavtseva.department.Degree;

/**
 * SQL-запрос, который заменил прежний SPARQL-запрос к онтологии: те же данные
 * (студент, его ВКР с руководителями и рецензентом, практика с руководителями),
 * но из таблиц базы данных.
 */
public record StudentsQuery(String degree, int course, String profile) {

    public StudentsQuery(Degree degree) {
        this(degree.name(), degree.course(), degree.profile());
    }

    private static final String QUERY_TEMPLATE = """
            SELECT s.full_name                            AS student_full_name,
                   s.group_name                           AS student_group,
                   d.name                                 AS department_name,
                   d.head_name                            AS department_head,
                   t.topic                                AS thesis_topic,
                   supervisor.full_name                   AS supervisor_full_name,
                   supervisor.thesis_position             AS supervisor_position,
                   supervisor.thesis_position_short       AS supervisor_position_short,
                   supervisor.academic_degree             AS supervisor_degree,
                   co_supervisor.full_name                AS co_supervisor_full_name,
                   co_supervisor.position                 AS co_supervisor_position,
                   reviewer.full_name                     AS reviewer_full_name,
                   reviewer.position                      AS reviewer_position,
                   practice.place                         AS practice_place,
                   COALESCE(practice.full_place, practice.place) AS practice_full_place,
                   practice.organization_name             AS practice_organization,
                   practice.order_number                  AS practice_order,
                   ngsu_supervisor.full_name              AS ngsu_supervisor_full_name,
                   ngsu_supervisor.ngsu_position          AS ngsu_supervisor_position,
                   org_supervisor.full_name               AS organization_supervisor_full_name,
                   org_supervisor.organization_position   AS organization_supervisor_position,
                   org_supervisor.signature_name          AS organization_supervisor_signature,
                   org_supervisor.bachelor_date           AS organization_supervisor_bachelor_date,
                   org_supervisor.bachelor_knis_date      AS organization_supervisor_bachelor_knis_date,
                   org_supervisor.master_mda_date         AS organization_supervisor_master_mda_date,
                   org_supervisor.master_trps_date        AS organization_supervisor_master_trps_date
            FROM student s
                     JOIN department d ON d.id = s.department_id
                     LEFT JOIN thesis t ON t.id = s.thesis_id
                     LEFT JOIN person supervisor ON supervisor.id = t.supervisor_id
                     LEFT JOIN person co_supervisor ON co_supervisor.id = t.co_supervisor_id
                     LEFT JOIN person reviewer ON reviewer.id = t.reviewer_id
                     LEFT JOIN practice ON practice.student_id = s.id
                     LEFT JOIN person ngsu_supervisor ON ngsu_supervisor.id = practice.ngsu_supervisor_id
                     LEFT JOIN person org_supervisor ON org_supervisor.id = practice.organization_supervisor_id
            WHERE s.degree = ? AND s.course = ? AND s.profile = ?
            ORDER BY s.full_name
            """;

    String create() {
        return QUERY_TEMPLATE;
    }
}
