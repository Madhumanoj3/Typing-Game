package model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

/**
 * Represents admin-managed typing content (words or paragraphs)
 * stored in the {@code content} collection.
 */
public class TypingContent {

    private ObjectId      id;
    private String        type;         // "WORD" | "PARAGRAPH"
    private String        text;
    private String        difficulty;   // "Beginner" | "Intermediate" | "Advanced"
    private LocalDateTime createdAt;

    public TypingContent() {
        this.createdAt = LocalDateTime.now();
    }

    public TypingContent(String type, String text, String difficulty) {
        this();
        this.type       = type;
        this.text       = text;
        this.difficulty = difficulty;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public ObjectId getId()                    { return id; }
    public void setId(ObjectId id)             { this.id = id; }

    public String getType()                    { return type; }
    public void setType(String v)              { this.type = v; }

    public String getText()                    { return text; }
    public void setText(String v)              { this.text = v; }

    public String getDifficulty()              { return difficulty; }
    public void setDifficulty(String v)        { this.difficulty = v; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)  { this.createdAt = v; }

    @Override
    public String toString() {
        return String.format("TypingContent{type='%s', difficulty='%s', text='%s'}",
                type, difficulty, text != null && text.length() > 30 ? text.substring(0, 30) + "..." : text);
    }
}
