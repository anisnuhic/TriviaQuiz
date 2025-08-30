package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Question;
import model.Quiz;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class QuestionRepository {
    
    public Question save(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (question.getId() == null) {
                em.persist(question);
            } else {
                question = em.merge(question);
            }
            em.getTransaction().commit();
            return question;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<Question> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Question question = em.find(Question.class, id);
            return Optional.ofNullable(question);
        } finally {
            em.close();
        }
    }
    
    public Optional<Question> findByIdWithAnswers(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Question> query = em.createQuery(
                "SELECT q FROM Question q LEFT JOIN FETCH q.answers WHERE q.id = :id", 
                Question.class);
            query.setParameter("id", id);
            List<Question> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
    public List<Question> findByQuiz(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Question> query = em.createQuery(
                "SELECT q FROM Question q WHERE q.quiz = :quiz ORDER BY q.questionOrder", 
                Question.class);
            query.setParameter("quiz", quiz);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Question> findByQuizWithAnswers(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Question> query = em.createQuery(
                "SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answers " +
                "WHERE q.quiz = :quiz ORDER BY q.questionOrder", 
                Question.class);
            query.setParameter("quiz", quiz);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public void delete(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(question) ? question : em.merge(question));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteByQuiz(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Question q WHERE q.quiz = :quiz")
                .setParameter("quiz", quiz)
                .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Long countByQuiz(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(q) FROM Question q WHERE q.quiz = :quiz", Long.class);
            query.setParameter("quiz", quiz);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public Integer getMaxQuestionOrder(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Integer> query = em.createQuery(
                "SELECT MAX(q.questionOrder) FROM Question q WHERE q.quiz = :quiz", 
                Integer.class);
            query.setParameter("quiz", quiz);
            Integer maxOrder = query.getSingleResult();
            return maxOrder != null ? maxOrder : 0;
        } finally {
            em.close();
        }
    }
}