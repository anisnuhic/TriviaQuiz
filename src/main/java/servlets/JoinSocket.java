package servlets;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import model.QuizParticipant;
import model.QuizSession;
import service.QuizParticipantService;
import service.QuizSessionService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@ServerEndpoint("/joinQuizSocket/{sessionPin}")
public class JoinSocket {

    private static Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private static Map<Session, String> sessionToParticipantId = Collections.synchronizedMap(new HashMap<>());

    private static Map<Session, String> sessionToQuizSessionId = Collections.synchronizedMap(new HashMap<>());

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionPin") String sessionPin) {
        sessions.add(session);

        sessionToQuizSessionId.put(session, sessionPin);
        System.out.println("WebSocket otvoren: " + session.getId() + " za quiz: " + sessionPin);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
        try {
            Gson gson = new Gson();
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);

            String messageType = jsonMessage.has("type") ? jsonMessage.get("type").getAsString() : "";

            if ("HOST_LEFT".equals(messageType)) {
                handleHostLeft(jsonMessage, session);
                return;
            }

            if ("QUIZ_STARTING".equals(messageType)) {
                handleQuizStarting(jsonMessage, session);
                return;
            }

            String sessionId = null;
            String username = null;
            if (jsonMessage.has("sessionId")) {
                sessionId = jsonMessage.get("sessionId").getAsString();
                System.out.println("Session ID: " + sessionId);
            }
            if (jsonMessage.has("name")) {
                username = jsonMessage.get("name").getAsString();
                System.out.println("Username: " + username);
            }

            QuizParticipantService participantService = new QuizParticipantService();
            QuizSessionService sessionService = new QuizSessionService();

            Optional<QuizSession> sessionOpt = sessionService.findBySessionPin(sessionId);

            if (!sessionOpt.isPresent()) {
                System.out.println("Nema session");
                return;
            }

            QuizSession quizSession = sessionOpt.get();

            QuizParticipant participant = participantService.createParticipant(
                    quizSession, username);

            sessionToParticipantId.put(session, participant.getParticipantId());

            System.out.println("Povezao WebSocket sesiju sa participantom: " + participant.getParticipantId());

            JsonObject joinResponse = new JsonObject();
            joinResponse.addProperty("type", "JOIN");
            joinResponse.addProperty("name", username);
            joinResponse.addProperty("participantId", participant.getParticipantId());
            joinResponse.addProperty("sessionId", sessionId);
            if (jsonMessage.has("timestamp")) {
                joinResponse.addProperty("timestamp", jsonMessage.get("timestamp").getAsString());
            }

            String responseMessage = gson.toJson(joinResponse);
            System.out.println(">>> PORUKA ZA SLANJE: " + responseMessage);

