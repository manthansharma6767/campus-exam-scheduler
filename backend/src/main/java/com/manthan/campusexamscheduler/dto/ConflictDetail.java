package com.manthan.campusexamscheduler.dto;

<<<<<<< HEAD
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictDetail {
    int row;
    String type;
    String message;
}
=======
public record ConflictDetail(int rowNumber, String type, String message) { }
>>>>>>> 2f4335a172fae9329362d4f32284c1e6aaa1cad0
