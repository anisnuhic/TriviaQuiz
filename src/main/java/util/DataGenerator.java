package util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

public class DataGenerator {

    public static void main(String[] args) {
        System.out.println("Pokretanje generiranja test podataka...");

        EntityManagerFactory emf = null;
        EntityManager em = null;

        try {
            emf = Persistence.createEntityManagerFactory("myPU");
            em = emf.createEntityManager();

            em.getTransaction().begin();

            User user = createUser(em);

            createJavaServletQuiz(em, user);
            createJavaScriptQuiz(em, user);

            em.getTransaction().commit();

            System.out.println("Test podaci uspješno generirani!");
            System.out.println("Kreiran korisnik: arsen (šifra: Arsen123)");
            System.out.println("Kreirani kvizovi: Java Servleti i JavaScript");

        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Greška prilikom generiranja podataka: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }
        }
    }

    private static User createUser(EntityManager em) {

        User existingUser = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", "arsen")
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existingUser != null) {
            System.out.println("Korisnik 'arsen' već postoji, koristi se postojeći");
            return existingUser;
        }

        User user = new User();
        user.setUsername("arsen");
        user.setEmail("arsen@example.com");
        user.setPasswordHash(BCrypt.hashpw("Arsen123", BCrypt.gensalt()));
        user.setFirstName("Arsen");
        user.setLastName("Stevanović");
        user.setIsVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        em.persist(user);
        em.flush();

        System.out.println("Kreiran korisnik: " + user.getUsername());
        return user;
    }

    private static void createJavaServletQuiz(EntityManager em, User creator) {
        Quiz quiz = new Quiz();
        quiz.setTitle("Java Servleti - Osnove");
        quiz.setDescription("Test znanja osnovnih pojmova Java servleta");
        quiz.setCategory("Java");
        quiz.setCreator(creator);
        quiz.setIsActive(true);

        em.persist(quiz);
        em.flush();

        Question q1 = new Question();
        q1.setQuestionText("Java Servlets je prva web tehnologija za Java platformu.");
        q1.setQuiz(quiz);
        q1.setQuestionOrder(1);
        q1.setQuestionType(QuestionType.TRUE_FALSE);
        q1.setTimeLimit(20);
        q1.setPoints(5);
        em.persist(q1);
        em.flush();

        Answer q1a1 = new Answer("Tačno", q1, 1, true);
        Answer q1a2 = new Answer("Netačno", q1, 2, false);
        em.persist(q1a1);
        em.persist(q1a2);

        Question q2 = new Question();
        q2.setQuestionText("Prvi metod u životnom ciklusu Java Servleta je:");
        q2.setQuiz(quiz);
        q2.setQuestionOrder(2);
        q2.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q2.setTimeLimit(25);
        q2.setPoints(10);
        em.persist(q2);
        em.flush();

        Answer q2a1 = new Answer("init", q2, 1, true);
        Answer q2a2 = new Answer("destroy", q2, 2, false);
        Answer q2a3 = new Answer("service", q2, 3, false);
        em.persist(q2a1);
        em.persist(q2a2);
        em.persist(q2a3);

        Question q3 = new Question();
        q3.setQuestionText("Metod service() poziva odgovarajuće metode u zavisnosti od vrste HTTP zahtijeva.");
        q3.setQuiz(quiz);
        q3.setQuestionOrder(3);
        q3.setQuestionType(QuestionType.TRUE_FALSE);
        q3.setTimeLimit(20);
        q3.setPoints(5);
        em.persist(q3);
        em.flush();

        Answer q3a1 = new Answer("Tačno", q3, 1, true);
        Answer q3a2 = new Answer("Netačno", q3, 2, false);
        em.persist(q3a1);
        em.persist(q3a2);

        Question q4 = new Question();
        q4.setQuestionText("Koji HTTP zahtjev se šalje za update nekog resursa?");
        q4.setQuiz(quiz);
        q4.setQuestionOrder(4);
        q4.setQuestionType(QuestionType.TEXT);
        q4.setTimeLimit(30);
        q4.setPoints(10);
        em.persist(q4);
        em.flush();

        Answer q4a1 = new Answer("POST", q4, 1, true);
        em.persist(q4a1);

        Question q5 = new Question();
        q5.setQuestionText("Ekstenzija deployment deskriptora je:");
        q5.setQuiz(quiz);
        q5.setQuestionOrder(5);
        q5.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q5.setTimeLimit(25);
        q5.setPoints(10);
        em.persist(q5);
        em.flush();

        Answer q5a1 = new Answer("xml", q5, 1, true);
        Answer q5a2 = new Answer("qml", q5, 2, false);
        Answer q5a3 = new Answer("uml", q5, 3, false);
        em.persist(q5a1);
        em.persist(q5a2);
        em.persist(q5a3);

        Question q6 = new Question();
        q6.setQuestionText("HTTP zahtjev kojeg servlet kontejner dobije se čuva u objektu klase:");
        q6.setQuiz(quiz);
        q6.setQuestionOrder(6);
        q6.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q6.setTimeLimit(30);
        q6.setPoints(10);
        em.persist(q6);
        em.flush();

        Answer q6a1 = new Answer("HTTPServletRequest", q6, 1, true);
        Answer q6a2 = new Answer("ServletRequest", q6, 2, false);
        Answer q6a3 = new Answer("Request", q6, 3, false);
        em.persist(q6a1);
        em.persist(q6a2);
        em.persist(q6a3);

        Question q7 = new Question();
        q7.setQuestionText("Zaglavlje zahtjeva možemo dobiti koristeći koji metod?");
        q7.setQuiz(quiz);
        q7.setQuestionOrder(7);
        q7.setQuestionType(QuestionType.TEXT);
        q7.setTimeLimit(30);
        q7.setPoints(10);
        em.persist(q7);
        em.flush();

        Answer q7a1 = new Answer("getHeader", q7, 1, true);
        em.persist(q7a1);

        Question q8 = new Question();
        q8.setQuestionText("Redirekciju zahtjeva je moguće izvršiti koristeći sendRedirect metod.");
        q8.setQuiz(quiz);
        q8.setQuestionOrder(8);
        q8.setQuestionType(QuestionType.TRUE_FALSE);
        q8.setTimeLimit(20);
        q8.setPoints(5);
        em.persist(q8);
        em.flush();

        Answer q8a1 = new Answer("Tačno", q8, 1, true);
        Answer q8a2 = new Answer("Netačno", q8, 2, false);
        em.persist(q8a1);
        em.persist(q8a2);

        Question q9 = new Question();
        q9.setQuestionText(
                "Cookies su komadići podataka koji se smještaju u web pregledniku a generirani su od strane web aplikacije.");
        q9.setQuiz(quiz);
        q9.setQuestionOrder(9);
        q9.setQuestionType(QuestionType.TRUE_FALSE);
        q9.setTimeLimit(25);
        q9.setPoints(5);
        em.persist(q9);
        em.flush();

        Answer q9a1 = new Answer("Tačno", q9, 1, true);
        Answer q9a2 = new Answer("Netačno", q9, 2, false);
        em.persist(q9a1);
        em.persist(q9a2);

        Question q10 = new Question();
        q10.setQuestionText("Istek sesije se može podesiti u deployment deskriptoru.");
        q10.setQuiz(quiz);
        q10.setQuestionOrder(10);
        q10.setQuestionType(QuestionType.TRUE_FALSE);
        q10.setTimeLimit(20);
        q10.setPoints(5);
        em.persist(q10);
        em.flush();

        Answer q10a1 = new Answer("Tačno", q10, 1, true);
        Answer q10a2 = new Answer("Netačno", q10, 2, false);
        em.persist(q10a1);
        em.persist(q10a2);

        System.out.println("Kreiran kviz: " + quiz.getTitle());
    }

    private static void createJavaScriptQuiz(EntityManager em, User creator) {
        Quiz quiz = new Quiz();
        quiz.setTitle("JavaScript - Osnove");
        quiz.setDescription("Test osnovnog znanja JavaScript programskog jezika");
        quiz.setCategory("JavaScript");
        quiz.setCreator(creator);
        quiz.setIsActive(true);

        em.persist(quiz);
        em.flush();

        Question q1 = new Question();
        q1.setQuestionText("JavaScript je interpretirani programski jezik u skladu sa ECMAScript standardom.");
        q1.setQuiz(quiz);
        q1.setQuestionOrder(1);
        q1.setQuestionType(QuestionType.TRUE_FALSE);
        q1.setTimeLimit(20);
        q1.setPoints(5);
        em.persist(q1);
        em.flush();

        Answer q1a1 = new Answer("Tačno", q1, 1, true);
        Answer q1a2 = new Answer("Netačno", q1, 2, false);
        em.persist(q1a1);
        em.persist(q1a2);

        Question q2 = new Question();
        q2.setQuestionText("JavaScript koristi:");
        q2.setQuiz(quiz);
        q2.setQuestionOrder(2);
        q2.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q2.setTimeLimit(25);
        q2.setPoints(10);
        em.persist(q2);
        em.flush();

        Answer q2a1 = new Answer("statičko tipiziranje", q2, 1, false);
        Answer q2a2 = new Answer("dinamičko tipiziranje", q2, 2, true);
        em.persist(q2a1);
        em.persist(q2a2);

        Question q3 = new Question();
        q3.setQuestionText("Za provjeru tipa vrijednosti koristi se:");
        q3.setQuiz(quiz);
        q3.setQuestionOrder(3);
        q3.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q3.setTimeLimit(20);
        q3.setPoints(10);
        em.persist(q3);
        em.flush();

        Answer q3a1 = new Answer("type", q3, 1, false);
        Answer q3a2 = new Answer("typeof", q3, 2, true);
        em.persist(q3a1);
        em.persist(q3a2);

        Question q4 = new Question();
        q4.setQuestionText("Koji član se koristi za dobijanje dužine stringa?");
        q4.setQuiz(quiz);
        q4.setQuestionOrder(4);
        q4.setQuestionType(QuestionType.TEXT);
        q4.setTimeLimit(25);
        q4.setPoints(10);
        em.persist(q4);
        em.flush();

        Answer q4a1 = new Answer("length", q4, 1, true);
        em.persist(q4a1);

        Question q5 = new Question();
        q5.setQuestionText("Konverzija vrijednosti nekog tipa u bool vrijednost se vrši sa !!(val).");
        q5.setQuiz(quiz);
        q5.setQuestionOrder(5);
        q5.setQuestionType(QuestionType.TRUE_FALSE);
        q5.setTimeLimit(25);
        q5.setPoints(5);
        em.persist(q5);
        em.flush();

        Answer q5a1 = new Answer("Tačno", q5, 1, true);
        Answer q5a2 = new Answer("Netačno", q5, 2, false);
        em.persist(q5a1);
        em.persist(q5a2);

        Question q6 = new Question();
        q6.setQuestionText("Izraz 5 + '6' + 7 daje string '567'.");
        q6.setQuiz(quiz);
        q6.setQuestionOrder(6);
        q6.setQuestionType(QuestionType.TRUE_FALSE);
        q6.setTimeLimit(30);
        q6.setPoints(5);
        em.persist(q6);
        em.flush();

        Answer q6a1 = new Answer("Tačno", q6, 1, true);
        Answer q6a2 = new Answer("Netačno", q6, 2, false);
        em.persist(q6a1);
        em.persist(q6a2);

        Question q7 = new Question();
        q7.setQuestionText("Za pristup članovima objekta može se koristiti operator . i operator [].");
        q7.setQuiz(quiz);
        q7.setQuestionOrder(7);
        q7.setQuestionType(QuestionType.TRUE_FALSE);
        q7.setTimeLimit(25);
        q7.setPoints(5);
        em.persist(q7);
        em.flush();

        Answer q7a1 = new Answer("Tačno", q7, 1, true);
        Answer q7a2 = new Answer("Netačno", q7, 2, false);
        em.persist(q7a1);
        em.persist(q7a2);

        Question q8 = new Question();
        q8.setQuestionText("Kodom console.log('2'+3+32) će se u konzoli ispisati:");
        q8.setQuiz(quiz);
        q8.setQuestionOrder(8);
        q8.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q8.setTimeLimit(30);
        q8.setPoints(10);
        em.persist(q8);
        em.flush();

        Answer q8a1 = new Answer("235", q8, 1, false);
        Answer q8a2 = new Answer("2332", q8, 2, true);
        Answer q8a3 = new Answer("37", q8, 3, false);
        Answer q8a4 = new Answer("532", q8, 4, false);
        em.persist(q8a1);
        em.persist(q8a2);
        em.persist(q8a3);
        em.persist(q8a4);

        Question q9 = new Question();
        q9.setQuestionText("ECMAScript 2015 uvodi block-scope deklaraciju sa ključnom riječi let.");
        q9.setQuiz(quiz);
        q9.setQuestionOrder(9);
        q9.setQuestionType(QuestionType.TRUE_FALSE);
        q9.setTimeLimit(25);
        q9.setPoints(5);
        em.persist(q9);
        em.flush();

        Answer q9a1 = new Answer("Tačno", q9, 1, true);
        Answer q9a2 = new Answer("Netačno", q9, 2, false);
        em.persist(q9a1);
        em.persist(q9a2);

        Question q10 = new Question();
        q10.setQuestionText("Ako u for petlji želimo iterirati kroz vrijednosti članova objekta, koristit ćemo:");
        q10.setQuiz(quiz);
        q10.setQuestionOrder(10);
        q10.setQuestionType(QuestionType.TEXT);
        q10.setTimeLimit(30);
        q10.setPoints(10);
        em.persist(q10);
        em.flush();

        Answer q10a1 = new Answer("of", q10, 1, true);
        em.persist(q10a1);

        System.out.println("Kreiran kviz: " + quiz.getTitle());
    }
}