            System.out.println(">>> POČINJE BROADCAST...");
            int sentCount = 0;
            synchronized (sessions) {
                System.out.println(">>> UKUPNO SESIJA: " + sessions.size());
                for (Session s : sessions) {
                    System.out.println(">>> PROVJERA SESIJE: " + s.getId());
                    if (s.isOpen()) {
                        String sQuizSessionId = sessionToQuizSessionId.get(s);
                        System.out.println(">>>   - Quiz ID za ovu sesiju: " + sQuizSessionId);
                        System.out.println(">>>   - Target Quiz ID: " + sessionId);
                        System.out.println(">>>   - Jednaki? " + sessionId.equals(sQuizSessionId));

                        if (sessionId.equals(sQuizSessionId)) {
                            s.getBasicRemote().sendText(responseMessage);
                            sentCount++;
                            System.out.println(">>>   - POSLANA PORUKA!");
                        } else {
                            System.out.println(">>>   - Poruka NIJE poslana (quiz ID se ne slaže)");
                        }
                    } else {
                        System.out.println(">>>   - Sesija zatvorena, preskačem");
                    }
                }
            }
            System.out.println(">>> BROADCAST ZAVRŠEN. Poslano " + sentCount + " poruka.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleQuizStarting(JsonObject jsonMessage, Session hostSession) {
        try {
            String sessionId = jsonMessage.has("sessionId") ? jsonMessage.get("sessionId").getAsString() : null;
            String message = jsonMessage.has("message") ? jsonMessage.get("message").getAsString()
                    : "Host je napustio kviz";

            if (sessionId == null) {
                System.out.println("HOST_LEFT: Nema sessionId");
                return;
            }

            System.out.println("QUIZ_STARTING: Šaljem poruku svim participantima u sesiji: " + sessionId);

            Gson gson = new Gson();
            JsonObject hostLeftMessage = new JsonObject();
            hostLeftMessage.addProperty("type", "QUIZ_STARTING");
            hostLeftMessage.addProperty("message", message);
            hostLeftMessage.addProperty("sessionId", sessionId);

            String broadcastMessage = gson.toJson(hostLeftMessage);

            synchronized (sessions) {
                for (Session s : sessions) {
                    if (s.isOpen() && !s.equals(hostSession)) {
                        String sQuizSessionId = sessionToQuizSessionId.get(s);
                        if (sessionId.equals(sQuizSessionId)) {
                            s.getBasicRemote().sendText(broadcastMessage);
                            System.out.println("QUIZ_STARTING poruka poslana sesiji: " + s.getId());
                        }
                    }
                }
            }
            try {
                QuizParticipantService participantService = new QuizParticipantService();
                QuizSessionService sessionService = new QuizSessionService();

                Optional<QuizSession> sessionOpt = sessionService.findBySessionPin(sessionId);
                if (sessionOpt.isPresent()) {
                    QuizSession quizSession = sessionOpt.get();

                    System.out.println("HOST_LEFT: Brišem sve participante iz sesije: " + sessionId);
                    participantService.markParticipantsPlaying(quizSession);

                    System.out.println("Zapocinjem sesiju: " + sessionId);
                    sessionService.startSession(quizSession.getId());

                    System.out.println("Sesija " + sessionId + " je uspješno zapoceta.");
                } else {
                    System.out.println("Sesija " + sessionId + " nije pronađena u bazi");
                }

            } catch (Exception e) {
                System.out.println("Greška pri startanju sesije: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Greška pri slanju QUIZ_STARTING poruke: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleHostLeft(JsonObject jsonMessage, Session hostSession) {
        try {
            String sessionId = jsonMessage.has("sessionId") ? jsonMessage.get("sessionId").getAsString() : null;
            String message = jsonMessage.has("message") ? jsonMessage.get("message").getAsString()
                    : "Host je napustio kviz";

            if (sessionId == null) {
                System.out.println("HOST_LEFT: Nema sessionId");
                return;
            }

            System.out.println("HOST_LEFT: Šaljem poruku svim participantima u sesiji: " + sessionId);

            Gson gson = new Gson();
            JsonObject hostLeftMessage = new JsonObject();
            hostLeftMessage.addProperty("type", "HOST_LEFT");
            hostLeftMessage.addProperty("message", message);
            hostLeftMessage.addProperty("sessionId", sessionId);

            String broadcastMessage = gson.toJson(hostLeftMessage);

            synchronized (sessions) {
                for (Session s : sessions) {
                    if (s.isOpen() && !s.equals(hostSession)) {
                        String sQuizSessionId = sessionToQuizSessionId.get(s);
                        if (sessionId.equals(sQuizSessionId)) {
                            s.getBasicRemote().sendText(broadcastMessage);
                            System.out.println("HOST_LEFT poruka poslana sesiji: " + s.getId());
                        }
                    }
                }
            }

            try {
                QuizParticipantService participantService = new QuizParticipantService();
                QuizSessionService sessionService = new QuizSessionService();

                Optional<QuizSession> sessionOpt = sessionService.findBySessionPin(sessionId);
                if (sessionOpt.isPresent()) {
                    QuizSession quizSession = sessionOpt.get();

                    System.out.println("HOST_LEFT: Brišem sve participante iz sesije: " + sessionId);
                    participantService.removeAllParticipantsFromSession(quizSession);

                    System.out.println("HOST_LEFT: Završavam sesiju: " + sessionId);
                    sessionService.finishSession(quizSession.getId());

                    System.out.println("HOST_LEFT: Sesija " + sessionId + " je uspješno završena i očišćena");
                } else {
                    System.out.println("HOST_LEFT: Sesija " + sessionId + " nije pronađena u bazi");
                }

            } catch (Exception e) {
                System.out.println("HOST_LEFT: Greška pri brisanju sesije iz baze: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("Greška pri slanju HOST_LEFT poruke: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("WebSocket zatvoren: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {

        String participantId = sessionToParticipantId.get(session);
        String quizSessionId = sessionToQuizSessionId.get(session);

        if (participantId != null) {
            try {
                QuizParticipantService participantService = new QuizParticipantService();
                participantService.removeParticipant(participantId);
                System.out.println("Participant uklonjen zbog greške: " + participantId);
                sessionToParticipantId.remove(session);
                sessionToQuizSessionId.remove(session);
                broadcastParticipantLeft(participantId, quizSessionId);
            } catch (Exception e) {
                System.out.println("Greška pri uklanjanju participanta: " + e.getMessage());
            }
        }

        sessions.remove(session);
        System.out.println("WebSocket greška: " + throwable.getMessage());
    }

    private void broadcastParticipantLeft(String participantId, String quizSessionId) {
        try {
            Gson gson = new Gson();
            JsonObject leaveMessage = new JsonObject();
            leaveMessage.addProperty("type", "LEAVE");
            leaveMessage.addProperty("participantId", participantId);

            String message = gson.toJson(leaveMessage);

            synchronized (sessions) {
                for (Session s : sessions) {
                    if (s.isOpen()) {
                        String sQuizSessionId = sessionToQuizSessionId.get(s);

                        if (quizSessionId != null && quizSessionId.equals(sQuizSessionId)) {
                            s.getBasicRemote().sendText(message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Greška pri slanju LEAVE poruke: " + e.getMessage());
        }
    }
}