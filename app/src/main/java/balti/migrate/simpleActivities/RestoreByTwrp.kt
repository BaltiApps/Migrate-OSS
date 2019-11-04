package balti.migrate.simpleActivities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import balti.migrate.R
import kotlinx.android.synthetic.main.how_to_restore.*

class RestoreByTwrp: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.how_to_restore)

        val arrLayouts = intArrayOf(
                R.layout.restore_0,
                R.layout.restore_1,
                R.layout.restore_2,
                R.layout.restore_3,
                R.layout.restore_4,
                R.layout.restore_5,
                R.layout.restore_6
        )

        val TOTAL_LAYOUTS = arrLayouts.size

        how_to_restore_view_pager.adapter = object : PagerAdapter(){
            override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
            override fun getCount(): Int = TOTAL_LAYOUTS

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val layout = View.inflate(this@RestoreByTwrp, arrLayouts[position], null);
                container.addView(layout)
                return layout
            }
            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }
        }

        how_to_restore_next.setOnClickListener {
            how_to_restore_view_pager.run {
                if (currentItem < TOTAL_LAYOUTS - 1)
                    currentItem += 1
                else finish()
            }
        }

        how_to_restore_prev.setOnClickListener {
            how_to_restore_view_pager.run {
                if (currentItem > 0)
                    currentItem -= 1
                else finish()
            }
        }

        how_to_restore_view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                how_to_restore_next.setText(
                        if (position == TOTAL_LAYOUTS - 1) R.string.got_it
                        else R.string.next
                )

                how_to_restore_prev.setText(
                        if (position == 0) R.string.close
                        else R.string.prev
                )
            }
        })
    }

}