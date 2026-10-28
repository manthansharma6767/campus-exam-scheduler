package com.manthan.campusexamscheduler.exception;

public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException(String message) {
        super(message);
    }
}
