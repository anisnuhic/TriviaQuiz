package servlets;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.QuizSession;
import model.User;
import service.QuizSessionService;
import service.UserService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet("/admin/createQuizSession")
public class CreateQuizSessionServlet extends HttpServlet {

    private QuizSessionService quizSessionService;
    private UserService userService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.quizSessionService = new QuizSessionService();
        this.userService = new UserService();
        this.gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        Map<String, Object> jsonResponse = new HashMap<>();

        try {

            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Niste ulogovani");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            Long sessionUserId = (Long) session.getAttribute("userId");

            Optional<User> userOpt = userService.findById(sessionUserId);
            if (!userOpt.isPresent()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Korisnik nije pronađen");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            User currentUser = userOpt.get();

            String quizIdParam = request.getParameter("quizId");
            if (quizIdParam == null || quizIdParam.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Quiz ID je obavezan");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            Long quizId;
            try {
                quizId = Long.parseLong(quizIdParam);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Nevaljan Quiz ID");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            QuizSession quizSession = quizSessionService.createQuizSession(quizId, currentUser);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Sesija je uspješno kreirana");

            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", quizSession.getId());
            sessionData.put("sessionPin", quizSession.getSessionPin());
            sessionData.put("quizTitle", quizSession.getQuiz().getTitle());
            sessionData.put("status", quizSession.getStatus().toString());
            sessionData.put("createdAt", quizSession.getCreatedAt().toString());

            jsonResponse.put("session", sessionData);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(jsonResponse));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Invalid ID format");
            out.print(gson.toJson(jsonResponse));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Greška pri kreiranju sesije: " + e.getMessage());
            out.print(gson.toJson(jsonResponse));

            System.out.println("ERROR creating quiz session: " + e.getMessage());
            e.printStackTrace();
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("{\"success\": false, \"message\": \"GET metoda nije dozvoljena\"}");
    }
}