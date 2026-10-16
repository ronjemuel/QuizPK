package com.zeke.quizpk.domain;

/** Offline rule: 100 points per correct answer, no speed bonus or penalty. */
public final class QuizScoring {
    public final int correct, incorrect, unanswered, score, accuracy;
    public QuizScoring(QuizSession session) {
        int c = 0, w = 0;
        for (int i = 0; i < session.questions.size(); i++) {
            if (session.outcome(i) == QuizSession.Outcome.CORRECT) c++;
            if (session.outcome(i) == QuizSession.Outcome.INCORRECT) w++;
        }
        correct = c; incorrect = w; unanswered = session.questions.size() - c - w;
        score = points(c); accuracy = Math.round(100f * c / session.questions.size());
    }
    public static int points(int correct) { return correct * 100; }
}
