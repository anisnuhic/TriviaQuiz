package servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.QuizSessionService;
import service.QuizParticipantService;
import service.QuizService;
import model.QuizSession;
import model.QuizParticipant;
import model.Quiz;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@WebServlet("/scores")
public class ScoreServlet extends HttpServlet {

    private QuizSessionService sessionService;
    private QuizParticipantService participantService;
    private QuizService quizService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.sessionService = new QuizSessionService();
        this.participantService = new QuizParticipantService();
        this.quizService = new QuizService();
        this.gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        try {
            String sessionPin = request.getParameter("sessionPin");
            String quizIdParam = request.getParameter("quizId");

            if (sessionPin != null && !sessionPin.trim().isEmpty()) {

                handleSessionPinRequest(sessionPin, response, out);
            } else if (quizIdParam != null && !quizIdParam.trim().isEmpty()) {

                handleQuizIdRequest(quizIdParam, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "sessionPin ili quizId parametar je obavezan");
                out.print(gson.toJson(error));
                return;
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Došlo je do greške: " + e.getMessage());
            out.print(gson.toJson(error));
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }

    private void handleSessionPinRequest(String sessionPin, HttpServletResponse response, PrintWriter out)
            throws Exception {

        Optional<QuizSession> sessionOpt = sessionService.findBySessionPinWithQuiz(sessionPin);

        if (!sessionOpt.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Sesija sa PIN-om " + sessionPin + " ne postoji");
            out.print(gson.toJson(error));
            return;
        }

        QuizSession session = sessionOpt.get();

        List<QuizParticipant> participants = participantService.getParticipantsBySession(session);

        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("sessionPin", sessionPin);
        jsonResponse.addProperty("quizTitle", session.getQuiz().getTitle());
        jsonResponse.addProperty("sessionStatus", session.getStatus().toString());
        jsonResponse.addProperty("totalParticipants", participants.size());

        JsonArray participantsArray = new JsonArray();

        for (QuizParticipant participant : participants) {
            JsonObject participantObj = new JsonObject();
            participantObj.addProperty("username", participant.getParticipantName());
            participantObj.addProperty("participantId", participant.getParticipantId());
            participantObj.addProperty("totalScore", participant.getTotalScore());
            participantObj.addProperty("correctAnswers", participant.getCorrectAnswers());
            participantObj.addProperty("totalAnswers", participant.getTotalAnswers());
            participantObj.addProperty("status", participant.getStatus().toString());

            if (participant.getTotalAnswers() > 0) {
                double accuracy = (double) participant.getCorrectAnswers() / participant.getTotalAnswers() * 100;
                participantObj.addProperty("accuracy", Math.round(accuracy * 100.0) / 100.0);
            } else {
                participantObj.addProperty("accuracy", 0.0);
            }

            participantsArray.add(participantObj);
        }

        jsonResponse.add("participants", participantsArray);

        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(jsonResponse));
    }

    private void handleQuizIdRequest(String quizIdParam, HttpServletResponse response, PrintWriter out)
            throws Exception {
        Long quizId;
        try {
            quizId = Long.parseLong(quizIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject error = new JsonObject();
            error.addProperty("error", "quizId mora biti valjan broj");
            out.print(gson.toJson(error));
            return;
        }

        Optional<Quiz> quizOpt = quizService.findQuizById(quizId);
        if (!quizOpt.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Kviz sa ID " + quizId + " ne postoji");
            out.print(gson.toJson(error));
            return;
        }

        Quiz quiz = quizOpt.get();

        List<QuizSession> quizSessions = sessionService.getSessionsByQuizId(quizId);

        List<QuizParticipant> allParticipants = new ArrayList<>();
        for (QuizSession session : quizSessions) {
            List<QuizParticipant> sessionParticipants = participantService.getParticipantsBySession(session);
            allParticipants.addAll(sessionParticipants);
        }

        JsonObject statisticsResponse = new JsonObject();
        statisticsResponse.addProperty("quizId", quizId);
        statisticsResponse.addProperty("quizTitle", quiz.getTitle());
        statisticsResponse.addProperty("totalParticipants", allParticipants.size());
        statisticsResponse.addProperty("totalSessions", quizSessions.size());

        if (!allParticipants.isEmpty()) {
            double avgScore = allParticipants.stream()
                    .filter(p -> p.getTotalAnswers() > 0)
                    .mapToDouble(p -> (double) p.getCorrectAnswers() / p.getTotalAnswers() * 100)
                    .average()
                    .orElse(0.0);
            statisticsResponse.addProperty("avgScore", Math.round(avgScore * 100.0) / 100.0);
        } else {
            statisticsResponse.addProperty("avgScore", 0.0);
        }

        List<QuizParticipant> topParticipants = allParticipants.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getTotalScore(), p1.getTotalScore()))
                .limit(10)
                .collect(Collectors.toList());

        JsonArray bestResultsArray = new JsonArray();
        for (QuizParticipant participant : topParticipants) {
            JsonObject participantObj = new JsonObject();
            participantObj.addProperty("username", participant.getParticipantName());
            participantObj.addProperty("score", participant.getTotalScore());
            participantObj.addProperty("correctAnswers", participant.getCorrectAnswers());
            participantObj.addProperty("totalAnswers", participant.getTotalAnswers());

            if (participant.getTotalAnswers() > 0) {
                double accuracy = (double) participant.getCorrectAnswers() / participant.getTotalAnswers() * 100;
                participantObj.addProperty("accuracy", Math.round(accuracy * 100.0) / 100.0);
            } else {
                participantObj.addProperty("accuracy", 0.0);
            }

            bestResultsArray.add(participantObj);
        }

        statisticsResponse.add("bestResults", bestResultsArray);

        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(statisticsResponse));
    }
}