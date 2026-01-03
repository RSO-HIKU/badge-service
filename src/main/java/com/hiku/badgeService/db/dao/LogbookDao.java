package com.hiku.badgeService.db.dao;

import com.hiku.badgeService.db.models.LogbookEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@ApplicationScoped
public class LogbookDao {

    @Inject
    private EntityManager em;

    public List<LogbookEntry> findByUser(Integer userId) {
        TypedQuery<LogbookEntry> q = em.createQuery(
                "SELECT l FROM LogbookEntry l WHERE l.userId = :userId ORDER BY l.addedAt DESC",
                LogbookEntry.class);
        q.setParameter("userId", userId);
        return q.getResultList();
    }

    public LogbookEntry insert(LogbookEntry entry) {
        // Ensure RESOURCE_LOCAL persistence commits when no container transaction is present
        var tx = em.getTransaction();
        boolean started = false;
        if (!tx.isActive()) {
            tx.begin();
            started = true;
        }

        em.persist(entry);
        em.flush();

        if (started) {
            tx.commit();
        }
        return entry;
    }
}
