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
import android.widget.RelativeLayout;

import balti.migrate.simpleActivities.MainActivityKotlin;

public class InitialGuide extends AppCompatActivity {
    Button previous, next;

    ViewPager viewPager;
    View.OnClickListener scrollNext, scrollPrevious;

    RelativeLayout buttonBar;

    int TOTAL_LAYOUTS = 0;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_guide);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        next = findViewById(R.id.initial_guide_next);
        previous = findViewById(R.id.initial_guide_prev);
        buttonBar = findViewById(R.id.initial_guide_button_bar);
        viewPager = findViewById(R.id.initial_guide_view_pager);

        final int arrLayouts[] = new int[]{
                R.layout.initial_guide_0,
                R.layout.initial_guide_1,
                R.layout.initial_guide_2,
                R.layout.initial_guide_3
        };

        TOTAL_LAYOUTS = arrLayouts.length;

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
                container.removeView((View) object);
            }
        });

        scrollNext = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() < TOTAL_LAYOUTS - 1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                } else {
                    finishGuide(true);
                }
            }
        };

        scrollPrevious = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() > 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                } else {
                    finishGuide(true);
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
                if (position == TOTAL_LAYOUTS - 1) next.setText(R.string.accept);
                else next.setText(R.string.next);

                if (position == 0) previous.setText(R.string.skip);
                else previous.setText(R.string.prev);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    void finishGuide(boolean write) {
        if (write && editor != null) {
            editor.putBoolean("firstRun", false);
            editor.commit();
        }
        startActivity(new Intent(this, MainActivityKotlin.class));
        finish();
    }
}
