package balti.migrate;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;

public class SimpleLogDisplay extends AppCompatActivity {

    ImageButton back, send;
    TextView header;
    TextView logBody;
    ProgressBar progressBar;

    String headerText, filePath;

    LoadLogText loadLogText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_log_display);

        back = findViewById(R.id.logViewBackButton);
        send = findViewById(R.id.logSendButton);
        header = findViewById(R.id.logViewHeader);
        logBody = findViewById(R.id.logBody);
        progressBar = findViewById(R.id.log_view_progress_bar);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CommonTools(SimpleLogDisplay.this).reportLogs(false);
            }
        });

        loadLogText = new LoadLogText(getIntent());
        loadLogText.execute();

    }

    class LoadLogText extends AsyncTask {

        Intent intent;
        String err;

        LoadLogText(Intent intent) {
            this.intent = intent;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            err = "";
            logBody.setText("");
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            if (!intent.hasExtra("head")) {
                err += getString(R.string.no_header) + "\n";
            }
            if (!intent.hasExtra("filePath")) {
                err += getString(R.string.no_filepath) + "\n";
            }

            err = err.trim();

            if (!err.equals("")) return null;

            headerText = intent.getStringExtra("head");
            publishProgress(0, headerText);

            filePath = intent.getStringExtra("filePath");

            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    publishProgress(1, line + "\n");
                }

            } catch (Exception e) {
                e.printStackTrace();
                err += e.getMessage() + "\n";
            }

            err = err.trim();

            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            try {
                int i = (int) values[0];

                if (i == 0) header.setText((String) values[1]);
                else logBody.append((String) values[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressBar.setVisibility(View.GONE);

            if (!err.equals("")) {
                new AlertDialog.Builder(SimpleLogDisplay.this)
                        .setTitle(R.string.log_reading_error)
                        .setMessage(err)
                        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            loadLogText.cancel(true);
        } catch (Exception ignored) {
        }
    }
}
