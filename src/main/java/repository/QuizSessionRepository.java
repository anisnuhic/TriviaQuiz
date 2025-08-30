package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.Quiz;
import model.QuizSession;
import model.SessionStatus;
import model.User;
import util.JPAUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class QuizSessionRepository {
    
    public QuizSession save(QuizSession session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (session.getId() == null) {
                em.persist(session);
            } else {
                session = em.merge(session);
            }
            em.getTransaction().commit();
            return session;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<QuizSession> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            QuizSession session = em.find(QuizSession.class, id);
            return Optional.ofNullable(session);
        } finally {
            em.close();
        }
    }
    
    public Optional<QuizSession> findBySessionPin(String sessionPin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizSession> query = em.createQuery(
                "SELECT s FROM QuizSession s WHERE s.sessionPin = :pin", QuizSession.class);
            query.setParameter("pin", sessionPin);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public Optional<QuizSession> findActiveSessionByPin(String sessionPin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizSession> query = em.createQuery(
                "SELECT s FROM QuizSession s WHERE s.sessionPin = :pin " +
                "AND s.status IN (:waiting, :active)", QuizSession.class);
            query.setParameter("pin", sessionPin);
            query.setParameter("waiting", SessionStatus.WAITING);
            query.setParameter("active", SessionStatus.ACTIVE);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public List<QuizSession> findByAdmin(User admin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizSession> query = em.createQuery(
                "SELECT s FROM QuizSession s WHERE s.admin = :admin ORDER BY s.createdAt DESC", 
                QuizSession.class);
            query.setParameter("admin", admin);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<QuizSession> findByQuiz(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizSession> query = em.createQuery(
                "SELECT s FROM QuizSession s WHERE s.quiz = :quiz ORDER BY s.createdAt DESC", 
                QuizSession.class);
            query.setParameter("quiz", quiz);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<QuizSession> findActiveSessionsByAdmin(User admin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<QuizSession> query = em.createQuery(
                "SELECT s FROM QuizSession s WHERE s.admin = :admin " +
                "AND s.status IN (:waiting, :active)", 
                QuizSession.class);
            query.setParameter("admin", admin);
            query.setParameter("waiting", SessionStatus.WAITING);
            query.setParameter("active", SessionStatus.ACTIVE);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public void delete(QuizSession session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(session) ? session : em.merge(session));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public boolean existsBySessionPin(String sessionPin) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(s) FROM QuizSession s WHERE s.sessionPin = :pin", Long.class);
            query.setParameter("pin", sessionPin);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public List<QuizSession> findByQuizId(Long quizId) {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        TypedQuery<QuizSession> query = em.createQuery(
            "SELECT qs FROM QuizSession qs WHERE qs.quiz.id = :quizId ORDER BY qs.createdAt DESC", 
            QuizSession.class);
        query.setParameter("quizId", quizId);
        return query.getResultList();
    } finally {
        em.close();
    }
}
    
    public void closeExpiredSessions(int hoursToExpire) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            LocalDateTime expirationTime = LocalDateTime.now().minusHours(hoursToExpire);
            em.createQuery(
                "UPDATE QuizSession s SET s.status = :finished " +
                "WHERE s.status IN (:waiting, :active) AND s.createdAt < :expirationTime")
                .setParameter("finished", SessionStatus.FINISHED)
                .setParameter("waiting", SessionStatus.WAITING)
                .setParameter("active", SessionStatus.ACTIVE)
                .setParameter("expirationTime", expirationTime)
                .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Long countActiveSessionsByQuiz(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(s) FROM QuizSession s WHERE s.quiz = :quiz " +
                "AND s.status IN (:waiting, :active)", Long.class);
            query.setParameter("quiz", quiz);
            query.setParameter("waiting", SessionStatus.WAITING);
            query.setParameter("active", SessionStatus.ACTIVE);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
}