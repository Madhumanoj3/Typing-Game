package ui;

import db.MongoDBManager;
import game.ThemeManager;
import javafx.geometry.*;
import service.StoreService;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.User;
import util.PasswordUtil;
import util.SessionManager;

/**
 * Login and Registration screen.
 * Toggles between the two forms in the same scene.
 */
public class LoginScreen {

    // ── State ─────────────────────────────────────────────────────────────
    private boolean showingLogin = true;

    // ── Login fields ──────────────────────────────────────────────────────
    private TextField     loginEmail;
    private PasswordField loginPassword;

    // ── Register fields ───────────────────────────────────────────────────
    private TextField     regUsername;
    private TextField     regEmail;
    private TextField     regPhone;
    private TextField     regAddress;
    private TextField     regAge;
    private DatePicker    regDob;
    private PasswordField regPassword;
    private PasswordField regConfirm;
    private HBox          strengthBar;

    // ── Root panes ────────────────────────────────────────────────────────
    private VBox loginForm;
    private VBox registerForm;
    private VBox formContainer;
    private Button themeToggleButton;

    public Scene buildScene() {
        // ── Background ────────────────────────────────────────────────────
        StackPane root = new StackPane();

        // Decorative gradient blobs (behind the card)
        Pane blobs = buildBlobs();

        // ── Center Card ───────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(600);
        card.setMinWidth(350);
        card.setPrefWidth(440);
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-radius: 20; -fx-padding: 40;");

        buildLoginForm();
        buildRegisterForm();

        formContainer = new VBox();
        formContainer.setPrefHeight(-1); // Let it size based on content
        formContainer.setStyle("-fx-padding: 0;");
        formContainer.getChildren().add(loginForm);

        card.getChildren().add(formContainer);

        themeToggleButton = buildThemeToggleButton();

        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setAlignment(themeToggleButton, Pos.TOP_RIGHT);
        StackPane.setMargin(themeToggleButton, new Insets(24));
        root.getChildren().addAll(blobs, card, themeToggleButton);

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    // ── Login Form ────────────────────────────────────────────────────────

    private void buildLoginForm() {
        loginForm = new VBox(18);
        loginForm.setAlignment(Pos.TOP_CENTER);
        loginForm.setMaxWidth(Double.MAX_VALUE);
        loginForm.setPrefHeight(-1);

        // Brand
        Label brand = new Label("⌨  TypeMaster");
        brand.getStyleClass().add("label-accent");
        brand.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label title = new Label("Welcome back");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Sign in to continue your journey");
        sub.getStyleClass().add("label-muted");

        // Fields
        loginEmail = new TextField();
        loginEmail.setPromptText("Email address");
        loginEmail.getStyleClass().add("field-dark");
        loginEmail.setMaxWidth(Double.MAX_VALUE);

        loginPassword = new PasswordField();
        loginPassword.setPromptText("Password");
        loginPassword.getStyleClass().add("field-dark");
        loginPassword.setMaxWidth(Double.MAX_VALUE);

        // Login button
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin());

        // Enter key support
        loginPassword.setOnAction(e -> handleLogin());

        // Switch to register
        HBox switchRow = new HBox(6);
        switchRow.setAlignment(Pos.CENTER);
        Label switchLabel = new Label("Don't have an account?");
        switchLabel.getStyleClass().add("label-muted");
        Button switchBtn = new Button("Register");
        switchBtn.getStyleClass().add("btn-secondary");
        switchBtn.setPadding(new Insets(6, 16, 6, 16));
        switchBtn.setOnAction(e -> toggleForm());
        switchRow.getChildren().addAll(switchLabel, switchBtn);

        loginForm.getChildren().addAll(
                brand, title, sub,
                gap(4),
                fieldLabel("Email"), loginEmail,
                fieldLabel("Password"), loginPassword,
                loginBtn,
                gap(8), switchRow
        );
    }

    // ── Register Form ─────────────────────────────────────────────────────

