
package servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Answer;
import model.Question;
import model.Quiz;
import model.QuizParticipant;
import model.QuizSession;
import service.AnswerService;
import service.QuestionService;
import service.QuizParticipantService;
import service.QuizService;
import service.QuizSessionService;

@WebServlet("/sessionToQuiz/*")
public class SessionToQuizServlet extends HttpServlet {
    private QuizSessionService sessionService;
    private QuizService quizService;
    private QuestionService questionService;
    private AnswerService answerService;
    private QuizParticipantService quizParticipantService;
    private Gson gson;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String sessionPin = req.getPathInfo().substring(1);
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<QuizSession> sessionOpt = sessionService.findActiveSessionByPin(sessionPin);
            QuizSession session = sessionOpt.get();

            Optional<Quiz> quizOpt = quizService.findQuizWithQuestions(session.getQuiz().getId());
            Quiz quiz = quizOpt.get();

            List<Question> questions = questionService.findQuestionsByQuiz(quiz);

            List<QuestionResponse> questionResponses = questions.stream()
                    .map(question -> {
                        List<Answer> answers = answerService.findAnswersByQuestion(question);
                        return new QuestionResponse(question, answers);
                    })
                    .collect(Collectors.toList());

            QuizWithQuestionsResponse quizResponse = new QuizWithQuestionsResponse(quiz, questionResponses);

            List<QuizParticipant> participantsList = quizParticipantService.getParticipantsBySession(session);
            System.out.println(participantsList);
            List<ParticipantResponse> participantResponses = participantsList.stream()
                    .map(ParticipantResponse::new)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("quiz", quizResponse);
            response.put("participants", participantResponses);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        Gson simpleGson = new GsonBuilder().setPrettyPrinting().create();
        resp.getWriter().print(simpleGson.toJson(response));
    }

    @Override
    public void init() throws ServletException {
        sessionService = new QuizSessionService();
        quizService = new QuizService();
        questionService = new QuestionService();
        answerService = new AnswerService();
        quizParticipantService = new QuizParticipantService();
        gson = new Gson();
    }
}

class ParticipantResponse {
    private String participantId;
    private String participantName;

    public ParticipantResponse(QuizParticipant participant) {
        this.participantId = participant.getParticipantId();
        this.participantName = participant.getParticipantName();
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }
}