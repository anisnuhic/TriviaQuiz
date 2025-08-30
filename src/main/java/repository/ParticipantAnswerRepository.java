package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.ParticipantAnswer;
import model.Question;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class ParticipantAnswerRepository {
    
    public ParticipantAnswer save(ParticipantAnswer participantAnswer) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (participantAnswer.getId() == null) {
                em.persist(participantAnswer);
            } else {
                participantAnswer = em.merge(participantAnswer);
            }
            em.getTransaction().commit();
            return participantAnswer;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<ParticipantAnswer> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            ParticipantAnswer answer = em.find(ParticipantAnswer.class, id);
            return Optional.ofNullable(answer);
        } finally {
            em.close();
        }
    }
    
    public Optional<ParticipantAnswer> findByParticipantAndQuestion(String participantId, Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ParticipantAnswer> query = em.createQuery(
                "SELECT pa FROM ParticipantAnswer pa WHERE pa.participantId = :participantId " +
                "AND pa.question = :question", 
                ParticipantAnswer.class);
            query.setParameter("participantId", participantId);
            query.setParameter("question", question);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public List<ParticipantAnswer> findByParticipant(String participantId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ParticipantAnswer> query = em.createQuery(
                "SELECT pa FROM ParticipantAnswer pa WHERE pa.participantId = :participantId " +
                "ORDER BY pa.answeredAt", 
                ParticipantAnswer.class);
            query.setParameter("participantId", participantId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<ParticipantAnswer> findByQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ParticipantAnswer> query = em.createQuery(
                "SELECT pa FROM ParticipantAnswer pa WHERE pa.question = :question", 
                ParticipantAnswer.class);
            query.setParameter("question", question);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Long countCorrectAnswersByParticipant(String participantId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ParticipantAnswer pa " +
                "WHERE pa.participantId = :participantId AND pa.answer.isCorrect = true", 
                Long.class);
            query.setParameter("participantId", participantId);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public Long countAnswersByQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ParticipantAnswer pa WHERE pa.question = :question", 
                Long.class);
            query.setParameter("question", question);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public Long countCorrectAnswersByQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ParticipantAnswer pa " +
                "WHERE pa.question = :question AND pa.answer.isCorrect = true", 
                Long.class);
            query.setParameter("question", question);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public void deleteBySessionPin(String sessionPin) {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        em.getTransaction().begin();
        em.createQuery(
            "DELETE FROM ParticipantAnswer pa WHERE pa.participantId IN " +
            "(SELECT p.participantId FROM QuizParticipant p WHERE p.session.sessionPin = :sessionPin)")
            .setParameter("sessionPin", sessionPin)
            .executeUpdate();
        em.getTransaction().commit();
        System.out.println("Obrisani svi odgovori za sesiju: " + sessionPin);
    } catch (Exception e) {
        em.getTransaction().rollback();
        System.out.println("Greška pri brisanju odgovora za sesiju " + sessionPin + ": " + e.getMessage());
        throw e;
    } finally {
        em.close();
    }
}
    
    public void delete(ParticipantAnswer participantAnswer) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(participantAnswer) ? participantAnswer : em.merge(participantAnswer));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteByParticipant(String participantId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM ParticipantAnswer pa WHERE pa.participantId = :participantId")
                .setParameter("participantId", participantId)
                .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public boolean hasParticipantAnsweredQuestion(String participantId, Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ParticipantAnswer pa " +
                "WHERE pa.participantId = :participantId AND pa.question = :question", 
                Long.class);
            query.setParameter("participantId", participantId);
            query.setParameter("question", question);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
}