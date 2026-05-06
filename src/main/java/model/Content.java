package model;

import org.bson.types.ObjectId;

public class Content {
    private ObjectId id;
    private String type; // "WORD" or "SENTENCE"
    private String text;
    private String difficulty; // "EASY", "MEDIUM", "HARD"
    private String category;
    private boolean active;

    public Content() {
        this.active = true;
    }

    public Content(String type, String text, String difficulty, String category, boolean active) {
        this.type = type;
        this.text = text;
        this.difficulty = difficulty;
        this.category = category;
        this.active = active;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
