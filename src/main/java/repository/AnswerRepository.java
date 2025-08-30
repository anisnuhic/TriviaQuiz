package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Answer;
import model.Question;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class AnswerRepository {
    
    public Answer save(Answer answer) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (answer.getId() == null) {
                em.persist(answer);
            } else {
                answer = em.merge(answer);
            }
            em.getTransaction().commit();
            return answer;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<Answer> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Answer answer = em.find(Answer.class, id);
            return Optional.ofNullable(answer);
        } finally {
            em.close();
        }
    }
    
    public List<Answer> findByQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Answer> query = em.createQuery(
                "SELECT a FROM Answer a WHERE a.question = :question ORDER BY a.answerOrder", 
                Answer.class);
            query.setParameter("question", question);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Optional<Answer> findCorrectAnswerForQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Answer> query = em.createQuery(
                "SELECT a FROM Answer a WHERE a.question = :question AND a.isCorrect = true", 
                Answer.class);
            query.setParameter("question", question);
            List<Answer> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
    public void delete(Answer answer) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(answer) ? answer : em.merge(answer));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteByQuestion(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Answer a WHERE a.question = :question")
                .setParameter("question", question)
                .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public List<Answer> saveAll(List<Answer> answers) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            for (Answer answer : answers) {
                if (answer.getId() == null) {
                    em.persist(answer);
                } else {
                    em.merge(answer);
                }
            }
            em.getTransaction().commit();
            return answers;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Integer getMaxAnswerOrder(Question question) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Integer> query = em.createQuery(
                "SELECT MAX(a.answerOrder) FROM Answer a WHERE a.question = :question", 
                Integer.class);
            query.setParameter("question", question);
            Integer maxOrder = query.getSingleResult();
            return maxOrder != null ? maxOrder : 0;
        } finally {
            em.close();
        }
    }
}