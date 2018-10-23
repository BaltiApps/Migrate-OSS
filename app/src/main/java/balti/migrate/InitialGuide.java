package balti.migrate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class InitialGuide extends AppCompatActivity {
    Button previous, next;

    ViewPager viewPager;
    View.OnClickListener scrollNext, scrollPrevious;

    int TOTAL_LAYOUTS = 0;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_guide);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        if (!main.getBoolean("firstRun", true) && !getIntent().getBooleanExtra("manual", false)){
            startActivity(new Intent(InitialGuide.this, MainActivity.class));
            finish();
        }

        next = findViewById(R.id.initial_guide_next);
        previous = findViewById(R.id.initial_guide_prev);


        final int arrLayouts[] = new int[]{
                R.layout.initial_guide_0,
                R.layout.initial_guide_1
        };

        TOTAL_LAYOUTS = arrLayouts.length;


        viewPager = findViewById(R.id.initial_guide_view_pager);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return TOTAL_LAYOUTS;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View layout = View.inflate(InitialGuide.this, arrLayouts[position], null);
                container.addView(layout);
                return layout;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }
        });

        scrollNext = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() < TOTAL_LAYOUTS-1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
                else {
                    finishGuide();
                }
            }
        };

        scrollPrevious = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() > 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                }
                else {
                    finishGuide();
                }
            }
        };

        next.setOnClickListener(scrollNext);
        previous.setOnClickListener(scrollPrevious);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == TOTAL_LAYOUTS-1) next.setText(R.string.got_it);
                else next.setText(R.string.next);

                if (position == 0) previous.setText(R.string.skip);
                else previous.setText(R.string.prev);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    void finishGuide(){
        if (editor != null){
            editor.putBoolean("firstRun", false);
            editor.commit();
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
