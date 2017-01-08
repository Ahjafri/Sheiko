package com.rwardrup.sheiko;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

//TODO: Purchase this icon: http://www.flaticon.com/free-icon/bench_249226#term=weightlifting&page=1&position=24

public class MainActivity extends AppCompatActivity {

    // Lift max variables
    private int squat_max;
    private int bench_max;
    private int deadlift_max;
    private String unit;
    private String unitAbbreviation;
    private Double bodyweight;
    private String sex;
    private String currentCycleText;
    private boolean firstLoad = false;

    // Set font
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up DB connection
        MySQLiteHelper db = new MySQLiteHelper(this);

        /**
         * CRUD Operations
         **/

        // get all workouts
        List<Workout> workoutHistory = db.getAllWorkoutHistory();

        for (int i = 0; i < workoutHistory.size(); i++) {
            Log.i("historyElement", "Workout session: " + workoutHistory.get(i).getDate());
        }

        final String[] oldNumberedPrograms = new String[]{"29", "30", "31", "32", "37", "39", "40"};

        SharedPreferences sharedpref = PreferenceManager.getDefaultSharedPreferences(this);
        TextView currentWorkoutDisplay = (TextView) findViewById(R.id.currentWorkoutText);

        // Get current workout
        String currentProgram = sharedpref.getString("selectedProgram", "Advanced Medium Load");
        String currentCycle = sharedpref.getString("selectedCycle", "1");
        String currentWeek = sharedpref.getString("selectedWeek", "1");
        String currentDay = sharedpref.getString("selectedDay", "1");

        if (Arrays.asList(oldNumberedPrograms).contains(currentProgram)) {
            currentCycleText = "";
        } else {
            currentCycleText = " (" + currentCycle + ") ";
        }


        // Build the string for current workout display
        String currentWorkoutText = currentProgram + currentCycleText + " - " + "Week " +
                currentWeek + " " + "Day " + currentDay;

        currentWorkoutDisplay.setText(currentWorkoutText);

        // Lift max textviews
        TextView squatMax = (TextView) findViewById(R.id.squatMax);
        TextView benchMax = (TextView) findViewById(R.id.benchMax);
        TextView deadliftMax = (TextView) findViewById(R.id.deadliftMax);
        TextView userTotal = (TextView) findViewById(R.id.currentTotal);
        TextView userWilks = (TextView) findViewById(R.id.userWilks);

        final Double bodyweightKg;
        final Double totalWeightKg;

        try {
            //readFromUserParamDb();  // Load databases
            unit = sharedpref.getString("unit", "kilograms");
            bodyweight = (double) sharedpref.getLong("bodyweight", -1);
            sex = sharedpref.getString("sex", "Male");

            firstLoad = bodyweight == -1;

            Log.i("ReadParameters", "Read the following user parameters: " + "Unit: " + unit +
                    ", bodyweight: " + bodyweight + ", sex: " + sex);
        } catch (NullPointerException e) {
            Log.d("DbReadError", "User parameter DB read error: " + e);  // First creation of database.
        }

        try {
            readFromUserMaxDb();  // Load databases

            Double totalWeight = (double) squat_max + (double) bench_max + (double) deadlift_max;
            Log.d("Calculation", "Calculated total: " + totalWeight);

            // set unit abbreviation
            if (unit.equals("kilograms")) {
                unitAbbreviation = "kg.";
            } else if (unit.equals("pounds")) {
                unitAbbreviation = "lb.";
            } else {
                unitAbbreviation = "";
            }

            if (unit.equals("pounds")) {
                bodyweightKg = unitConverter(bodyweight, unit);
                totalWeightKg = unitConverter(totalWeight, unit);
            } else {
                bodyweightKg = bodyweight;
                totalWeightKg = totalWeight;
            }

            double wilksScore = wilksScore(sex, bodyweightKg, totalWeightKg);

            // TODO: properly set the strings
            squatMax.setText(String.valueOf(squat_max) + " " + unitAbbreviation);
            benchMax.setText(String.valueOf(bench_max) + " " + unitAbbreviation);
            deadliftMax.setText(String.valueOf(deadlift_max) + " " + unitAbbreviation);
            userTotal.setText(String.valueOf(totalWeight) + " " + unitAbbreviation);
            userWilks.setText(String.format(Locale.US, "%.2f", wilksScore));


        } catch (NullPointerException e) {
            Log.d("DbReadError", "User max DB read error: " + e);  // First creation of database.
            firstLoad = true;
        }

        if (firstLoad) {
            squatMax.setText("");
            benchMax.setText("");
            deadliftMax.setText("");
            userTotal.setText("");
            userWilks.setText("");
        }

        // The graph on the main window. See www.android-graphview.org for more info
        Calendar calendar = Calendar.getInstance();

