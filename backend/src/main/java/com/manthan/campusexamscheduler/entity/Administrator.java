package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "administrators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Administrator {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long adminId;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Column(nullable = false, length = 255)
    private String password;

}
