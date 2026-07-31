package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Administrator;
import com.manthan.campusexamscheduler.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmail(String email);

    boolean existsByEmail(String email);

}