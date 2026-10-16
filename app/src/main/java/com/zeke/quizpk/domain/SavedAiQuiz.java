package com.zeke.quizpk.domain;

public final class SavedAiQuiz {
    public final String id,title,topic,difficulty;
    public final int count,seconds;
    public final long createdAt;
    public SavedAiQuiz(String id,String title,String topic,String difficulty,int count,int seconds,long createdAt) {
        this.id=id;this.title=title;this.topic=topic;this.difficulty=difficulty;this.count=count;this.seconds=seconds;this.createdAt=createdAt;
    }
}
