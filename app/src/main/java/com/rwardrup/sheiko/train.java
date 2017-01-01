package com.rwardrup.sheiko;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.shawnlin.numberpicker.NumberPicker;

import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class train extends AppCompatActivity implements RestDurationPicker.DurationListener {

    private static long millisLeftOnTimer;
    private static Integer secondsLeftOnTimer;
    Button startBreakTimerButton;
    Button stopBreakTimerButton;
    Button pauseBreakTimerButton;

    // Activity buttons
    Button squatSelectButton;
    Button deadliftSelectButton;
    Button benchSelectButton;
    Spinner accessorySpinner;

    // Text output
    TextView breakTimerOutput;
    TextView currentExercise;

    // Timer stuff
    private Integer timerDurationSeconds;  // 3 minutes is a good default value
    private boolean timerIsPaused = false;
    private boolean timerIsRunning = false;
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent);
        }
    };

    // Set font
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        // TODO: Programmatically set the array of today's accessories based on the sqlite db row
        String[] todaysAccessories = new String[]{"", "French Press", "Pullups", "Abs", "Bent-Over Rows",
                "Seated Good Mornings", "Good Mornings", "Hyperextensions", "Dumbell Flys"};

        // Hide the accessory spinner text
        accessorySpinner = (Spinner) findViewById(R.id.accessorySpinner);
        CustomAdapter<String> accessorySpinnerAdapter = new CustomAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, todaysAccessories);
        accessorySpinner.setAdapter(accessorySpinnerAdapter);

        // Shared prefs
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Try to get the timer duration from shared preferences, defaulting to 1.5 minutes if it
        // hasn't been set. Time is stored in milliseconds in sharedPreferences. The time is saved
        // in the sharedPreferences file in RestDurationPicker.
        timerDurationSeconds = (int) (long) (sharedPref.getLong("timerDuration", 180000) / 1000);
        Log.d("TimerSettings", "Timer duration loaded: " + timerDurationSeconds);

        // For passing the timer duration to the timer service
        final Intent sendDuration = new Intent("getting_data");

        startBreakTimerButton = (Button) findViewById(R.id.startBreakTimer);
        stopBreakTimerButton = (Button) findViewById(R.id.stopBreakButton);
        pauseBreakTimerButton = (Button) findViewById(R.id.pauseBreakButton);
        squatSelectButton = (Button) findViewById(R.id.squatSelectButton);
        benchSelectButton = (Button) findViewById(R.id.benchSelectButton);
        deadliftSelectButton = (Button) findViewById(R.id.deadliftButton);

        breakTimerOutput = (TextView) findViewById(R.id.breakTimerOutput);
        currentExercise = (TextView) findViewById(R.id.currentExerciseDisplay);

        // Set the reps and weights
        setRepsWeightPickers();

        this.squatSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExercise.setText(R.string.squat);
            }
        });

        this.benchSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExercise.setText(R.string.bench);
            }
        });

        this.deadliftSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExercise.setText(R.string.deadlift);
            }
        });

        this.accessorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean activityLoaded = false;

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String selectedAccessory = arg0.getSelectedItem().toString();
                Log.i("Selection", "Accessory was selected: " + selectedAccessory);

                // If user has selected an accessory, update the current activity. This prevents
                // The current activity from being set to the acessory on Activity load.
                if (selectedAccessory.length() > 0 && activityLoaded) {
                    currentExercise.setText(selectedAccessory);
                } else {
                    activityLoaded = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }
        });


        // TODO: Handle accesssory change -> updated current workout text

        // Handle user long-clicking on the timer output text to change timer length on-the-fly
        // This utilizes the onDurationSet method at the bottom of this class.
        breakTimerOutput.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                new RestDurationPicker().show(getFragmentManager(), "Session break length");
                return true;
            }
        });

        // Set timer display TODO: Get this to keep incrementing dimer display off-activity
        breakTimerOutput.setText(secondsToString(timerDurationSeconds));

        // break timer start / stop / pause
        // break timer start
        startBreakTimerButton.setOnClickListener(new View.OnClickListener() {

            Intent timerService = new Intent(train.this, BreakTimer.class);

            @Override
            public void onClick(View v) {
                    breakTimerOutput.setTextSize(36);
                    int timerDuration = timerDurationSeconds * 1000;  // This will be set by user in final code

                    // Set up service for timer
                    startService(timerService);
                    Log.i("TimerService", "Started timer service");
                }
        });

        // break timer stop
        stopBreakTimerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                    millisLeftOnTimer = 0;
                    //mCountDownTimer.cancel();
                    stopService(new Intent(train.this, BreakTimer.class));
                    breakTimerOutput.setTextSize(36);
                    breakTimerOutput.setText(secondsToString(timerDurationSeconds));
                }
        });
    }

    // Break timer long-click set time
    @Override
    public void onDurationSet(long duration) {
        Integer i = (int) (long) duration;  // get integer i from duration (long)
        timerDurationSeconds = i / 1000; // convert millis to seconds


        // Assign the new custom timer duration to the timerduration variable
        breakTimerOutput.setText(secondsToString(timerDurationSeconds));
        Log.d("NewTimer", "New Timer Duration: " + secondsToString(timerDurationSeconds));
    }

    // convert seconds to minutes and seconds for display
    private String secondsToString(int pTime) {
        return String.format(Locale.US, "%02d:%02d", pTime / 60, pTime % 60);
    }

    // set reps and weight picker contents
    private void setRepsWeightPickers() {

        NumberPicker repPicker = (NumberPicker) findViewById(R.id.repsPicker);
        NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);

        // Disable keyboard
        repPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        weightPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // Enable fading edges
        repPicker.setVerticalFadingEdgeEnabled(true);
        weightPicker.setVerticalFadingEdgeEnabled(true);

        // Reps
        repPicker.setMinValue(0);
        repPicker.setMaxValue(20);
        repPicker.setValue(5);

        // Weight
        weightPicker.setMinValue(0);
        weightPicker.setMaxValue(240); //480 * 2.5 == 1200

        int length = 500;
        int step = 5;

        String[] numbers = new String[length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = String.valueOf((i + 1) * step);
        }

        weightPicker.setDisplayedValues(numbers);

        // TODO: Pull this start value from the DB of today's workout, per lift
        weightPicker.setValue(50);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(BreakTimer.COUNTDOWN_BR));
        Log.i("BreakTimer", "Registered broacast receiver");
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(br);
        } catch (Exception e) {
            Log.e("TimerError", "timer encountered: " + e);  // User probably closed during pause
        }
        Log.i("BreakTimer", "Unregistered broacast receiver");
    }

    @Override
    public void onStop() {
        try {
            unregisterReceiver(br);
        } catch (Exception e) {
            // Receiver was probably already stopped in onPause()
        }
        super.onStop();
    }

    //@Override
    //public void onDestroy() {
    // do nothing
    //   super.onDestroy();
    //}

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            long millisUntilFinished = intent.getLongExtra("countdown", 0);
            millisLeftOnTimer = millisUntilFinished;
            secondsLeftOnTimer = (int) (long) millisUntilFinished / 1000;
            breakTimerOutput.setText(secondsToString(secondsLeftOnTimer));
            Log.i("BreakTimer", "Countdown seconds remaining: " + millisUntilFinished / 1000);
        }
    }

    // Adapter for accessory spinner. Want to hide the display string and dynamically update it
    // when the user changes workouts. Hide the first entry which is an empty string that prevents
    // the currentExercise from being displayed on load.
    private static class CustomAdapter<T> extends ArrayAdapter<String> {

        public CustomAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText("");
            return view;
        }
    }
}