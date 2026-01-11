package com.hiku.badgeService.util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ApplicationScoped
public class EntityManagerProducer {

    private static EntityManagerFactory emf;

    static {
        emf = Persistence.createEntityManagerFactory("hikuPU");
    }

    @Produces
    @RequestScoped
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public void close(@Disposes EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }
}