    private void buildRegisterForm() {
        registerForm = new VBox(14);
        registerForm.setAlignment(Pos.TOP_CENTER);
        registerForm.setMaxWidth(Double.MAX_VALUE);
        registerForm.setPrefHeight(-1);

        Label brand = new Label("⌨  TypeMaster");
        brand.getStyleClass().add("label-accent");
        brand.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label title = new Label("Create account");
        title.getStyleClass().add("label-title");

        Label sub = new Label("Start your typing journey today");
        sub.getStyleClass().add("label-muted");

        regUsername = new TextField();
        regUsername.setPromptText("Username");
        regUsername.getStyleClass().add("field-dark");
        regUsername.setMaxWidth(Double.MAX_VALUE);

        regEmail = new TextField();
        regEmail.setPromptText("Email address");
        regEmail.getStyleClass().add("field-dark");
        regEmail.setMaxWidth(Double.MAX_VALUE);

        regPhone = new TextField();
        regPhone.setPromptText("Phone Number");
        regPhone.getStyleClass().add("field-dark");
        regPhone.setMaxWidth(Double.MAX_VALUE);
        // Validation: Phone number - max 10 digits, only numbers
        regPhone.textProperty().addListener((obs, old, nv) -> {
            if (!nv.matches("\\d*")) {
                regPhone.setText(nv.replaceAll("[^0-9]", ""));
            } else if (nv.length() > 10) {
                regPhone.setText(nv.substring(0, 10));
            }
        });

        regAddress = new TextField();
        regAddress.setPromptText("Address");
        regAddress.getStyleClass().add("field-dark");
        regAddress.setMaxWidth(Double.MAX_VALUE);

        regAge = new TextField();
        regAge.setPromptText("Age");
        regAge.getStyleClass().add("field-dark");
        regAge.setMaxWidth(Double.MAX_VALUE);
        // Validation: Age - only numbers, no characters or symbols
        regAge.textProperty().addListener((obs, old, nv) -> {
            if (!nv.matches("\\d*")) {
                regAge.setText(nv.replaceAll("[^0-9]", ""));
            }
        });

        regDob = new DatePicker();
        regDob.setPromptText("Date of Birth");
        regDob.getStyleClass().add("field-dark");
        regDob.setMaxWidth(Double.MAX_VALUE);
        regDob.getEditor().getStyleClass().add("field-dark");

        regPassword = new PasswordField();
        regPassword.setPromptText("Password");
        regPassword.getStyleClass().add("field-dark");
        regPassword.setMaxWidth(Double.MAX_VALUE);

        // Password strength bar
        Label strengthLabel = new Label("Password strength");
        strengthLabel.getStyleClass().add("label-muted");
        strengthLabel.setStyle("-fx-font-size: 11px;");

        strengthBar = new HBox(4);
        strengthBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 3; i++) {
            Rectangle seg = new Rectangle(85, 5);
            seg.setArcWidth(4);
            seg.setArcHeight(4);
            seg.setFill(Color.web("#1e293b"));
            strengthBar.getChildren().add(seg);
        }
        regPassword.textProperty().addListener((obs, old, nv) -> updateStrengthBar(nv));

