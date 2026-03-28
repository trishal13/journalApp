package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {

    User findByUserName(String userName);

    boolean existsByUserName(String userName);

    @Transactional
    void deleteByUserName(String userName);
}