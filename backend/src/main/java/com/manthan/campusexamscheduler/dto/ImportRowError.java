package com.manthan.campusexamscheduler.dto;

<<<<<<< HEAD
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportRowError {
    int row;
    String message;
}
=======
public record ImportRowError(int rowNumber, String message) { }
>>>>>>> 2f4335a172fae9329362d4f32284c1e6aaa1cad0
