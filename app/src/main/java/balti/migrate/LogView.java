package balti.migrate;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by sayantan on 1/11/17.
 */

public class LogView extends LinearLayout {

    Button button;
    View container;

    TextView progressLog, errorLog;
    TextView errorLogHeader;

    LinearLayout logViewActual;

    public LogView(final Context context, @Nullable AttributeSet attrs, @Nullable LogView logView) {
        super(context, attrs);

        container = inflate(context, R.layout.log_view, this);
        button = container.findViewById(R.id.logButton);
        logViewActual = container.findViewById(R.id.logViewActual);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (logViewActual.getVisibility() == GONE)
                logViewActual.setVisibility(VISIBLE);
                else logViewActual.setVisibility(GONE);
            }
        });

        progressLog = container.findViewById(R.id.progressLogDisplay);
        progressLog.setGravity(Gravity.BOTTOM);
        progressLog.setMovementMethod(new ScrollingMovementMethod());
        errorLog = container.findViewById(R.id.errorLogDisplay);
        errorLogHeader = container.findViewById(R.id.errorLogHeader);

        if (logView != null){
            //progressLog.setText(logView.progressLog.getText());
            errorLog.setVisibility(logView.errorLog.getVisibility());
            errorLog.setText(logView.errorLog.getText());
            errorLogHeader.setVisibility(logView.errorLogHeader.getVisibility());
        }
    }

    void addProgress(String text){
        progressLog.append(text+"\n");
    }

    void setError(ArrayList<String> errors){
        if (errors.size() > 0) {
            errorLog.setVisibility(VISIBLE);
            errorLogHeader.setVisibility(VISIBLE);
            for (int i = 0; i < errors.size(); i++) {
                errorLog.append(errors.get(i) + "\n");
            }
        }
    }
}
