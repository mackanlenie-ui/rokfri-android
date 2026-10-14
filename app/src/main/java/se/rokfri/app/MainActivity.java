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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.text.InputType;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
    private TextView countText, goalText, timerText, statusText, savingsText, savingsHint;
    private int goal = 15;
    private final Handler handler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            updateTimer();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("rokfri_data", MODE_PRIVATE);
        goal = prefs.getInt("goal", 15);
        showHome();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private int getCount() { return prefs.getInt("count_" + today(), 0); }

    private void setCount(int count) {
        prefs.edit().putInt("count_" + today(), Math.max(0, count)).apply();
    }

    private double packPrice() { return Double.longBitsToDouble(prefs.getLong("pack_price_bits", Double.doubleToLongBits(80.0))); }
    private int cigarettesPerPack() { return Math.max(1, prefs.getInt("cigs_per_pack", 20)); }
    private int baseline() { return Math.max(0, prefs.getInt("baseline_daily", 15)); }
    private double pricePerCigarette() { return packPrice() / cigarettesPerPack(); }

    private String money(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("sv", "SE"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(Math.max(0, value)) + " kr";
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
        TextView subtitle = text("Små steg. En dag i taget.", 15, muted, false);
        subtitle.setPadding(0, dp(4), 0, dp(22));
        root.addView(subtitle);

        LinearLayout counterCard = card();
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
            prefs.edit().putLong("last_cigarette", System.currentTimeMillis()).apply();
            updateCount();
            updateTimer();
        });

        Button undo = button("Ångra senaste registreringen", Color.rgb(232, 239, 234));
        undo.setTextColor(dark);
        counterCard.addView(undo);
        undo.setOnClickListener(v -> {
            if (getCount() > 0) {
                setCount(getCount() - 1);
                updateCount();
            }
        });

        LinearLayout timerCard = card();
        timerCard.addView(text("⏱  TID SEDAN SENASTE CIGARETTEN", 12, muted, true));
        timerText = text("Inte registrerad ännu", 22, green, true);
        timerText.setPadding(0, dp(10), 0, dp(4));
        timerCard.addView(timerText);
        timerCard.addView(text("Varje rökfri minut räknas.", 14, muted, false));

        LinearLayout savingsCard = card();
        savingsCard.addView(text("💰 MÖJLIG BESPARING IDAG", 12, muted, true));
        savingsText = text("0 kr", 30, green, true);
        savingsText.setPadding(0, dp(8), 0, dp(3));
        savingsCard.addView(savingsText);
        savingsHint = text("Jämfört med din vanliga konsumtion.", 13, muted, false);
        savingsCard.addView(savingsHint);

        Button goalButton = button("🎯  Ändra dagens mål", green);
        root.addView(goalButton);
        goalButton.setOnClickListener(v -> chooseGoal());

        Button cravingButton = button("💚  Jag är röksugen", Color.rgb(220, 239, 227));
        cravingButton.setTextColor(dark);
        root.addView(cravingButton);
        cravingButton.setOnClickListener(v -> showCravingHelp());

        Button statsButton = button("📊  Min utveckling", Color.rgb(232, 239, 234));
        statsButton.setTextColor(dark);
        root.addView(statsButton);
        statsButton.setOnClickListener(v -> showStats());

        Button moneyButton = button("⚙️  Ekonomi och inställningar", Color.rgb(232, 239, 234));
        moneyButton.setTextColor(dark);
        root.addView(moneyButton);
        moneyButton.setOnClickListener(v -> showMoneySettings());

        statusText = text("", 14, muted, false);
        statusText.setPadding(0, dp(14), 0, dp(6));
        root.addView(statusText);
        TextView footer = text("Du behöver inte vara perfekt. Fortsätt bara framåt. 🌱", 14, muted, false);
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        updateCount();
        updateTimer();
        updateStatus();
    }

    private void updateCount() {
        if (countText == null) return;
        int count = getCount();
        countText.setText(String.valueOf(count));
        String target = "Dagens mål: " + goal + " cigaretter";
        if (count <= goal) target += " · " + (goal - count) + " kvar till målet";
        else target += " · " + (count - goal) + " över målet";
        goalText.setText(target);
        updateSavings();
        updateStatus();
    }

    private void updateSavings() {
        if (savingsText == null) return;
        int reduced = Math.max(0, baseline() - getCount());
        double amount = reduced * pricePerCigarette();
        savingsText.setText(money(amount));
        if (reduced == 0) {
            savingsHint.setText("Ingen minskning mot din vanliga nivå ännu. Varje nytt val räknas.");
        } else {
            savingsHint.setText(reduced + " färre cigaretter än din vanliga nivå (" + money(pricePerCigarette()) + " per cigarett).");
        }
    }

    private void updateStatus() {
        if (statusText == null) return;
        int count = getCount();
        if (count < goal) statusText.setText("Bra jobbat! Du ligger under ditt dagsmål.");
        else if (count == goal) statusText.setText("Du har nått dagens mål. Bra jobbat!");
        else statusText.setText("Dagen är inte förstörd. Nästa val är en ny chans.");
    }

    private void updateTimer() {
        if (timerText == null) return;
        long last = prefs.getLong("last_cigarette", 0);
        if (last == 0) { timerText.setText("Registrera första cigaretten"); return; }
        long seconds = Math.max(0, (System.currentTimeMillis() - last) / 1000);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours >= 24) timerText.setText((hours / 24) + " d " + (hours % 24) + " tim");
        else if (hours > 0) timerText.setText(hours + " tim " + minutes + " min");
        else if (minutes > 0) timerText.setText(minutes + " min " + secs + " sek");
        else timerText.setText(secs + " sek");
    }

    private void chooseGoal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(8), dp(24), dp(8));
        layout.addView(text("Välj hur många cigaretter du siktar på idag.", 15, dark, false));
        TextView value = text(goal + " cigaretter", 26, green, true);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, dp(16), 0, dp(8));
        layout.addView(value);
        SeekBar seek = new SeekBar(this);
        seek.setMax(30);
        seek.setProgress(goal);
        layout.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { value.setText(Math.max(1, progress) + " cigaretter"); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        new AlertDialog.Builder(this).setTitle("Ditt dagsmål").setView(layout)
                .setNegativeButton("Avbryt", null)
                .setPositiveButton("Spara", (dialog, which) -> {
                    goal = Math.max(1, seek.getProgress());
                    prefs.edit().putInt("goal", goal).apply();
                    updateCount();
                }).show();
    }

    private EditText numberInput(String value, boolean decimal) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        return input;
    }

    private void showMoneySettings() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(4), dp(22), dp(4));

        layout.addView(text("Alla belopp visas i svenska kronor (SEK). Ändra siffrorna så de stämmer för dig.", 14, muted, false));
        layout.addView(text("Pris per cigarettpaket (kr)", 14, dark, true));
        EditText price = numberInput(String.format(Locale.US, "%.2f", packPrice()), true);
        layout.addView(price);
        layout.addView(text("Cigaretter per paket", 14, dark, true));
        EditText packSize = numberInput(String.valueOf(cigarettesPerPack()), false);
        layout.addView(packSize);
        layout.addView(text("Din vanliga konsumtion per dag", 14, dark, true));
        EditText normal = numberInput(String.valueOf(baseline()), false);
        layout.addView(normal);
        layout.addView(text("Standardvärdet är 80 kr per paket och 20 cigaretter. Besparingen är en uppskattning jämfört med din valda vanliga nivå.", 12, muted, false));

        new AlertDialog.Builder(this).setTitle("💰 Ekonomi")
                .setView(layout).setNegativeButton("Avbryt", null)
                .setPositiveButton("Spara", (dialog, which) -> {
                    try {
                        String p = price.getText().toString().trim().replace(',', '.');
                        double priceValue = Double.parseDouble(p);
                        int sizeValue = Integer.parseInt(packSize.getText().toString().trim());
                        int baselineValue = Integer.parseInt(normal.getText().toString().trim());
                        if (priceValue >= 0 && priceValue <= 10000 && sizeValue >= 1 && sizeValue <= 100 && baselineValue >= 0 && baselineValue <= 200) {
                            prefs.edit()
                                .putLong("pack_price_bits", Double.doubleToLongBits(priceValue))
                                .putInt("cigs_per_pack", sizeValue)
                                .putInt("baseline_daily", baselineValue)
                                .apply();
                            updateSavings();
                        } else {
                            new AlertDialog.Builder(this).setTitle("Kontrollera värdena")
                                .setMessage("Ange ett rimligt paketpris, 1–100 cigaretter per paket och 0–200 cigaretter som vanlig dagsnivå.")
                                .setPositiveButton("Okej", null).show();
                        }
                    } catch (Exception ex) {
                        new AlertDialog.Builder(this).setTitle("Kontrollera värdena")
                            .setMessage("Skriv siffror i alla fält, till exempel 79,90 för paketpriset.")
                            .setPositiveButton("Okej", null).show();
                    }
                }).show();
    }

    private void showCravingHelp() {
        String[] tips = {
            "Vänta en stund. Röksug kommer ofta i vågor och kan klinga av.",
            "Drick ett glas vatten och ta tio långsamma andetag.",
            "Byt miljö: gå en kort promenad eller lämna rummet.",
            "Gör något med händerna, till exempel diska, rita eller håll i en penna.",
            "Du behöver inte bestämma över resten av dagen. Ta bara nästa fem minuter."
        };
        new AlertDialog.Builder(this).setTitle("💚 Ta dig igenom röksuget")
            .setMessage("Välj ett tips eller starta en femminuterspaus. Du behöver inte kämpa med hela dagen på en gång.")
            .setItems(tips, (dialog, which) -> new AlertDialog.Builder(this)
                .setTitle("Prova detta nu").setMessage(tips[which])
                .setPositiveButton("Okej", null).show())
            .setNeutralButton("Starta 5 min paus", (dialog, which) -> startCravingTimer())
            .setPositiveButton("Stäng", null).show();
    }

    private void startCravingTimer() {
        final TextView countdown = text("05:00", 42, green, true);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(dp(20), dp(20), dp(20), dp(12));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(8));
        layout.addView(countdown);
        TextView prompt = text("Lägg undan cigaretterna en stund. Andas lugnt, drick lite vatten och låt suget passera utan att behöva agera på det.", 16, dark, false);
        layout.addView(prompt);
        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🌿 Fem minuter för dig")
            .setView(layout)
            .setPositiveButton("Avsluta", null)
            .create();
        final long endAt = System.currentTimeMillis() + 5 * 60 * 1000L;
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            long remaining = Math.max(0, (endAt - System.currentTimeMillis() + 999) / 1000);
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            countdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            if (remaining == 0) {
                countdown.setText("Klart! 🌱");
                prompt.setText("Bra jobbat med att ge dig själv fem minuter. Känn efter hur du mår nu — varje paus är en vinst.");
            } else if (dialog.isShowing()) {
                handler.postDelayed(tick[0], 1000);
            }
        };
        dialog.setOnDismissListener(d -> handler.removeCallbacks(tick[0]));
        dialog.setOnShowListener(d -> handler.post(tick[0]));
        dialog.show();
    }

    private void showStats() {
        StringBuilder report = new StringBuilder("Dina senaste 7 dagar\n\n");
        int total = 0;
        int max = 1;
        int[] values = new int[7];
        String[] labels = new String[7];
        java.util.Calendar day = java.util.Calendar.getInstance();
        java.util.Calendar todayCalendar = (java.util.Calendar) day.clone();
        for (int i = 0; i < 7; i++) {
            day.setTimeInMillis(todayCalendar.getTimeInMillis());
            day.add(java.util.Calendar.DAY_OF_YEAR, -i);
            Date dayDate = day.getTime();
            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayDate);
            values[i] = prefs.getInt("count_" + key, 0);
            max = Math.max(max, values[i]);
            total += values[i];
            labels[i] = i == 0 ? "Idag" : new SimpleDateFormat("EEE dd/MM", Locale.getDefault()).format(dayDate);
        }
        for (int i = 0; i < 7; i++) {
            int barCount = values[i] == 0 ? 0 : Math.max(1, (int)Math.round(values[i] * 12.0 / max));
            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < barCount; j++) bar.append("▰");
            report.append(String.format(Locale.getDefault(), "%-9s %3d  ", labels[i], values[i])).append(bar).append("\n");
        }
        report.append("\nTotalt: ").append(total).append(" cigaretter under perioden.");
        report.append("\nGenomsnitt: ").append(String.format(Locale.getDefault(), "%.1f", total / 7.0)).append(" per dag.");
        report.append("\n\nUngefärlig möjlig besparing idag: ").append(money(Math.max(0, baseline() - getCount()) * pricePerCigarette())).append(".");
        report.append("\n\nSiffrorna hjälper dig se mönster, inte döma dig själv.");
        new AlertDialog.Builder(this).setTitle("📊 Min utveckling").setMessage(report.toString())
            .setPositiveButton("Stäng", null).show();
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.WHITE);
        shape.setCornerRadius(dp(22));
        box.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(timerRunnable);
        handler.post(timerRunnable);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(timerRunnable);
    }
}