        // Add example dates
        String date1 = "01/21/2016";
        String date2 = "03/21/2016";
        String date3 = "05/21/2016";
        String date4 = "07/21/2016";
        String date5 = "09/21/2016";
        String date6 = "11/21/2016";

        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        Date d1 = null;
        Date d2 = null;
        Date d3 = null;
        Date d4 = null;
        Date d5 = null;
        Date d6 = null;

        // Create Date objects from the strings declared above.
        // For the app, this will probably need to be done using a for loop.

        try {
            d1 = df.parse(date1);
            calendar.add(Calendar.DATE, 1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            d2 = df.parse(date2);
            calendar.add(Calendar.DATE, 1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            d3 = df.parse(date3);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            d4 = df.parse(date4);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            d5 = df.parse(date5);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            d6 = df.parse(date6);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        GraphView graph = (GraphView) findViewById(R.id.graph);

        // Add the points to the line graph - Not currently used
        //LineGraphSeries<DataPoint> totalWeightLifted = new LineGraphSeries<>(new DataPoint[]{

        //      new DataPoint(d1, 25475),
        //    new DataPoint(d2, 22330),
        //      new DataPoint(d3, 28795),
        //      new DataPoint(d4, 29977),
        //      new DataPoint(d5, 22590),
        //      new DataPoint(d6, 22550)
        //});

        // Series style
        //totalWeightLifted.setTitle("Volume");
        //totalWeightLifted.setColor(Color.rgb(255, 116, 52));
        //totalWeightLifted.setDrawDataPoints(true);
        //totalWeightLifted.setDataPointsRadius(10);
        //totalWeightLifted.setThickness(8);

        LineGraphSeries<DataPoint> numberLifts = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> averageWeightLifted = new LineGraphSeries<>();

        float minVolume = 999;
        float maxVolume = 0;

        for (int i = 0; i < workoutHistory.size(); i++) {
            Log.i("historyElement", "Workout session: " + workoutHistory.get(i).getDate());
            String _date = workoutHistory.get(i).getDate();
            Integer nLifts = workoutHistory.get(i).getTotalReps();
            Log.i("WorkoutHistory", "Volume: " + nLifts);
            Float averageWeightLiftedAll = workoutHistory.get(i).getAverageWeightLiftedAll();
            Date date = null;

            // Set the minimum and max values for graph second axis
            if (averageWeightLiftedAll <= minVolume) {
                minVolume = averageWeightLiftedAll - 10;
            } else if (averageWeightLiftedAll >= maxVolume) {
                maxVolume = averageWeightLiftedAll + 10;
            }

            try {
                date = df.parse(_date);

                // Add the volume data points
                DataPoint volumedataPoint = new DataPoint(date, nLifts);
                numberLifts.appendData(volumedataPoint, false, 1000, false);

                // Add the AWL data points
                DataPoint averageWeightLiftedDataPoint = new DataPoint(date, averageWeightLiftedAll);
                averageWeightLifted.appendData(averageWeightLiftedDataPoint, false, 1000, false);

            } catch (ParseException e) {
                Log.e("DateParseError", "ParseException in reading workout history: " + e);
            }
        }

        graph.addSeries(numberLifts);

        // Series style for number of lifts
        numberLifts.setTitle("# Lifts");
        numberLifts.setColor(Color.rgb(38, 138, 58));
        numberLifts.setDrawDataPoints(true);
        numberLifts.setDataPointsRadius(10);
        numberLifts.setThickness(8);

        // Series style for Average Weight Lifted
        averageWeightLifted.setTitle("AWL");
        averageWeightLifted.setColor(Color.rgb(0, 119, 211));
        averageWeightLifted.setDrawDataPoints(true);
        averageWeightLifted.setDataPointsRadius(10);
        averageWeightLifted.setThickness(8);

        //graph.addSeries(totalWeightLifted);
        //graph.addSeries(numberLifts);
        graph.getSecondScale().addSeries(averageWeightLifted);

        // Set the date label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.rgb(38, 138, 58));

        // Label the y-axis. TODO: let this be set by the user in the settings window.
        //graph.getGridLabelRenderer().setVerticalAxisTitle("Avg. Wt. Lifted");

        // set manual x bounds to have nice steps
        //graph.getViewport().setMinX(d1.getTime());
        //graph.getViewport().setMaxX(d6.getTime());
        //graph.getViewport().setXAxisBoundsManual(true);

        // User scrollable x axis
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);

        // Set padding for axis labels
        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setPadding(12);

        // Set vertical axis color
        glr.setVerticalLabelsColor(Color.rgb(38, 138, 58));
        // Set second scale bounds TODO: Set these programattically
        graph.getSecondScale().setMinY(minVolume);
        graph.getSecondScale().setMaxY(maxVolume);
        glr.setVerticalLabelsSecondScaleColor(Color.rgb(0, 119, 211));
        graph.getSecondScale().setVerticalAxisTitle("# Lifts");

        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        glr.setHumanRounding(false);

        // Set label font size
        glr.setTextSize(30f);
        glr.reloadStyles();

        // Enable legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setBackgroundColor(Color.argb(50, 157, 157, 157));

        // set Title
        graph.setTitle("Program History");
        graph.setTitleTextSize(48);

        // Handle button clicks that take user to another action.

        //Settings button -> settings action
        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Settings.class));
            }
        });

        // Select Program button -> select program action
        Button selectProgramButton = (Button) findViewById(R.id.selectProgramButton);
        selectProgramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, selectProgram.class));
            }
        });

        // Train button -> Train action
        Button startTrainingButton = (Button) findViewById(R.id.TrainButton);
        startTrainingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, train.class));
            }
        });
    }

    // Read from user User Max DB
    private void readFromUserMaxDb() {
        SQLiteDatabase database = new userMaxesHelper(this).getReadableDatabase();

        Log.d("ReadUserMaxTable", "Attempting to read from User Max table");

        // Get the last user parameter entry
        String[] columns = {
                "units",
                "squat_max",
                "bench_max",
                "deadlift_max",
                "date",
                "wilks"
        };

        Cursor cursor = database.query(userData.UserMaxes.TABLE_NAME, columns, null, null, null, null, null);
        Log.d("CursorCount", "The total cursor count is " + cursor.getColumnCount());

        // Get values from DB cursor
        if (cursor.moveToLast()) {
            try {
                squat_max = Integer.parseInt(cursor.getString(cursor.getColumnIndex(userData.UserMaxes.SQUAT_MAX)));
                bench_max = Integer.parseInt(cursor.getString(cursor.getColumnIndex(userData.UserMaxes.BENCH_MAX)));
                deadlift_max = Integer.parseInt(cursor.getString(cursor.getColumnIndex(userData.UserMaxes.DEADLIFT_MAX)));
            } catch (Exception e) {
                // User probably didn't save anything on the settings page
                FirebaseCrash.report(new Exception("DbReadError: " + e));
                Log.e("DbReadError", "Db Read Error: " + e);
            }
        }
        cursor.close();

        // Write the DB values to text entry fields

        Log.d("Database", "Successfully read user maxes from DB.");
    }

    private double wilksScore(String sex, double bodyWeight, double weightLifted) {
        // Wilk's coefficients. Default to males as they will most likely be the primary user
        double a = -216.0475144;
        double b = 16.2606339;
        double c = -0.002388645;
        double d = -0.00113732;
        double e = 0.00000701863;
        double f = -0.00000001291;

        if (sex.equals("Female")) {  // calculate wilks for females
            a = 594.31747775582;
            b = -27.23842536447;
            c = 0.82112226871;
            d = -0.00930733913;
            e = 0.00004731582;
            f = -0.00000009054;
            Log.i("Wilks", "Calculating wilks for female");
        }

        Log.i("Wilks", "Sex: " + sex);
        Log.i("Wilks", "Bodyweight: " + bodyWeight);
        Log.i("Wilks", "Total: " + weightLifted);

        // The wilk's coeficient:
        double wilksCoefficient = 500 / (a +
                (b * bodyWeight) +
                (c * (bodyWeight * bodyWeight)) +
                (d * (bodyWeight * bodyWeight * bodyWeight)) +
                (e * (bodyWeight * bodyWeight * bodyWeight * bodyWeight)) +
                (f * (bodyWeight * bodyWeight * bodyWeight * bodyWeight * bodyWeight)));

        Log.i("Wilks", "Wilks Coefficient: " + wilksCoefficient);
        Log.i("Wilks", "Wilks a: " + a);
        Log.i("Wilks", "Wilks b: " + b);
        Log.i("Wilks", "Wilks c: " + c);
        Log.i("Wilks", "Wilks d: " + d);
        Log.i("Wilks", "Wilks e: " + e);
        Log.i("Wilks", "Wilks f: " + f);

        return weightLifted * wilksCoefficient;
    }

    private double unitConverter(double fromWeight, String fromUnit) {
        double toWeight;
        if (fromUnit.equals("pounds")) {
            Log.i("UnitConverter", "Converting " + fromUnit + " to kg");
            toWeight = fromWeight * 0.45359237;
        } else {
            Log.i("UnitConverter", "Converting " + fromUnit + " to lbs");
            toWeight = fromWeight * 2.2046;
        }

        return toWeight;
    }

    // Create user parameter DB
    public class userParametersHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "SheikoDb";  // TODO: change this
        private static final int DATABASE_VERSION = 1;

        public userParametersHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(userData.UserParameters.CREATE_TABLE);
            sqLiteDatabase.execSQL(userData.UserMaxes.CREATE_TABLE);
            Log.d("SQL", "Created userMaxes table");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + userData.UserParameters.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }

    // Create user maxes DB
    public class userMaxesHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "SheikoDb";  // TODO: change this
        private static final int DATABASE_VERSION = 1;

        public userMaxesHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(userData.UserMaxes.CREATE_TABLE);
            Log.d("SQL", "Created userMaxes table");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + userData.UserMaxes.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }
}