        regConfirm = new PasswordField();
        regConfirm.setPromptText("Confirm password");
        regConfirm.getStyleClass().add("field-dark");
        regConfirm.setMaxWidth(Double.MAX_VALUE);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> handleRegister());
        regConfirm.setOnAction(e -> handleRegister());

        HBox switchRow = new HBox(6);
        switchRow.setAlignment(Pos.CENTER);
        Label switchLabel = new Label("Already have an account?");
        switchLabel.getStyleClass().add("label-muted");
        Button switchBtn = new Button("Sign In");
        switchBtn.getStyleClass().add("btn-secondary");
        switchBtn.setPadding(new Insets(6, 16, 6, 16));
        switchBtn.setOnAction(e -> toggleForm());
        switchRow.getChildren().addAll(switchLabel, switchBtn);

        registerForm.getChildren().addAll(
                brand, title, sub,
                gap(4),
                fieldLabel("Username"), regUsername,
                fieldLabel("Email"), regEmail,
                fieldLabel("Phone"), regPhone,
                fieldLabel("Address"), regAddress,
                fieldLabel("Age"), regAge,
                fieldLabel("Date of Birth"), regDob,
                fieldLabel("Password"), regPassword,
                strengthLabel, strengthBar,
                fieldLabel("Confirm Password"), regConfirm,
                registerBtn,
                gap(4), switchRow
        );
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void handleLogin() {
        String email = loginEmail.getText().trim();
        String pass  = loginPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            AppDialogs.showError("Missing Fields", "Please enter your email and password.");
            return;
        }

        // Admin login — credentials loaded from config.properties
        String[] adminCreds = loadAdminCredentials();
        if (adminCreds[0].equals(email) && adminCreds[1].equals(pass)) {
            User adminUser = new User("", "", 0, "", "admin", email, "");
            SessionManager.getInstance().login(adminUser);
            MainUI.showAdminPanel();
            return;
        }

        String hash = PasswordUtil.hash(pass);
        User user = MongoDBManager.getInstance().loginUser(email, hash);
        if (user == null) {
            AppDialogs.showError("Login Failed", "Invalid email or password.\nPlease check your credentials and try again.");
        } else {
            if (user.isBlocked()) {
                AppDialogs.showError("Account Blocked", "Your account has been blocked.\nPlease contact the administrator.");
                return;
            }
            SessionManager.getInstance().login(user);
            StoreService.getInstance().loadUserPreferences(user.getUsername());
            MainUI.showVideoIntro();
        }
    }

    private void handleRegister() {
        String username  = regUsername.getText().trim();
        String email     = regEmail.getText().trim();
        String phone     = regPhone.getText().trim();
        String address   = regAddress.getText().trim();
        String ageStr    = regAge.getText().trim();
        String dob       = (regDob.getValue() != null) ? regDob.getValue().toString() : "";
        String pass      = regPassword.getText();
        String confirm   = regConfirm.getText();

        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() ||
            ageStr.isEmpty() || dob.isEmpty() || pass.isEmpty()) {
            AppDialogs.showError("Missing Fields", "Please fill in all required fields before creating your account.");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            AppDialogs.showError("Invalid Phone Number", "Phone number must be exactly 10 digits.\nExample: 9876543210");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 5 || age > 100) {
                AppDialogs.showError("Invalid Age", "Age must be between 5 and 100 years old.");
                return;
            }
        } catch (NumberFormatException e) {
            AppDialogs.showError("Invalid Age", "Please enter a valid age between 5 and 100.");
            return;
        }
        if (!email.contains("@")) {
            AppDialogs.showError("Invalid Email", "Please enter a valid email address.\nExample: user@example.com");
            return;
        }
        if (!pass.equals(confirm)) {
            AppDialogs.showError("Passwords Don't Match", "The passwords you entered do not match.\nPlease re-enter and try again.");
            return;
        }
        if (pass.length() < 6) {
            AppDialogs.showError("Weak Password", "Password must be at least 6 characters long.\nChoose a stronger password.");
            return;
        }

        if (MongoDBManager.getInstance().isEmailTaken(email)) {
            AppDialogs.showError("Email Already Registered", "This email address is already in use.\nTry signing in or use a different email.");
            return;
        }
        if (MongoDBManager.getInstance().isUsernameTaken(username)) {
            AppDialogs.showError("Username Taken", "The username \"" + username + "\" is already taken.\nPlease choose a different username.");
            return;
        }

        User newUser = new User(phone, address, age, dob, username, email, PasswordUtil.hash(pass));
        boolean ok = MongoDBManager.getInstance().registerUser(newUser);
        if (!ok) {
            AppDialogs.showError("Registration Failed", "Something went wrong while creating your account.\nPlease try again.");
        } else {
            SessionManager.getInstance().login(newUser);
            StoreService.getInstance().loadUserPreferences(newUser.getUsername());
            MainUI.showVideoIntro();
        }
    }

    private void toggleForm() {
        showingLogin = !showingLogin;
        formContainer.getChildren().clear();
        if (showingLogin) {
            formContainer.getChildren().add(loginForm);
        } else {
            ScrollPane sp = new ScrollPane(registerForm);
            sp.setFitToWidth(true);
            sp.setPrefHeight(-1); // Let it size naturally
            sp.setMaxHeight(Double.MAX_VALUE);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.getStyleClass().add("scroll-dark");
            formContainer.getChildren().add(sp);
        }
    }

    private void updateStrengthBar(String password) {
        int score = PasswordUtil.strength(password);
        String[] styles = { "#1e293b", "#ef4444", "#f59e0b", "#10b981" };
        for (int i = 0; i < strengthBar.getChildren().size(); i++) {
            Rectangle seg = (Rectangle) strengthBar.getChildren().get(i);
            seg.setFill(Color.web(i < score ? styles[score] : "#1e293b"));
        }
    }

    /** Returns [adminEmail, adminPassword] from config.properties (fallback to defaults). */
    private String[] loadAdminCredentials() {
        java.util.Properties p = new java.util.Properties();
        try (java.io.InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) p.load(is);
        } catch (java.io.IOException ignored) { }
        return new String[]{
            p.getProperty("admin.email",    "admin@typing.com"),
            p.getProperty("admin.password", "admin123")
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("label-muted");
        l.setStyle("-fx-font-size: 12px; -fx-padding: 0 0 2 0;");
        return l;
    }

    private Region gap(int height) {
        Region r = new Region();
        r.setMinHeight(height);
        return r;
    }

    private Button buildThemeToggleButton() {
        Button btn = new Button();
        btn.getStyleClass().add("theme-toggle-btn");
        btn.setMinSize(44, 44);
        btn.setPrefSize(44, 44);
        btn.setMaxSize(44, 44);
        updateThemeToggleButton(btn);
        btn.setOnAction(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            if (tm.isPrintLightTheme()) {
                tm.restoreLastNonLightTheme();
            } else {
                tm.setPrintLightTheme();
            }
            updateThemeToggleButton(btn);
            ThemeManager.applyTheme(btn.getScene());
        });
        return btn;
    }

    private void updateThemeToggleButton(Button btn) {
        boolean light = ThemeManager.getInstance().isPrintLightTheme();
        btn.setText(light ? "☾" : "☀");
        btn.setTooltip(new Tooltip(light ? "Switch to dark theme" : "Switch to light print theme"));
    }

    private Pane buildBlobs() {
        Pane p = new Pane();
        p.setMouseTransparent(true);

        // Purple blob top-left
        Region b1 = new Region();
        b1.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 50%, #7c3aed44, transparent); " +
                    "-fx-background-radius: 300;");
        b1.getStyleClass().add("login-blob");
        b1.setPrefSize(500, 500);
        b1.setLayoutX(-100);
        b1.setLayoutY(-100);

        // Cyan blob bottom-right
        Region b2 = new Region();
        b2.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 50%, #06b6d422, transparent); " +
                    "-fx-background-radius: 300;");
        b2.getStyleClass().add("login-blob");
        b2.setPrefSize(400, 400);
        b2.setLayoutX(800);
        b2.setLayoutY(400);

        p.getChildren().addAll(b1, b2);
        return p;
    }
}
