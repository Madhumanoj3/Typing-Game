package db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import model.Lesson;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the {@code lessons} collection.
 * Seeds the 10 built-in lessons on first connection if the collection is empty.
 */
public class LessonDAO {

    private static LessonDAO instance;
    private final MongoCollection<Document> col;

    private LessonDAO() {
        col = MongoDBManager.getInstance().getDatabase().getCollection("lessons");
        col.createIndex(new Document("lessonId", 1), new IndexOptions().unique(true));
        seedIfEmpty();
    }

    public static LessonDAO getInstance() {
        if (instance == null) instance = new LessonDAO();
        return instance;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public List<Lesson> getAll() {
        List<Lesson> list = new ArrayList<>();
        for (Document d : col.find().sort(Sorts.ascending("lessonNumber"))) {
            list.add(toLesson(d));
        }
        return list;
    }

    public List<Lesson> getByLevel(String level) {
        List<Lesson> list = new ArrayList<>();
        for (Document d : col.find(Filters.eq("level", level))
                             .sort(Sorts.ascending("lessonNumber"))) {
            list.add(toLesson(d));
        }
        return list;
    }

    public Lesson getById(String lessonId) {
        Document d = col.find(Filters.eq("lessonId", lessonId)).first();
        return d == null ? null : toLesson(d);
    }

    // ── Admin Write Operations ────────────────────────────────────────────

    /** Inserts a new lesson. */
    public void saveNew(Lesson lesson) {
        col.insertOne(fromLesson(lesson));
    }

    /** Updates an existing lesson by lessonId. */
    public void updateLesson(Lesson lesson) {
        col.replaceOne(
                Filters.eq("lessonId", lesson.getLessonId()),
                fromLesson(lesson),
                new ReplaceOptions().upsert(true)
        );
    }

    /** Deletes a lesson by lessonId. */
    public void deleteLesson(String lessonId) {
        col.deleteOne(Filters.eq("lessonId", lessonId));
    }

    // ── Seed data ─────────────────────────────────────────────────────────

    private void seedIfEmpty() {
        // Upsert all lessons so new lessons are added to existing databases
        for (Lesson l : buildSeedLessons()) upsert(l);
    }

    private void upsert(Lesson lesson) {
        Document doc = fromLesson(lesson);
        col.replaceOne(Filters.eq("lessonId", lesson.getLessonId()), doc,
                new ReplaceOptions().upsert(true));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private Lesson toLesson(Document d) {
        Lesson l = new Lesson();
        l.setLessonId(d.getString("lessonId"));
        l.setTitle(d.getString("title"));
        l.setLevel(d.getString("level"));
        l.setContent(d.getString("content"));
        l.setFingerHint(d.getString("fingerHint"));
        l.setPremium(Boolean.TRUE.equals(d.getBoolean("isPremium")));
        l.setLessonNumber(d.getInteger("lessonNumber", 0));
        l.setTargetWpm(d.getInteger("targetWpm", 20));
        return l;
    }

    private Document fromLesson(Lesson l) {
        return new Document()
                .append("lessonId",     l.getLessonId())
                .append("title",        l.getTitle())
                .append("level",        l.getLevel())
                .append("content",      l.getContent())
                .append("fingerHint",   l.getFingerHint())
                .append("isPremium",    l.isPremium())
                .append("lessonNumber", l.getLessonNumber())
                .append("targetWpm",    l.getTargetWpm());
    }

    // ── Seed lesson definitions ───────────────────────────────────────────

    private List<Lesson> buildSeedLessons() {
        List<Lesson> lessons = new ArrayList<>();

        // ── Beginner (Free) ───────────────────────────────────────────────
        lessons.add(new Lesson(
            "lesson_001", "Home Row Mastery", "Beginner",
            "aaa sss ddd fff jjj kkk lll asd fjk asd fjk fj dk sl a; fd jk fds jkl asdf jkl; " +
            "sad fads lads flask salad falls skull flags lass asks dads",
            "Place your LEFT hand on A S D F and RIGHT hand on J K L ; — these are your HOME ROW " +
            "keys. Every finger has a home: pinky→A, ring→S, middle→D, index→F (left) and " +
            "index→J, middle→K, ring→L, pinky→; (right). Always return here between keystrokes.",
            false, 1, 20
        ));

        lessons.add(new Lesson(
            "lesson_002", "Top Row Keys", "Beginner",
            "qqq www eee rrr ttt yyy uuu iii ooo ppp qwer tyui qwerty uiop " +
            "wet try yew pure tire ripe quit your worry tower",
            "TOP ROW — LEFT hand: Q(pinky) W(ring) E(middle) R(index) T(index). " +
            "RIGHT hand: Y(index) U(index) I(middle) O(ring) P(pinky). " +
            "Reach UP from home row without moving your wrists — only your fingers extend.",
            false, 2, 20
        ));

        lessons.add(new Lesson(
            "lesson_003", "Bottom Row Keys", "Beginner",
            "zzz xxx ccc vvv bbb nnn mmm zxcv bnm zxc vbn xcv bnm " +
            "cab van zinc venom calm zinc exam cabin bench",
            "BOTTOM ROW — LEFT hand: Z(pinky) X(ring) C(middle) V(index) B(index). " +
            "RIGHT hand: B(index) N(index) M(middle). Keep both thumbs resting on the " +
            "SPACEBAR — the space key is always pressed with the thumb opposite the last typed hand.",
            false, 3, 20
        ));

        lessons.add(new Lesson(
            "lesson_004", "Common Short Words", "Beginner",
            "the and for are but not all can her was one our out day get has him his how " +
            "man new now old see two way who did its let put say she too use make time " +
            "just like know take come good well also back after must work such here even",
            "These 40 words cover nearly 50% of all everyday English text. Practice them " +
            "until your fingers move automatically without conscious thought. Focus on " +
            "RHYTHM — each word should flow as a single muscle-memory motion, not letter by letter.",
            false, 4, 30
        ));

        // ── Intermediate (Premium) ────────────────────────────────────────
        lessons.add(new Lesson(
            "lesson_005", "Two-Hand Coordination", "Intermediate",
            "when they what there with from this have been that will your some more time " +
            "very name much just know take year them over such well even back then look " +
            "because come many both hand part world great still went think should around " +
            "those every never where right being under never doing again those found",
            "Words like 'when', 'then', 'both', 'make' naturally ALTERNATE between hands — " +
            "exploit this for speed. Type in a smooth fluid rhythm rather than bursting letter " +
            "by letter. Imagine your fingers as piano keys playing a melody, never rushing a note.",
            true, 5, 40
        ));

        lessons.add(new Lesson(
            "lesson_006", "Full Sentence Flow", "Intermediate",
            "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. " +
            "How vexingly quick daft zebras jump. The five boxing wizards jump quickly. " +
            "Sphinx of black quartz judge my vow. Waltz nymph for quick jigs vex bud.",
            "These are PANGRAMS — sentences using every letter of the alphabet. They are perfect " +
            "for full-keyboard practice. Notice how spaces between words act as micro-resets — " +
            "use each space to breathe and prepare for the next word.",
            true, 6, 45
        ));

        lessons.add(new Lesson(
            "lesson_007", "Numbers and Symbols", "Intermediate",
            "Call 555-1234 or email info@company.com for price list. " +
            "Order #4872 costs $49.99 (20% off). Deadline is 03/15/2025. " +
            "Use code: ABC-100 to save $10. Total: 3 items @ $15.00 each = $45.00",
            "NUMBERS: reach up with the same finger used for the letter directly below " +
            "(e.g. 1=Q finger, 4=R finger, 7=U finger). SHIFT for symbols: hold shift with " +
            "the OPPOSITE hand from the key. @ is Shift+2 with left hand holding shift.",
            true, 7, 30
        ));

        // ── Advanced (Premium) ────────────────────────────────────────────
        lessons.add(new Lesson(
            "lesson_008", "Speed Burst Drill", "Advanced",
            "the and that have with this will from they which their been more about would there " +
            "other time after first when your some them were come said each she all but are " +
            "from what make also just year people into over last since before take only work " +
            "life where back then most both down look feel place give most very still such",
            "SPEED FOCUS: Push beyond your comfort zone. Type each word faster than feels natural. " +
            "Speed is built by reducing HESITATION — trust your muscle memory and keep moving forward. " +
            "Do NOT stop to fix errors mid-word; finish the word and correct spacing issues only. " +
            "Target: 60+ WPM. Restart until you hit the target.",
            true, 8, 60
        ));

        lessons.add(new Lesson(
            "lesson_009", "Accuracy Challenge", "Advanced",
            "specifically deliberately comprehensively infrastructure entrepreneurship " +
            "characteristics approximately sophisticated internationally unfortunately " +
            "acknowledgement simultaneously responsibilities straightforward " +
            "conscientiousness multidimensional uncharacteristically incomprehensible",
            "ACCURACY FOCUS: Every error costs more time than going slowly. Type each word " +
            "ONCE perfectly rather than fast and wrong. Say each syllable mentally as you type: " +
            "spe-ci-fi-cal-ly. Aim for 100% accuracy. Speed will naturally follow with practice. " +
            "Target: zero errors. Restart from the beginning on any mistake.",
            true, 9, 40
        ));

        lessons.add(new Lesson(
            "lesson_010", "The Grand Challenge", "Advanced",
            "Typing mastery comes from daily deliberate practice across all letter combinations. " +
            "A skilled typist reaches beyond eighty words per minute with near-perfect accuracy, " +
            "their fingers dancing across the keyboard with the confidence of a concert pianist. " +
            "The journey begins with home row fundamentals and progresses through speed drills, " +
            "accuracy challenges, and full sentence composition. You have trained for this moment. " +
            "Trust your preparation and let your fingers flow freely across every key.",
            "GRAND CHALLENGE — combines everything: speed, accuracy, rhythm, and endurance. " +
            "Maintain a steady breathing pattern. Keep your posture upright, wrists floating " +
            "above the keys (not resting). Eyes on the screen, never on the keyboard. " +
            "This is your typing mastery test. Target: 70+ WPM with 95%+ accuracy.",
            true, 10, 70
        ));

        // ── Bonus: Alphabet & Suffix Lessons (Free + Premium) ────────────

        lessons.add(new Lesson(
            "lesson_011", "Alphabet A to M", "Beginner",
            "a b c d e f g h i j k l m ab ac ad ae af ag ah ai aj ak al am " +
            "ba ca da ea fa ga ha ia ja ka la ma abc bcd cde def efg fgh ghi hij ijk jkl klm",
            "Practice the FIRST HALF of the alphabet in order. Say each letter aloud as you type it. " +
            "This builds muscle memory for letter positions. A-M covers your left hand's primary keys " +
            "plus the transition across the center. Keep both hands on home row and reach for each key.",
            false, 11, 25
        ));

        lessons.add(new Lesson(
            "lesson_012", "Alphabet N to Z", "Beginner",
            "n o p q r s t u v w x y z no op pq qr rs st tu uv vw wx xy yz " +
            "nop opq pqr qrs rst stu tuv uvw vwx wxy xyz no op pq qr rs st tu uv vwxyz",
            "Practice the SECOND HALF of the alphabet. N-Z are spread across the right side of the " +
            "keyboard. Notice that N, M are typed with the right index and middle fingers. Q, Z are " +
            "your left pinky — reach from A row upward. Keep your posture straight and wrists floating.",
            false, 12, 25
        ));

        lessons.add(new Lesson(
            "lesson_013", "Full Alphabet Drill", "Beginner",
            "a b c d e f g h i j k l m n o p q r s t u v w x y z " +
            "abcdefghijklm nopqrstuvwxyz abcdefghijklmnopqrstuvwxyz " +
            "the quick brown fox jumps over a lazy dog pack my box with five dozen liquor jugs",
            "Type the FULL ALPHABET in sequence, then the two classic pangrams. " +
            "Your goal: type each letter without looking at the keyboard. If you hesitate on any " +
            "letter, isolate it and practice: that letter plus its neighbours 10 times.",
            false, 13, 30
        ));

        lessons.add(new Lesson(
            "lesson_014", "Reverse Alphabet", "Beginner",
            "z y x w v u t s r q p o n m l k j i h g f e d c b a " +
            "zy yx xw wv vu ut ts sr rq qp po on nm ml lk kj ji ih hg gf fe ed dc cb ba " +
            "zyx yxw xwv wvu vut uts tsr srq rqp qpo pon onm nml mlk lkj kji jih ihg hgf gfe fed edc dcb cba",
            "Typing the REVERSE ALPHABET breaks your automatic forward-sequence habit and forces your " +
            "brain to locate each key independently. This is an advanced coordination drill. " +
            "Start slowly — accuracy matters more than speed here. Build to fluency over multiple attempts.",
            false, 14, 25
        ));

        lessons.add(new Lesson(
            "lesson_015", "Words Ending in -ING", "Intermediate",
            "running jumping typing writing reading playing working thinking looking coming " +
            "going making taking doing being having feeling trying keeping calling bringing " +
            "setting getting moving learning teaching sitting standing waiting showing building " +
            "drawing flying sleeping eating drinking speaking listening holding walking turning",
            "Words ending in -ING are extremely common in English. Notice that 'ing' is always typed " +
            "with your RIGHT hand: I(middle), N(index), G(index crosses to left). " +
            "The ING trigram should become a single fluid motion: index stretches to G while " +
            "middle and ring fingers prepare for I and N. Practice until ING flows automatically.",
            true, 15, 45
        ));

        lessons.add(new Lesson(
            "lesson_016", "Words Ending in -AND", "Intermediate",
            "and band hand land sand grand stand bland brand command demand expand " +
            "understand withstand husband island mainland farmland wasteland overland " +
            "outstand offhand firsthand shorthand secondhand beforehand reprimand " +
            "contraband firebrand headband armband headstand handstand kickstand",
            "The -AND suffix is one of the most common word endings in English. " +
            "A-N-D: A is left pinky, N is right index, D is left middle. This alternates hands perfectly! " +
            "Exploit this hand-alternation for maximum speed. The word 'and' itself should take " +
            "under 0.3 seconds at 60 WPM — it's one of your fastest words.",
            true, 16, 40
        ));

        lessons.add(new Lesson(
            "lesson_017", "Words Ending in -EAR", "Intermediate",
            "ear hear fear near year bear dear gear tear wear clear shear smear spear " +
            "appear unclear nuclear linear career veneer pioneer volunteer sincere severe " +
            "atmosphere hemisphere disappear reappear engineer persevere interfere " +
            "adhere austere cashier chandelier frontier musketeer mutineer pamphleteer",
            "Words ending in -EAR combine the E-A digraph (both typed with left hand middle finger " +
            "and left ring finger) followed by R (left index). This is a left-hand cluster — " +
            "your right hand gets a brief rest. Practice the EAR trigram repeatedly: " +
            "ear ear ear ear — then move to full words.",
            true, 17, 45
        ));

        lessons.add(new Lesson(
            "lesson_018", "Words Ending in -TION", "Advanced",
            "action nation station question solution pollution attention mention position " +
            "education information situation imagination participation demonstration " +
            "administration organization communication consideration determination " +
            "transformation transportation recommendation accommodation investigation " +
            "implementation concentration cooperation collaboration interpretation",
            "The -TION suffix is everywhere in formal English. T-I-O-N alternates between hands: " +
            "T(left index), I(right middle), O(right ring), N(right index). " +
            "After the T, your entire right hand takes over for ION. " +
            "Mastering TION turns these long words into fast, flowing sequences. " +
            "Target: type 'information' in under 1.5 seconds at 60+ WPM.",
            true, 18, 55
        ));

        lessons.add(new Lesson(
            "lesson_019", "Words Ending in -NESS", "Advanced",
            "kindness darkness sadness madness fitness wellness illness stillness coolness " +
            "awareness readiness happiness loneliness wilderness forgiveness effectiveness " +
            "consciousness completeness correctness righteousness worthiness emptiness " +
            "awkwardness carelessness meaninglessness thoughtfulness purposefulness " +
            "distinctiveness attractiveness comprehensiveness innovativeness responsiveness",
            "NESS words tend to be abstract nouns describing states or qualities. " +
            "N-E-S-S: Right index, left middle, left ring, left ring (double S). " +
            "The double-S at the end is a common stumble point — practice 'ss' separately: " +
            "press press less miss bass grass class. Build the habit of hitting the same key twice " +
            "with a smooth repeated motion, not two hesitant taps.",
            true, 19, 55
        ));

        lessons.add(new Lesson(
            "lesson_020", "Words Ending in -FUL", "Advanced",
            "helpful useful harmful careful hopeful fearful grateful powerful colorful wonderful " +
            "beautiful peaceful thankful faithful mindful joyful successful meaningful cheerful " +
            "resourceful respectful thoughtful youthful graceful skillful truthful pitiful " +
            "dreadful handful bashful shameful spiteful tasteful wasteful disgraceful ungrateful",
            "FUL endings: F(left index), U(right index), L(right ring). This is a strong hand-alternation " +
            "pattern — left index then full right hand movement. The word 'beautiful' is one of the " +
            "most frequently mistyped words in English due to the BEAU cluster (B-E-A-U alternates " +
            "hands rapidly). Practice: beau beau beau beau — then add TIFUL to complete the word.",
            true, 20, 60
        ));

        return lessons;
    }
}