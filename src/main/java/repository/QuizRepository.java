package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Quiz;
import model.User;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class QuizRepository {
    
    public Quiz save(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (quiz.getId() == null) {
                em.persist(quiz);
            } else {
                quiz = em.merge(quiz);
            }
            em.getTransaction().commit();
            return quiz;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<Quiz> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Quiz quiz = em.find(Quiz.class, id);
            return Optional.ofNullable(quiz);
        } finally {
            em.close();
        }
    }
    
    public Optional<Quiz> findByIdWithQuestions(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Quiz> query = em.createQuery(
                "SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id", 
                Quiz.class);
            query.setParameter("id", id);
            List<Quiz> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
   public List<Quiz> findAllWithCreators() {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        TypedQuery<Quiz> query = em.createQuery(
            "SELECT DISTINCT q FROM Quiz q JOIN FETCH q.creator ORDER BY q.createdAt DESC", 
            Quiz.class);
        return query.getResultList();
    } finally {
        em.close();
    }
}

public List<Quiz> findAll() {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        TypedQuery<Quiz> query = em.createQuery(
            "SELECT DISTINCT q FROM Quiz q JOIN FETCH q.creator ORDER BY q.createdAt DESC", 
            Quiz.class);
        return query.getResultList();
    } finally {
        em.close();
    }
}
    
    public List<Quiz> findByCreator(User creator) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Quiz> query = em.createQuery(
                "SELECT q FROM Quiz q WHERE q.creator = :creator ORDER BY q.createdAt DESC", 
                Quiz.class);
            query.setParameter("creator", creator);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    
    
    public List<Quiz> findActiveQuizzes() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Quiz> query = em.createQuery(
                "SELECT q FROM Quiz q WHERE q.isActive = true ORDER BY q.createdAt DESC", 
                Quiz.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Quiz> findByCategory(String category) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Quiz> query = em.createQuery(
                "SELECT q FROM Quiz q WHERE q.category = :category AND q.isActive = true", 
                Quiz.class);
            query.setParameter("category", category);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Quiz> searchByTitle(String searchTerm) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Quiz> query = em.createQuery(
                "SELECT q FROM Quiz q WHERE LOWER(q.title) LIKE LOWER(:searchTerm) " +
                "AND q.isActive = true ORDER BY q.createdAt DESC", 
                Quiz.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public void delete(Quiz quiz) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(quiz) ? quiz : em.merge(quiz));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Long countByCreator(User creator) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(q) FROM Quiz q WHERE q.creator = :creator", Long.class);
            query.setParameter("creator", creator);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public List<String> findAllCategories() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<String> query = em.createQuery(
                "SELECT DISTINCT q.category FROM Quiz q WHERE q.category IS NOT NULL", 
                String.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}