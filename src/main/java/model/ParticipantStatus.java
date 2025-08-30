package model;

public enum ParticipantStatus {
    WAITING,    // Čeka da kviz počne
    READY,      // Spreman za kviz  
    PLAYING,    // Trenutno igra kviz
    FINISHED,   // Završio kviz
    DISCONNECTED // Prekinuta konekcija
}