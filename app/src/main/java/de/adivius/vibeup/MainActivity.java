package de.adivius.vibeup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Constants for game logic
    private static final int TOLERANCE = 100;
    private static final int[] PATTERN_SIZES = {2, 3, 4};
    private static final int[] VIBRATION_DURATIONS = {250, 500, 750, 1000};

    // Enum for game modes
    enum MODES {START, PLAYING, WAITING_FOR_INPUT, RECORDING_INPUT}

    private Vibrator deviceVibrator;
    private MODES currentMode = MODES.START;
    private int touchIndex = 0;
    private long lastTouchTime = 0;
    private int patternSize = 0;
    private long[] targetPattern;
    private long[] userInputPattern;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(new BlackScreenView(this));
        hideSystemUI();

        deviceVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        new AlertDialog.Builder(this, R.style.TutorialDialog).setView(getLayoutInflater().inflate(R.layout.tutorial, null)).create().show();
    }

    // Starts the game by generating a random pattern and playing it
    private void startGame() {
        patternSize = PATTERN_SIZES[randomInt(0, PATTERN_SIZES.length - 1)];
        targetPattern = generateRandomVibrationPattern(patternSize);
        new Handler().postDelayed(() -> playVibrationPattern(targetPattern), 500);
    }

    public int randomInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    // Generates a random vibration pattern with the given size
    private long[] generateRandomVibrationPattern(int size) {
        long[] randomPattern = new long[size];
        randomPattern[0] = 0;
        for (int i = 1; i < size; i++) {
            randomPattern[i] = VIBRATION_DURATIONS[randomInt(0, VIBRATION_DURATIONS.length - 1)];
        }
        return randomPattern;
    }

    // Plays the given vibration pattern
    private void playVibrationPattern(long[] pattern) {
        new Thread(() -> {
            for (long vibrationDuration : pattern) {
                delay(vibrationDuration);
                deviceVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            }
            delay(500);
            currentMode = MODES.WAITING_FOR_INPUT;
        }).start();
    }

    // Delays the thread for a specified time
    private void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Compares system-generated and user-input patterns to determine a win
    private boolean isUserInputCorrect(long[] systemPattern, long[] userPattern) {
        for (int i = 0; i < patternSize; i++) {
            if (Math.abs(systemPattern[i] - userPattern[i]) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    // Handles touch events
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            handleTouchInput();
        }
        return true;
    }

    // Handles the touch input based on the current game mode
    private void handleTouchInput() {
        switch (currentMode) {
            case START:
                currentMode = MODES.PLAYING;
                startGame();
                break;

            case WAITING_FOR_INPUT:
                prepareForUserInput();
                currentMode = MODES.RECORDING_INPUT;
                break;

            case RECORDING_INPUT:
                recordUserTouch();
                break;
        }
    }

    // Prepares for user input by resetting variables
    private void prepareForUserInput() {
        deviceVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        userInputPattern = new long[patternSize];
        touchIndex = 0;
        userInputPattern[touchIndex] = 0;
        lastTouchTime = System.currentTimeMillis();
    }

    // Records each user touch and checks for win condition
    private void recordUserTouch() {
        if (touchIndex + 1 >= patternSize) {
            return;
        }
        deviceVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        touchIndex++;
        userInputPattern[touchIndex] = System.currentTimeMillis() - lastTouchTime;
        lastTouchTime = System.currentTimeMillis();

        if (touchIndex == patternSize - 1) {
            evaluateUserInput();
        }
    }

    // Evaluates user input and gives feedback on success or failure
    private void evaluateUserInput() {
        if (isUserInputCorrect(targetPattern, userInputPattern)) {
            new Handler().postDelayed(() -> deviceVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)), 750);
        } else {
            new Handler().postDelayed(() -> deviceVibrator.vibrate(VibrationEffect.createOneShot(750, 25)), 750);
        }
        new Handler().postDelayed(() -> currentMode = MODES.START, 1000);

    }

    // Hides the system UI
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    // Handles window focus changes
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    // Black screen view for the game interface
    public static class BlackScreenView extends View {
        public BlackScreenView(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
        }
    }

}
