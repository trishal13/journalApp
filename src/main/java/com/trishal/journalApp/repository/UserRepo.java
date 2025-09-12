package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {

    User findByUserName(String userName);

    User deleteByUserName(String userName);
}
