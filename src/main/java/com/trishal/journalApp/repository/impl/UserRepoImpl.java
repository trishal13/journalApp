package com.trishal.journalApp.repository.impl;

import com.trishal.journalApp.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

public class UserRepoImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public List<User> getUsersForSentimentAnalysis(){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);

        Root<User> user = cq.from(User.class);

        cq.select(user).where(cb.and(
                cb.isNotNull(
                        cb.function("regexp_match", String.class, user.get("email"),
                                cb.literal("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"))
                ),
                cb.isTrue(user.get("sentimentAnalysis"))
        ));

        return entityManager.createQuery(cq).getResultList();
    }

}
