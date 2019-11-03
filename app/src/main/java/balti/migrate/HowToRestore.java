package balti.migrate;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class HowToRestore extends AppCompatActivity {

    Button previous, next;
    ImageButton close;

    ViewPager viewPager;
    View.OnClickListener scrollNext, scrollPrevious;

    int TOTAL_LAYOUTS = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_to_restore);

        next = findViewById(R.id.how_to_restore_next);
        previous = findViewById(R.id.how_to_restore_prev);

        close = findViewById(R.id.how_to_restore_close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final int arrLayouts[] = new int[]{
                R.layout.restore_0,
                R.layout.restore_1,
                R.layout.restore_2,
                R.layout.restore_3,
                R.layout.restore_4,
                R.layout.restore_6
        };

        TOTAL_LAYOUTS = arrLayouts.length;

        viewPager = findViewById(R.id.how_to_restore_view_pager);
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
                View layout = View.inflate(HowToRestore.this, arrLayouts[position], null);
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
                    finish();
                }
            }
        };

        scrollPrevious = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() > 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                } else {
                    finish();
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
                if (position == TOTAL_LAYOUTS - 1) next.setText(R.string.got_it);
                else next.setText(R.string.next);

                if (position == 0) previous.setText(R.string.close);
                else previous.setText(R.string.prev);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
