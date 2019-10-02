package balti.migrate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HelpPage extends AppCompatActivity {

    ImageButton back;
    Button contact;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_page);

        back = findViewById(R.id.helpBackButton);
        contact = findViewById(R.id.more_questions_feedback);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent email = new Intent(Intent.ACTION_SENDTO);
                email.setData(Uri.parse("mailto:"));
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                email.putExtra(Intent.EXTRA_SUBJECT, "Feedback for Migrate");
                try {
                    startActivity(Intent.createChooser(email, getString(R.string.select_telegram)));
                } catch (Exception e) {
                    Toast.makeText(HelpPage.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
