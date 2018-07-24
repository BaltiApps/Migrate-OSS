package balti.migrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import static android.view.View.VISIBLE;

/**
 * Created by sayantan on 22/1/18.
 */

public class BackupProgressLayout extends AppCompatActivity {

    BroadcastReceiver progressReceiver, logReceiver;
    IntentFilter progressReceiverIF, logReceiverIF;

    TextView task;
    TextView progress;
    TextView progressLog;
    TextView errorLog;
    ProgressBar progressBar;
    Button actionButton;

    Intent backIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_progress_layout);

        task = findViewById(R.id.progressTask);
        progress = findViewById(R.id.progressPercent);
        progressBar = findViewById(R.id.progressBar);
        progressLog = findViewById(R.id.progressLogTextView);
        errorLog = findViewById(R.id.errorLogTextView);
        actionButton = findViewById(R.id.progressActionButton);

        progressLog.setGravity(Gravity.BOTTOM);
        progressLog.setMovementMethod(new ScrollingMovementMethod());

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent("Migrate backup cancel broadcast"));
            }
        });

        backIntent = null;

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                int p = intent.getIntExtra("progress", 0);
                if (p >= 0) {
                    progress.setText(p + "%");
                    String t = intent.getStringExtra("task");
                    task.setText(t);
                    progressBar.setProgress(p);
                    if (p < 100){
                        backIntent = null;
                        actionButton.setText(getString(android.R.string.cancel));
                    }
                    else{
                        backIntent = new Intent(BackupProgressLayout.this, BackupActivity.class);
                        actionButton.setText(getString(R.string.close));
                        actionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finishThis();
                            }
                        });
                    }
                }
            }

        };

        progressReceiverIF = new IntentFilter("Migrate progress broadcast");
        registerReceiver(progressReceiver, progressReceiverIF);


        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("type").equals("progress")){
                    progressLog.append(intent.getStringExtra("content") + "\n");
                }
                else if (intent.getStringExtra("type").equals("errors")){
                    setError(intent.getStringArrayListExtra("content"));
                }
            }
        };

        logReceiverIF = new IntentFilter("Migrate log broadcast");
        registerReceiver(logReceiver, logReceiverIF);


        if (getIntent().getAction() != null && getIntent().getAction().equals("finished"))
        {
            Intent finishedBroadcast = new Intent("Migrate progress broadcast");
            finishedBroadcast.putExtra("progress", 100);
            finishedBroadcast.putExtra("task", getIntent().getStringExtra("finishedMessage"));
            sendBroadcast(finishedBroadcast);
            sendBroadcast(new Intent("Migrate log broadcast").putExtra("type", "errors").putStringArrayListExtra("content", getIntent().getStringArrayListExtra("errors")));
        }
        else {
            sendBroadcast(new Intent("get data"));
        }
    }


    void setError(ArrayList<String> errors){
        if (errors.size() > 0) {
            errorLog.setVisibility(VISIBLE);
            for (int i = 0; i < errors.size(); i++) {
                errorLog.append(errors.get(i) + "\n");
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishThis();
    }


    void finishThis(){
        try {
            if (progressReceiver != null) unregisterReceiver(progressReceiver);
        }
        catch (IllegalArgumentException ignored){}
        try {
            if (logReceiver != null) unregisterReceiver(logReceiver);
        }
        catch (IllegalArgumentException ignored){}
        if (backIntent != null) startActivity(backIntent);
        finish();
    }
}
