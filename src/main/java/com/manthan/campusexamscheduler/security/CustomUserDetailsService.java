package com.manthan.campusexamscheduler.security;

import com.manthan.campusexamscheduler.entity.Administrator;
import com.manthan.campusexamscheduler.repository.AdministratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdministratorRepository administratorRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Administrator administrator = administratorRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Administrator not found"));

        return new CustomUserDetails(administrator);
    }
}