package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Answer;
import model.Question;
import model.QuestionType;
import model.Quiz;
import model.User;
import service.AnswerService;
import service.QuestionService;
import service.QuizService;
import service.UserService;

@WebServlet("/admin/api/quiz/*")
public class QuizServlet extends HttpServlet {
    private QuizService quizService;
    private UserService userService;
    private QuestionService questionService;
    private AnswerService answerService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        quizService = new QuizService();
        userService = new UserService();
        questionService = new QuestionService();
        answerService = new AnswerService();
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        if (f.getDeclaredType() == LocalDateTime.class) {
                            return true;
                        }

                        if (f.getName().equals("questions") &&
                                f.getDeclaringClass().getName().equals("model.Quiz")) {
                            return true;
                        }

                        if (f.getName().equals("hibernateLazyInitializer") ||
                                f.getName().equals("handler") ||
                                f.getName().equals("quizSessions") ||
                                f.getName().equals("creator")) {
                            return true;
                        }

                        return false;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return clazz.getName().contains("hibernate") ||
                                clazz.getName().contains("proxy") ||
                                clazz.getName().contains("HibernateProxy");
                    }
                })
                .setPrettyPrinting()
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Quiz> quizzes = quizService.findActiveQuizzes();
                resp.getWriter().write(gson.toJson(quizzes));

            } else if (pathInfo.equals("/superadmin")) {

                HttpSession session = req.getSession(false);
                if (session == null) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("{\"error\": \"Not authenticated nije superadmin\"}");
                    return;
                }

                String sessionUser = (String) session.getAttribute("user");
                if (!sessionUser.equals("superadmin")) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                    return;
                }

                List<Quiz> allQuizzes = quizService.findAllQuizzes();

                List<SuperAdminQuizResponse> superAdminResponses = allQuizzes.stream()
                        .map(quiz -> {
                            List<Question> questions = questionService.findQuestionsByQuiz(quiz);
                            return new SuperAdminQuizResponse(quiz, questions.size());
                        })
                        .collect(Collectors.toList());

                Gson superAdminGson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                        .setPrettyPrinting()
                        .create();

                resp.getWriter().write(superAdminGson.toJson(superAdminResponses));
            }

            else if (pathInfo.matches("/\\d+")) {
                String userIdStr = pathInfo.substring(1);
                Long userId = Long.parseLong(userIdStr);

                HttpSession session = req.getSession(false);
                Long sessionUserId = (Long) session.getAttribute("userId");

                if (!userIdStr.equals(sessionUserId.toString())) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                    return;
                }

                Optional<User> userOpt = userService.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    List<Quiz> quizzes = quizService.findQuizzesByCreator(user);

                    List<Quiz> unproxiedQuizzes = quizzes.stream()
                            .map(quiz -> (Quiz) Hibernate.unproxy(quiz))
                            .collect(Collectors.toList());

                    resp.getWriter().write(gson.toJson(unproxiedQuizzes));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"User not found\"}");
                }

            } else if (pathInfo.matches("/\\d+/\\d+")) {
                String[] pathParts = pathInfo.substring(1).split("/");
                Long userId = Long.parseLong(pathParts[0]);
                Long quizId = Long.parseLong(pathParts[1]);

                HttpSession session = req.getSession(false);
                Long sessionUserId = (Long) session.getAttribute("userId");

                if (!userId.equals(sessionUserId)) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                    return;
                }

                Optional<Quiz> quizOpt = quizService.findQuizById(quizId);
                if (!quizOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Quiz not found\"}");
                    return;
                }

                Quiz quiz = quizOpt.get();

                if (!quiz.getCreator().getId().equals(userId)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"error\": \"Access denied - not quiz creator\"}");
                    return;
                }

                List<Question> questions = questionService.findQuestionsByQuiz(quiz);

                List<QuestionResponse> questionResponses = questions.stream()
                        .map(question -> {
                            List<Answer> answers = answerService.findAnswersByQuestion(question);
                            return new QuestionResponse(question, answers);
                        })
                        .collect(Collectors.toList());

                QuizWithQuestionsResponse response = new QuizWithQuestionsResponse(quiz, questionResponses);
                Gson simpleGson = new GsonBuilder().setPrettyPrinting().create();
                resp.getWriter().write(simpleGson.toJson(response));

            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Invalid path\"}");
            }

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid ID format\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        try {
            String[] pathParts = pathInfo.substring(1).split("/");
            Long userId = Long.parseLong(pathParts[0]);

            HttpSession session = req.getSession(false);
            Long sessionUserId = (Long) session.getAttribute("userId");

            if (!userId.equals(sessionUserId)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                return;
            }

            Optional<User> userOpt = userService.findById(userId);
            if (!userOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"User not found\"}");
                return;
            }

            User user = userOpt.get();

            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuffer.append(line);
                }
            }

            QuizRequest quizRequest = gson.fromJson(jsonBuffer.toString(), QuizRequest.class);

            System.out.println("=== QUIZ CREATION DEBUG ===");
            System.out.println("Received JSON: " + jsonBuffer.toString());
            System.out.println("Quiz title: " + quizRequest.getTitle());
            System.out.println("Questions count: "
                    + (quizRequest.getQuestions() != null ? quizRequest.getQuestions().size() : "null"));

            if (quizRequest.getTitle() == null || quizRequest.getTitle().trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Quiz title is required\"}");
                return;
            }

            Quiz quiz = quizService.createQuiz(
                    quizRequest.getTitle(),
                    quizRequest.getDescription(),
                    quizRequest.getCategory(),
                    quizRequest.getQuizImage(),
                    user);

            if (quizRequest.getQuestions() != null && !quizRequest.getQuestions().isEmpty()) {
                System.out.println("Processing " + quizRequest.getQuestions().size() + " questions");

                for (int i = 0; i < quizRequest.getQuestions().size(); i++) {
                    QuestionRequest questionRequest = quizRequest.getQuestions().get(i);
                    System.out.println("Processing question " + (i + 1) + ": " + questionRequest.getText());
                    System.out.println("Question type: " + questionRequest.getQuestionType());
                    System.out.println("Answers count: "
                            + (questionRequest.getAnswers() != null ? questionRequest.getAnswers().size() : "null"));

                    try {
                        QuestionType questionType = QuestionType.valueOf(questionRequest.getQuestionType());

                        Question question = quizService.addQuestion(
                                quiz.getId(),
                                questionRequest.getText(),
                                null,
                                questionRequest.getTimeLimit(),
                                questionRequest.getPoints(),
                                questionType);

                        System.out.println("Question created with ID: " + question.getId());

                        if (questionRequest.getAnswers() != null && !questionRequest.getAnswers().isEmpty()) {
                            System.out.println("Processing " + questionRequest.getAnswers().size() + " answers");

                            for (int j = 0; j < questionRequest.getAnswers().size(); j++) {
                                AnswerRequest answerRequest = questionRequest.getAnswers().get(j);
                                System.out.println("Processing answer " + (j + 1) + ": " + answerRequest.getText()
                                        + " (correct: " + answerRequest.isCorrect() + ")");

                                Answer answer = quizService.addAnswer(
                                        question.getId(),
                                        answerRequest.getText(),
                                        answerRequest.isCorrect());

                                System.out.println("Answer created with ID: " + answer.getId());
                            }
                        } else {
                            System.out.println("No answers to process for question " + (i + 1));
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR processing question " + (i + 1) + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("No questions to process");
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("{\"message\": \"Quiz created successfully\", \"quizId\": " + quiz.getId() + "}");

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid ID format\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        try {

            if (!pathInfo.matches("/\\d+/\\d+")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Invalid path format. Expected: /userId/quizId\"}");
                return;
            }

            String[] pathParts = pathInfo.substring(1).split("/");
            Long userId = Long.parseLong(pathParts[0]);
            Long quizId = Long.parseLong(pathParts[1]);

            HttpSession session = req.getSession(false);
            if (session == null || !userId.equals(session.getAttribute("userId"))) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                return;
            }

            Optional<Quiz> quizOpt = quizService.findQuizById(quizId);
            if (!quizOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Quiz not found\"}");
                return;
            }

            Quiz quiz = quizOpt.get();

            if (!quiz.getCreator().getId().equals(userId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("{\"error\": \"Access denied - not quiz creator\"}");
                return;
            }

            System.out.println("=== QUIZ DELETION DEBUG ===");
            System.out.println("Deleting quiz ID: " + quizId);
            System.out.println("Quiz title: " + quiz.getTitle());
            System.out.println("User ID: " + userId);

            quizService.deleteQuiz(quizId);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"message\": \"Quiz successfully deleted\", \"deletedQuizId\": " + quizId + "}");

            System.out.println("Quiz deletion completed successfully");

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid ID format\"}");

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Error deleting quiz: " + e.getMessage() + "\"}");
            e.printStackTrace();
            System.out.println("ERROR during quiz deletion: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        try {
            if (!pathInfo.matches("/\\d+/\\d+")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Invalid path format\"}");
                return;
            }

            String[] pathParts = pathInfo.substring(1).split("/");
            Long userId = Long.parseLong(pathParts[0]);
            Long quizId = Long.parseLong(pathParts[1]);

            HttpSession session = req.getSession(false);
            if (session == null || !userId.equals(session.getAttribute("userId"))) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Not authenticated\"}");
                return;
            }

            Optional<Quiz> quizOpt = quizService.findQuizById(quizId);
            if (!quizOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Quiz not found\"}");
                return;
            }

            Quiz quiz = quizOpt.get();
            if (!quiz.getCreator().getId().equals(userId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("{\"error\": \"Access denied\"}");
                return;
            }

            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuffer.append(line);
                }
            }

            QuizRequest quizRequest = gson.fromJson(jsonBuffer.toString(), QuizRequest.class);

            quiz = quizService.updateQuiz(
                    quizId,
                    quizRequest.getTitle(),
                    quizRequest.getDescription(),
                    quizRequest.getCategory(),
                    quizRequest.getQuizImage());

            List<Question> existingQuestions = questionService.findQuestionsByQuiz(quiz);
            for (Question question : existingQuestions) {
                answerService.deleteByQuestion(question);
                questionService.delete(question);
            }

            if (quizRequest.getQuestions() != null && !quizRequest.getQuestions().isEmpty()) {
                for (QuestionRequest questionRequest : quizRequest.getQuestions()) {
                    QuestionType questionType = QuestionType.valueOf(questionRequest.getQuestionType());

                    Question question = quizService.addQuestion(
                            quiz.getId(),
                            questionRequest.getText(),
                            null,
                            questionRequest.getTimeLimit(),
                            questionRequest.getPoints(),
                            questionType);

                    if (questionRequest.getAnswers() != null && !questionRequest.getAnswers().isEmpty()) {
                        for (AnswerRequest answerRequest : questionRequest.getAnswers()) {
                            quizService.addAnswer(
                                    question.getId(),
                                    answerRequest.getText(),
                                    answerRequest.isCorrect());
                        }
                    }
                }
            }

            if (quizRequest.isActive()) {
                quiz = quizService.activateQuiz(quiz.getId());
            } else {
                quiz = quizService.deactivateQuiz(quiz.getId());
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(quiz));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Error updating quiz: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }
}

