package balti.migrate.simpleActivities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_SHOW_FIRST_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FIRST_RUN
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import kotlinx.android.synthetic.main.initial_guide.*

class InitialGuideKotlin: AppCompatActivity() {

    var TOTAL_LAYOUTS = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.initial_guide)

        val arrLayouts = arrayOf(
                R.layout.initial_guide_0,
                R.layout.initial_guide_1,
                R.layout.initial_guide_2,
                R.layout.initial_guide_3
        )

        TOTAL_LAYOUTS = arrLayouts.size

        initial_guide_view_pager.adapter = object : androidx.viewpager.widget.PagerAdapter(){

            override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
            override fun getCount(): Int = TOTAL_LAYOUTS

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val layout = View.inflate(this@InitialGuideKotlin, arrLayouts[position], null)
                container.addView(layout)
                return layout
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }
        }

        initial_guide_next.setOnClickListener {
            if (initial_guide_view_pager.currentItem < TOTAL_LAYOUTS - 1){
                initial_guide_view_pager.currentItem += 1
            } else finishGuide()
        }

        initial_guide_prev.setOnClickListener {
            if (initial_guide_view_pager.currentItem > 0){
                initial_guide_view_pager.currentItem -= 1
            } else finishGuide()
        }

        initial_guide_view_pager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageSelected(position: Int) {
                if (position == TOTAL_LAYOUTS - 1) initial_guide_next.setText(R.string.accept)
                else initial_guide_next.setText(R.string.next)

                if (position == 0) initial_guide_prev.setText(R.string.skip)
                else initial_guide_prev.setText(R.string.prev)
            }

        })
    }

    private fun finishGuide(){
        val intent =
                Intent(this, MainActivityKotlin::class.java)
                        .putExtra(EXTRA_SHOW_FIRST_WARNING, getPrefBoolean(PREF_FIRST_RUN, true))
        putPrefBoolean(PREF_FIRST_RUN, false, immediate = true)
        startActivity(intent)
        finish()
    }
}