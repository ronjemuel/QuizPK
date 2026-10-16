package com.zeke.quizpk.data.local;

import com.zeke.quizpk.domain.Question;
import com.zeke.quizpk.domain.QuizConfig;
import java.util.List;
import java.util.Random;

/** Shared catalogue boundary for offline play and future moderator bank selection. */
public interface QuestionRepository {
    int count(String category, String difficulty);
    List<Question> select(QuizConfig config, Random random);
}