class QuestionRequest {
    private Long id;
    private String text;
    private String questionType;
    private Integer questionOrder;
    private Integer timeLimit;
    private Integer points;
    private List<AnswerRequest> answers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<AnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerRequest> answers) {
        this.answers = answers;
    }
}

class AnswerRequest {
    private Long id;
    private String text;
    private boolean isCorrect;
    private Integer answerOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getAnswerOrder() {
        return answerOrder;
    }

    public void setAnswerOrder(Integer answerOrder) {
        this.answerOrder = answerOrder;
    }
}

class QuizRequest {
    private Long id;
    private String title;
    private String description;
    private String category;
    private boolean isActive;
    private String quizImage;
    private List<QuestionRequest> questions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getQuizImage() {
        return quizImage;
    }

    public void setQuizImage(String quizImage) {
        this.quizImage = quizImage;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }
}

class QuestionResponse {
    private Long id;
    private String text;
    private String questionType;
    private Integer questionOrder;
    private String questionImage;
    private Integer timeLimit;
    private Integer points;
    private List<AnswerResponse> answers;

    public QuestionResponse(Question question, List<Answer> answers) {
        this.id = question.getId();
        this.text = question.getQuestionText();
        this.questionType = question.getQuestionType() != null ? question.getQuestionType().toString() : null;
        this.questionOrder = question.getQuestionOrder();
        this.questionImage = question.getQuestionImage();
        this.timeLimit = question.getTimeLimit();
        this.points = question.getPoints();
        this.answers = answers.stream()
                .map(AnswerResponse::new)
                .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getQuestionType() {
        return questionType;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public String getQuestionImage() {
        return questionImage;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public Integer getPoints() {
        return points;
    }

    public List<AnswerResponse> getAnswers() {
        return answers;
    }
}

class AnswerResponse {
    private Long id;
    private String text;
    private boolean isCorrect;
    private Integer answerOrder;

    public AnswerResponse(Answer answer) {
        this.id = answer.getId();
        this.text = answer.getAnswerText();
        this.isCorrect = answer.getIsCorrect();
        this.answerOrder = answer.getAnswerOrder();
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public Integer getAnswerOrder() {
        return answerOrder;
    }
}

class QuizWithQuestionsResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private boolean isActive;
    private String quizImage;
    private List<QuestionResponse> questions;

    public QuizWithQuestionsResponse(Quiz quiz, List<QuestionResponse> questions) {
        this.id = quiz.getId();
        this.title = quiz.getTitle();
        this.description = quiz.getDescription();
        this.category = quiz.getCategory();
        this.isActive = quiz.getIsActive();
        this.quizImage = quiz.getQuizImage();
        this.questions = questions;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getQuizImage() {
        return quizImage;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }
}

class SuperAdminQuizResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String quizImage;
    private boolean isActive;
    private int questionCount;
    private CreatorInfo creator;

    public SuperAdminQuizResponse(Quiz quiz, int questionCount) {
        this.id = quiz.getId();
        this.title = quiz.getTitle();
        this.description = quiz.getDescription();
        this.category = quiz.getCategory();
        this.quizImage = quiz.getQuizImage();
        this.isActive = quiz.getIsActive();
        this.questionCount = questionCount;
        this.creator = new CreatorInfo(quiz.getCreator());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getQuizImage() {
        return quizImage;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public CreatorInfo getCreator() {
        return creator;
    }
}

class CreatorInfo {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public CreatorInfo(User creator) {
        this.id = creator.getId();
        this.username = creator.getUsername();
        this.email = creator.getEmail();
        this.firstName = creator.getFirstName();
        this.lastName = creator.getLastName();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}