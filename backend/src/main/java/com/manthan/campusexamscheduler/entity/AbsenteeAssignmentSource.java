package com.manthan.campusexamscheduler.entity;

/**
 * Identifies who created or last claimed an absentee-paper mapping.
 * ADMIN mappings may be locked so student self-service cannot remove them.
 */
public enum AbsenteeAssignmentSource {
    ADMIN,
    STUDENT
}
