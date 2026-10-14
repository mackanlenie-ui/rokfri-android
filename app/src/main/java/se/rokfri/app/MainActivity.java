
package se.rokfri.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int green = Color.rgb(28, 120, 86);
    private final int dark = Color.rgb(28, 43, 38);
    private final int muted = Color.rgb(102, 119, 110);
    private final int background = Color.rgb(245, 248, 245);

    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView countText, goalText, timerText, statusText;
    private int goal = 15;
    private final Handler handler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("rokfri_data", MODE_PRIVATE);
        goal = prefs.getInt("goal", 15);

        showHome();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }

    private int getCount() {
        return prefs.getInt("count_" + today(), 0);
    }

    private void setCount(int count) {
        prefs.edit().putInt("count_" + today(), Math.max(0, count)).apply();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(30));
        root.setBackgroundColor(background);

        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("🌿 Rökfri", 30, green, true);
        root.addView(brand);

        TextView subtitle = text(
                "Små steg. En dag i taget.", 15, muted, false);
        subtitle.setPadding(0, dp(4), 0, dp(22));
        root.addView(subtitle);

        LinearLayout counterCard = card();
        root.addView(counterCard);

        counterCard.addView(text("DAGENS CIGARETTER", 12, muted, true));

        countText = text(String.valueOf(getCount()), 58, dark, true);
        countText.setGravity(Gravity.CENTER);
        counterCard.addView(countText);

        goalText = text("", 15, muted, false);
        goalText.setGravity(Gravity.CENTER);
        goalText.setPadding(0, 0, 0, dp(12));
        counterCard.addView(goalText);

        Button add = button("+ Registrera en cigarett", green);
        counterCard.addView(add);
        add.setOnClickListener(v -> {
            setCount(getCount() + 1);
            updateCount();
            prefs.edit().putLong("last_cigarette", System.currentTimeMillis())
                    .apply();
            updateTimer();
        });

        Button undo = button("Ångra senaste registreringen",
                Color.rgb(232, 239, 234));
        undo.setTextColor(dark);
        counterCard.addView(undo);
        undo.setOnClickListener(v -> {
            if (getCount() > 0) {
                setCount(getCount() - 1);
                updateCount();
            }
        });

        LinearLayout timerCard = card();
        timerCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.addView(timerCard);

        timerCard.addView(text("⏱  TID SEDAN SENASTE CIGARETTEN",
                12, muted, true));

        timerText = text("Inte registrerad ännu", 22, green, true);
        timerText.setPadding(0, dp(10), 0, dp(4));
        timerCard.addView(timerText);

        TextView timerHint = text(
                "Varje rökfri minut räknas.", 14, muted, false);
        timerCard.addView(timerHint);

        Button goalButton = button("🎯  Ändra dagens mål", green);
        root.addView(goalButton);
        goalButton.setOnClickListener(v -> chooseGoal());

        Button cravingButton = button("💚  Jag är röksugen", 
                Color.rgb(220, 239, 227));
        cravingButton.setTextColor(dark);
        root.addView(cravingButton);
        cravingButton.setOnClickListener(v -> showCravingHelp());

        Button statsButton = button("📊  Min utveckling", 
                Color.rgb(232, 239, 234));
        statsButton.setTextColor(dark);
        root.addView(statsButton);
        statsButton.setOnClickListener(v -> showStats());

        statusText = text("", 14, muted, false);
        statusText.setPadding(0, dp(14), 0, dp(6));
        root.addView(statusText);

        TextView footer = text(
                "Du behöver inte vara perfekt. Fortsätt bara framåt. 🌱",
                14, muted, false);
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        updateCount();
        updateTimer();
        updateStatus();
    }

    private void updateCount() {
        int count = getCount();
        countText.setText(String.valueOf(count));
        goalText.setText("Dagens mål: " + goal + " cigaretter");

        if (count <= goal) {
            goalText.setText(goalText.getText() +
                    " · " + (goal - count) + " kvar till målet");
        } else {
            goalText.setText(goalText.getText() +
                    " · " + (count - goal) + " över målet");
        }
        updateStatus();
    }

    private void updateStatus() {
        if (statusText == null) return;
        int count = getCount();
        if (count < goal) {
            statusText.setText("Bra jobbat! Du ligger under ditt dagsmål.");
        } else if (count == goal) {
            statusText.setText("Du har nått dagens mål. Bra jobbat!");
        } else {
            statusText.setText(
                    "Dagen är inte förstörd. Nästa val är en ny chans.");
        }
    }

    private void updateTimer() {
        if (timerText == null) return;
        long last = prefs.getLong("last_cigarette", 0);

        if (last == 0) {
            timerText.setText("Registrera första cigaretten");
            return;
        }

        long seconds = Math.max(0, (System.currentTimeMillis() - last) / 1000);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours >= 24) {
            long days = hours / 24;
            timerText.setText(days + " d " + (hours % 24) + " tim");
        } else if (hours > 0) {
            timerText.setText(hours + " tim " + minutes + " min");
        } else if (minutes > 0) {
            timerText.setText(minutes + " min " + secs + " sek");
        } else {
            timerText.setText(secs + " sek");
        }
    }

    private void chooseGoal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(8), dp(24), dp(8));

        TextView current = text("Välj hur många cigaretter du siktar på idag.",
                15, dark, false);
        layout.addView(current);

        TextView value = text(goal + " cigaretter", 26, green, true);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, dp(16), 0, dp(8));
        layout.addView(value);

        SeekBar seek = new SeekBar(this);
        seek.setMax(30);
        seek.setProgress(goal);
        layout.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress,
                                          boolean fromUser) {
                int selected = Math.max(1, progress);
                value.setText(selected + " cigaretter");
            }
            @Override
            public void onStartTrackingTouch(SeekBar bar) {}
            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("Ditt dagsmål")
                .setView(layout)
                .setNegativeButton("Avbryt", null)
                .setPositiveButton("Spara", (dialog, which) -> {
                    goal = Math.max(1, seek.getProgress());
                    prefs.edit().putInt("goal", goal).apply();
                    updateCount();
                })
                .show();
    }

    private void showCravingHelp() {
        String[] tips = {
                "Vänta 5 minuter. Röksug brukar komma i vågor och kan avta.",
                "Ta ett glas vatten och andas långsamt tio gånger.",
                "Gå en kort promenad eller byt rum en stund.",
                "Känner du dig ensam eller nere? Skicka ett enkelt meddelande till någon.",
                "Påminn dig själv: jag behöver inte bestämma resten av dagen nu."
        };

        new AlertDialog.Builder(this)
                .setTitle("💚 Ta dig igenom röksuget")
                .setItems(tips, (dialog, which) -> new AlertDialog.Builder(this)
                        .setTitle("Prova detta nu")
                        .setMessage(tips[which])
                        .setPositiveButton("Okej", null)
                        .show())
                .setPositiveButton("Stäng", null)
                .show();
    }

    private void showStats() {
        StringBuilder report = new StringBuilder();
        report.append("Dina senaste 7 dagar\n\n");
        int total = 0;

        for (int i = 0; i < 7; i++) {
            long dayMillis = System.currentTimeMillis()
                    - i * 24L * 60L * 60L * 1000L;
            String key = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(new Date(dayMillis));
            int count = prefs.getInt("count_" + key, 0);
            total += count;

            String label = i == 0 ? "Idag" :
                    new SimpleDateFormat("EEE dd/MM",
                            Locale.getDefault()).format(new Date(dayMillis));
            report.append(label).append(": ").append(count)
                    .append(" cigaretter\n");
        }

        report.append("\nTotalt: ").append(total)
                .append(" cigaretter under perioden.")
                .append("\n\nSiffrorna hjälper dig se mönster, inte döma dig själv.");

        new AlertDialog.Builder(this)
                .setTitle("📊 Min utveckling")
                .setMessage(report.toString())
                .setPositiveButton("Stäng", null)
                .show();
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.WHITE);
        shape.setCornerRadius(dp(22));
        box.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(16);
        root.addView(box, params);
        return box;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(16));
        b.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(7);
        params.bottomMargin = dp(4);
        b.setLayoutParams(params);
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(timerRunnable);
        handler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(timerRunnable);
    }
}
