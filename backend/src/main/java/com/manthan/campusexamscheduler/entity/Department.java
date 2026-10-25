package com.manthan.campusexamscheduler.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long departmentId;
    // what if client sends empty value for this use not blank if there is empty value not blank will not accept it
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(
            // database constraint
            nullable = false,
            unique = true,
            length = 100
    )
    private String departmentName;

}
