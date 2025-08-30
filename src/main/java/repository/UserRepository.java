package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.User;
import util.JPAUtil;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    
    public User save(User user) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (user.getId() == null) {
                em.persist(user);
            } else {
                user = em.merge(user);
            }
            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Optional<User> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, id);
            return Optional.ofNullable(user);
        } finally {
            em.close();
        }
    }
    
    public Optional<User> findByUsername(String username) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public Optional<User> findByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public Optional<User> findByVerificationToken(String token) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.verificationToken = :token", User.class);
            query.setParameter("token", token);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public List<User> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public void delete(User user) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(user) ? user : em.merge(user));
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public boolean existsByUsername(String username) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
    
    public boolean existsByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
            query.setParameter("email", email);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
}