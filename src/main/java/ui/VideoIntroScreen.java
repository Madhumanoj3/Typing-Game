package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;

/**
 * Plays a welcome video once after a user's first login,
 * then navigates to the Dashboard.
 */
public class VideoIntroScreen {

    private Object mediaPlayer; // Use Object to avoid class-load failure if javafx.media is missing

    public Scene buildScene() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        boolean videoLoaded = false;

        try {
            // Check if the resource exists
            URL videoUrl = getClass().getResource("/entry_video.mp4");
            if (videoUrl == null) {
                System.err.println("[VideoIntro] entry_video.mp4 not found in resources!");
            } else {
                String videoPath = videoUrl.toExternalForm();
                System.out.println("[VideoIntro] Loading video from: " + videoPath);

                // Use reflection-free direct import — class loads only if javafx.media module is present
                javafx.scene.media.Media media = new javafx.scene.media.Media(videoPath);
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                this.mediaPlayer = player;

                javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(player);
                mediaView.setPreserveRatio(true);

                // Bind video size to window
                mediaView.fitWidthProperty().bind(root.widthProperty());
                mediaView.fitHeightProperty().bind(root.heightProperty());

                root.getChildren().add(mediaView);

                // Listen for status changes to debug
                player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                    System.out.println("[VideoIntro] MediaPlayer status: " + oldStatus + " -> " + newStatus);
                });

                // Auto-advance when video ends
                player.setOnEndOfMedia(() -> {
                    System.out.println("[VideoIntro] Video ended, navigating to dashboard.");
                    Platform.runLater(this::finishVideo);
                });

                // Handle media errors — DON'T auto-skip, just log
                media.setOnError(() -> {
                    System.err.println("[VideoIntro] Media error: " + media.getError());
                });

                player.setOnError(() -> {
                    System.err.println("[VideoIntro] MediaPlayer error: " + player.getError());
                    // Don't auto-skip on error — let user use Skip button
                });

                player.setOnReady(() -> {
                    System.out.println("[VideoIntro] Video ready! Duration: " + player.getTotalDuration());
                    player.play();
                });

                // Try auto-play as well
                player.setAutoPlay(true);
                videoLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("[VideoIntro] Failed to initialize video: " + e.getMessage());
            e.printStackTrace();
        }

        // ── Skip button (always visible as fallback) ────────────────────
        Button skipBtn = new Button("Skip ▸");
        String baseStyle =
            "-fx-background-color: rgba(255,255,255,0.2);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-padding: 10 28 10 28; -fx-background-radius: 22;" +
            "-fx-cursor: hand;";
        String hoverStyle =
            "-fx-background-color: rgba(255,255,255,0.4);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-padding: 10 28 10 28; -fx-background-radius: 22;" +
            "-fx-cursor: hand;";
        skipBtn.setStyle(baseStyle);
        skipBtn.setOnMouseEntered(e -> skipBtn.setStyle(hoverStyle));
        skipBtn.setOnMouseExited(e -> skipBtn.setStyle(baseStyle));
        skipBtn.setOnAction(e -> finishVideo());

        StackPane.setAlignment(skipBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(skipBtn, new Insets(0, 30, 30, 0));

        // If video loaded, fade skip button in after 2 seconds
        if (videoLoaded) {
            skipBtn.setOpacity(0);
            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(e -> {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), skipBtn);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            delay.play();
        }

        // If video failed to load, show a message and the skip button immediately
        if (!videoLoaded) {
            Label msg = new Label("Welcome to TypeMaster! 🎉");
            msg.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
            root.getChildren().add(msg);
            // Auto-navigate after 3 seconds
            PauseTransition autoSkip = new PauseTransition(Duration.seconds(3));
            autoSkip.setOnFinished(e -> finishVideo());
            autoSkip.play();
        }

        root.getChildren().add(skipBtn);

        Scene scene = new Scene(root, 1200, 750);
        scene.setFill(Color.BLACK);
        return scene;
    }

    private void finishVideo() {
        if (mediaPlayer != null) {
            try {
                javafx.scene.media.MediaPlayer player = (javafx.scene.media.MediaPlayer) mediaPlayer;
                player.stop();
                player.dispose();
            } catch (Exception e) {
                System.err.println("[VideoIntro] Error disposing media player: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        Platform.runLater(MainUI::showDashboard);
    }
}
