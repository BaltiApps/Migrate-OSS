package balti.migrate.simpleActivities

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_DEV_LINK
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_LINK
import kotlinx.android.synthetic.main.help_page.*

class HelpPageKotlin: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.help_page)

        helpBackButton.setOnClickListener { finish() }

        more_questions_feedback.setOnClickListener {

            AlertDialog.Builder(this)
                    .apply {
                    setTitle(R.string.contact_via_telegram)
                    setMessage(R.string.contact_via_telegram_desc)
                    setPositiveButton(R.string.post_in_group) {_, _ ->
                        commonTools.openWebLink(TG_LINK)
                    }
                    setNeutralButton(android.R.string.cancel, null)
                    setNegativeButton(R.string.contact_dev) {_, _ ->
                        commonTools.openWebLink(TG_DEV_LINK)
                    }
            }.show()
        }
    }

}