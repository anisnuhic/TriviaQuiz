package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.QuizParticipant;
import model.QuizSession;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class QuizParticipantRepository {
    
    public QuizParticipant save(QuizParticipant participant) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (participant.getId() == null) {
                em.persist(participant);
            } else {
                participant = em.merge(participant);
            }
            em.getTransaction().commit();
            return participant;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<QuizParticipant> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            QuizParticipant participant = em.find(QuizParticipant.class, id);
            return Optional.ofNullable(participant);
        } finally {
            em.close();
        }
    }
    
    public List<QuizParticipant> findBySession(QuizSession session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizParticipant> query = em.createQuery(
                "SELECT p FROM QuizParticipant p WHERE p.session = :session " +
                "ORDER BY p.totalScore DESC", 
                QuizParticipant.class);
            query.setParameter("session", session);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Optional<QuizParticipant> findBySessionAndName(QuizSession session, String participantName) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizParticipant> query = em.createQuery(
                "SELECT p FROM QuizParticipant p WHERE p.session = :session " +
                "AND p.participantName = :name", 
                QuizParticipant.class);
            query.setParameter("session", session);
            query.setParameter("name", participantName);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public List<QuizParticipant> findTopParticipantsBySession(QuizSession session, int limit) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizParticipant> query = em.createQuery(
                "SELECT p FROM QuizParticipant p WHERE p.session = :session " +
                "ORDER BY p.totalScore DESC", 
                QuizParticipant.class);
            query.setParameter("session", session);
            query.setMaxResults(limit);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public void delete(QuizParticipant participant) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(participant) ? participant : em.merge(participant));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteBySession(QuizSession session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM QuizParticipant p WHERE p.session = :session")
                .setParameter("session", session)
                .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Long countBySession(QuizSession session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(p) FROM QuizParticipant p WHERE p.session = :session", 
                Long.class);
            query.setParameter("session", session);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public boolean existsBySessionAndName(QuizSession session, String participantName) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(p) FROM QuizParticipant p WHERE p.session = :session " +
                "AND p.participantName = :name", 
                Long.class);
            query.setParameter("session", session);
            query.setParameter("name", participantName);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    public List<QuizParticipant> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizParticipant> query = em.createQuery(
                "SELECT p FROM QuizParticipant p ORDER BY p.joinedAt DESC", 
                QuizParticipant.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}