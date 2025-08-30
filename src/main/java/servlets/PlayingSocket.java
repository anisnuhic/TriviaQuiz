package servlets;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import model.*;
import service.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@ServerEndpoint("/playingQuiz/{sessionPin}/{participantId}/{quizId}")
public class PlayingSocket {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Session, String> sessionToParticipantId = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Session, String> sessionToQuizSessionId = Collections.synchronizedMap(new HashMap<>());

    private static final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final Gson gson = new Gson();

    private static class GameState {
        final Set<String> participants = ConcurrentHashMap.newKeySet();
        final Set<String> answeredParticipants = ConcurrentHashMap.newKeySet();
        final Map<String, Integer> scores = new ConcurrentHashMap<>();
        final Map<String, List<String>> questionResponses = new ConcurrentHashMap<>();

        List<Question> questions = new ArrayList<>();
        int currentQuestionIndex = 0;
        ScheduledFuture<?> timer;

        void reset() {
            answeredParticipants.clear();
        }

        void cleanup() {
            if (timer != null)
                timer.cancel(false);
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionPin") String sessionPin,
            @PathParam("participantId") String participantId) {
        sessions.add(session);
        sessionToQuizSessionId.put(session, sessionPin);
        sessionToParticipantId.put(session, participantId);

        if (!"HOST".equals(participantId)) {
            gameStates.computeIfAbsent(sessionPin, k -> new GameState()).participants.add(participantId);
        }

        System.out.println("Connected: " + participantId + " to session: " + sessionPin);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {
                case "START_TIMER" -> handleStartTimer(json, session);
                case "START_QUESTIONS" -> handleStartQuestions(json, session);
                case "SUBMIT_ANSWER" -> handleSubmitAnswer(json, session);
                case "PARTICIPANT_LEFT" -> handleParticipantLeft(json, session);
                case "HOST_LEFT" -> handleHostLeft(json, session);
                case "HOST_NEXT_QUESTION" -> handleHostNextQuestion(json, session);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        String sessionPin = sessionToQuizSessionId.get(session);
        String participantId = sessionToParticipantId.get(session);

        if (sessionPin != null && participantId != null && !"HOST".equals(participantId)) {
            GameState state = gameStates.get(sessionPin);
            if (state != null) {
                state.participants.remove(participantId);
                if (state.participants.isEmpty()) {
                    state.cleanup();
                    gameStates.remove(sessionPin);
                }
            }
        }

        sessions.remove(session);
        sessionToQuizSessionId.remove(session);
        sessionToParticipantId.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    private void handleStartTimer(JsonObject json, Session session) {
        String sessionPin = sessionToQuizSessionId.get(session);
        if (sessionPin != null) {
            broadcastToSession(gson.toJson(json), sessionPin);
        }
    }

    private void handleStartQuestions(JsonObject json, Session session) {
        String sessionPin = sessionToQuizSessionId.get(session);
        String quizId = json.get("quizId").getAsString();

        try {
            QuizService quizService = new QuizService();
            QuestionService questionService = new QuestionService();

            Optional<Quiz> quizOpt = quizService.findQuizById(Long.parseLong(quizId));
            if (quizOpt.isEmpty()) {
                sendError(sessionPin, "Quiz not found");
                return;
            }

            List<Question> questions = questionService.findQuestionsByQuizWithAnswers(quizOpt.get());
            if (questions.isEmpty()) {
                sendError(sessionPin, "No questions found");
                return;
            }

            questions.sort(Comparator.comparingInt(Question::getQuestionOrder));

            GameState state = gameStates.computeIfAbsent(sessionPin, k -> new GameState());
            state.questions = questions;
            state.currentQuestionIndex = 0;

            sendQuestion(sessionPin, 0);

        } catch (Exception e) {
            System.err.println("Error starting questions: " + e.getMessage());
        }
    }

    private void handleSubmitAnswer(JsonObject json, Session session) {
        String participantId = json.get("participantId").getAsString();
        if ("HOST".equals(participantId))
            return;

        String sessionPin = sessionToQuizSessionId.get(session);
        String questionId = json.get("questionId").getAsString();
        String answerId = json.has("answerId") ? json.get("answerId").getAsString() : null;
        String textAnswer = json.has("text") ? json.get("text").getAsString() : null;
        boolean isCorrect = json.has("isCorrect") && json.get("isCorrect").getAsBoolean();

        try {

            ParticipantAnswerService answerService = new ParticipantAnswerService();
            boolean saved = answerService.saveParticipantAnswer(
                    participantId,
                    Long.parseLong(questionId),
                    answerId != null ? Long.parseLong(answerId) : null,
                    textAnswer);

            if (saved) {
                updateScore(sessionPin, participantId, isCorrect, questionId);
                notifyParticipantAnswered(sessionPin, participantId, isCorrect);

                GameState state = gameStates.get(sessionPin);
                if (state != null) {
                    state.answeredParticipants.add(participantId);
                    checkAllAnswered(sessionPin, state);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling answer: " + e.getMessage());
        }
    }

    private void handleParticipantLeft(JsonObject json, Session session) {
        String participantId = json.get("participantId").getAsString();
        String sessionPin = sessionToQuizSessionId.get(session);

        try {
            new QuizParticipantService().updateParticipantStatus(participantId, ParticipantStatus.DISCONNECTED);

            JsonObject message = new JsonObject();
            message.addProperty("type", "PARTICIPANT_LEFT");
            message.addProperty("participantId", participantId);

            broadcastToSession(gson.toJson(message), sessionPin);
        } catch (Exception e) {
            System.err.println("Error handling participant left: " + e.getMessage());
        }
    }

    private void handleHostLeft(JsonObject json, Session session) {
        String sessionPin = sessionToQuizSessionId.get(session);
        if (sessionPin == null)
            return;

        try {

            JsonObject message = new JsonObject();
            message.addProperty("type", "HOST_LEFT");
            message.addProperty("message", "Host left. Session terminated.");
            broadcastToSession(gson.toJson(message), sessionPin);

            cleanupSession(sessionPin);

            GameState state = gameStates.remove(sessionPin);
            if (state != null)
                state.cleanup();

        } catch (Exception e) {
            System.err.println("Error handling host left: " + e.getMessage());
        }
    }

    private void handleHostNextQuestion(JsonObject json, Session session) {
        String sessionPin = sessionToQuizSessionId.get(session);
        if (sessionPin == null)
            return;

        GameState state = gameStates.get(sessionPin);
        if (state != null) {

            if (state.timer != null) {
                state.timer.cancel(false);
                state.timer = null;
            }

            state.currentQuestionIndex++;
            scheduler.schedule(() -> sendQuestion(sessionPin, state.currentQuestionIndex), 1, TimeUnit.SECONDS);
        }
    }

    private void updateScore(String sessionPin, String participantId, boolean isCorrect, String questionId) {
        GameState state = gameStates.get(sessionPin);
        if (state == null)
            return;

        List<String> responses = state.questionResponses.computeIfAbsent(questionId, k -> new ArrayList<>());
        if (responses.contains(participantId))
            return;

        responses.add(participantId);

        try {
            QuizParticipantService participantService = new QuizParticipantService();

            Optional<QuizParticipant> participantOpt = participantService.findByParticipantId(participantId);
            if (!participantOpt.isPresent()) {
                System.err.println("Participant not found: " + participantId);
                return;
            }

            QuizParticipant participant = participantOpt.get();

            int newTotalAnswers = participant.getTotalAnswers() + 1;
            int newCorrectAnswers = participant.getCorrectAnswers() + (isCorrect ? 1 : 0);
            int newTotalScore = participant.getTotalScore();

            if (isCorrect && state.currentQuestionIndex < state.questions.size()) {
                Question question = state.questions.get(state.currentQuestionIndex);
                newTotalScore += question.getPoints();

                state.scores.merge(participantId, question.getPoints(), Integer::sum);
            }

            participantService.updateParticipantScore(participantId, newTotalScore, newCorrectAnswers, newTotalAnswers);

            System.out.println("Updated participant " + participantId + ": " +
                    newCorrectAnswers + "/" + newTotalAnswers + " correct, " +
                    newTotalScore + " points");

            sendScoreUpdate(sessionPin, state);

        } catch (Exception e) {
            System.err.println("Error updating participant score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendScoreUpdate(String sessionPin, GameState state) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "SCORE_UPDATE");

        JsonObject scores = new JsonObject();
        state.scores.forEach(scores::addProperty);
        message.add("scores", scores);

        broadcastToSession(gson.toJson(message), sessionPin);
    }

    private void notifyParticipantAnswered(String sessionPin, String participantId, boolean isCorrect) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "PARTICIPANT_ANSWERED");
        message.addProperty("participantId", participantId);
        message.addProperty("isCorrect", isCorrect);
        message.addProperty("sessionPin", sessionPin);

        broadcastToSession(gson.toJson(message), sessionPin);
    }

    private void sendQuestion(String sessionPin, int questionIndex) {
        GameState state = gameStates.get(sessionPin);
        if (state == null || questionIndex >= state.questions.size()) {
            sendQuizCompleted(sessionPin);
            return;
        }

        Question question = state.questions.get(questionIndex);
        state.reset();

        JsonObject message = createQuestionMessage(question, questionIndex, state.questions.size());
        broadcastToSession(gson.toJson(message), sessionPin);

        System.out.println("Question " + (questionIndex + 1) + " sent to session: " + sessionPin);
    }

    private JsonObject createQuestionMessage(Question question, int index, int total) {
        JsonObject message = new JsonObject();
        message.addProperty("type", index == 0 ? "FIRST_QUESTION" : "NEXT_QUESTION");
        message.addProperty("questionId", question.getId().toString());
        message.addProperty("questionText", question.getQuestionText());
        message.addProperty("questionImage", question.getQuestionImage());
        message.addProperty("questionOrder", question.getQuestionOrder());
        message.addProperty("timeLimit", question.getTimeLimit());
        message.addProperty("points", question.getPoints());
        message.addProperty("questionType", question.getQuestionType().toString());
        message.addProperty("totalQuestions", total);
        message.addProperty("currentQuestionNumber", index + 1);

        JsonArray answers = new JsonArray();
        question.getAnswers().forEach(answer -> {
            JsonObject answerObj = new JsonObject();
            answerObj.addProperty("id", answer.getId().toString());
            answerObj.addProperty("answerText", answer.getAnswerText());
            answerObj.addProperty("answerOrder", answer.getAnswerOrder());
            answerObj.addProperty("isCorrect", answer.getIsCorrect());
            answers.add(answerObj);
        });
        message.add("answers", answers);

        return message;
    }

    private void checkAllAnswered(String sessionPin, GameState state) {
        int totalParticipants = state.participants.size();
        int answeredCount = state.answeredParticipants.size();

        System.out.println("Session " + sessionPin + ": " + answeredCount + "/" + totalParticipants + " answered");

        if (answeredCount >= totalParticipants) {
            System.out.println("All participants answered for session: " + sessionPin);
        }
    }

    private void sendQuizCompleted(String sessionPin) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "QUIZ_COMPLETED");
        message.addProperty("message", "Quiz completed!");

        broadcastToSession(gson.toJson(message), sessionPin);

        try {
            QuizSessionService sessionService = new QuizSessionService();
            Optional<QuizSession> sessionOpt = sessionService.findBySessionPin(sessionPin);

            if (sessionOpt.isPresent()) {
                QuizSession session = sessionOpt.get();
                new QuizParticipantService().markQuizFinishedForSession(session);
                sessionService.finishSession(session.getId());
            }
        } catch (Exception e) {
            System.err.println("Error completing quiz: " + e.getMessage());
        }

        GameState state = gameStates.remove(sessionPin);
        if (state != null)
            state.cleanup();
    }

    private void cleanupSession(String sessionPin) {
        try {
            QuizSessionService sessionService = new QuizSessionService();
            Optional<QuizSession> sessionOpt = sessionService.findBySessionPin(sessionPin);

            if (sessionOpt.isPresent()) {
                QuizSession session = sessionOpt.get();

                new ParticipantAnswerService().deleteAnswersBySessionPin(sessionPin);
                new QuizParticipantService().removeAllParticipantsFromSession(session);
                sessionService.deleteSession(session.getId());
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up session: " + e.getMessage());
        }
    }

    private void sendError(String sessionPin, String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "ERROR");
        error.addProperty("message", errorMessage);

        broadcastToSession(gson.toJson(error), sessionPin);
    }

    private void broadcastToSession(String message, String sessionPin) {
        synchronized (sessions) {
            sessions.stream()
                    .filter(Session::isOpen)
                    .filter(s -> sessionPin.equals(sessionToQuizSessionId.get(s)))
                    .forEach(s -> {
                        try {
                            s.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            System.err.println("Error sending message: " + e.getMessage());
                        }
                    });
        }
    }
}