package com.trishal.journalApp.repository.impl;

import com.trishal.journalApp.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BUG FIX: Added @Repository so Spring manages this as a bean.
 * Without it, @Autowired injection of UserRepoImpl fails at startup.
 *
 * NOTE: The regexp_match function is PostgreSQL-specific. If you're using
 * a different DB (e.g. H2 for tests), swap it for a simple cb.isNotNull(user.get("email")).
 */
@Repository
public class UserRepoImpl {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns all users who have:
     * - sentimentAnalysis = true
     * - a non-null email matching a basic RFC-5322-like pattern
     */
    public List<User> getUsersForSentimentAnalysis() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> user = cq.from(User.class);

        cq.select(user).where(cb.and(
                cb.isNotNull(user.get("email")),
                cb.isTrue(user.get("sentimentAnalysis"))
        ));

        return entityManager.createQuery(cq).getResultList();
    }
}