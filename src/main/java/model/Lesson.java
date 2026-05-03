package model;

import org.bson.types.ObjectId;

public class Lesson {

    private ObjectId id;
    private String   lessonId;     // e.g. "lesson_001"
    private String   title;
    private String   level;        // "Beginner" | "Intermediate" | "Advanced"
    private String   content;      // passage the user must type
    private String   fingerHint;   // keyboard/posture guidance shown above the passage
    private boolean  isPremium;
    private int      lessonNumber; // display ordering (1-based)
    private int      targetWpm;    // suggested minimum WPM goal

    public Lesson() {}

    public Lesson(String lessonId, String title, String level, String content,
                  String fingerHint, boolean isPremium, int lessonNumber, int targetWpm) {
        this.lessonId     = lessonId;
        this.title        = title;
        this.level        = level;
        this.content      = content;
        this.fingerHint   = fingerHint;
        this.isPremium    = isPremium;
        this.lessonNumber = lessonNumber;
        this.targetWpm    = targetWpm;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()               { return id; }
    public void setId(ObjectId id)        { this.id = id; }

    public String getLessonId()           { return lessonId; }
    public void setLessonId(String v)     { this.lessonId = v; }

    public String getTitle()              { return title; }
    public void setTitle(String v)        { this.title = v; }

    public String getLevel()              { return level; }
    public void setLevel(String v)        { this.level = v; }

    public String getContent()            { return content; }
    public void setContent(String v)      { this.content = v; }

    public String getFingerHint()         { return fingerHint; }
    public void setFingerHint(String v)   { this.fingerHint = v; }

    public boolean isPremium()            { return isPremium; }
    public void setPremium(boolean v)     { this.isPremium = v; }

    public int getLessonNumber()          { return lessonNumber; }
    public void setLessonNumber(int v)    { this.lessonNumber = v; }

    public int getTargetWpm()             { return targetWpm; }
    public void setTargetWpm(int v)       { this.targetWpm = v; }

    @Override
    public String toString() {
        return String.format("Lesson{id='%s', title='%s', level='%s', premium=%b}",
                lessonId, title, level, isPremium);
    }
}