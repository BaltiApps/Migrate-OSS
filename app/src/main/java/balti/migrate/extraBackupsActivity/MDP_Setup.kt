package balti.migrate.extraBackupsActivity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MDP_INSTALL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MDP_SU
import balti.migrate.utilities.constants.MDP_Constants
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.mdp_setup.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MDP_Setup: AppCompatActivity() {

    private val suStatusReceiver by lazy {
        object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val granted = intent?.getBooleanExtra(MDP_Constants.EXTRA_SU_GRANTED, false) ?: false
                if (granted) {
                    setSuDone()
                    sendResult(true)
                }
                else {
                    setSuCancel()
                    sendResult(false, "${ERR_MDP_SU}: " +
                            if (intent?.hasExtra(MDP_Constants.EXTRA_SU_ERROR) == true)
                                intent.getStringExtra(MDP_Constants.EXTRA_SU_ERROR).toString()
                            else "null"
                    )
                }
            }
        }
    }

    private val LBM by lazy { LocalBroadcastManager.getInstance(this) }
    private val installError = StringBuffer("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mdp_setup)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)

        mdp_setup_close.setOnClickListener {
            sendResult(false)
        }

        if (Misc.isPackageInstalled(MDP_Constants.MDP_PACKAGE_NAME)) {
            setInstallDone()
            setSuInProgress()
            askSu()
        }
        else {
            mdp_install_button.setOnClickListener {
                setInstallInProgress()
                doBackgroundTask({
                    try {
                        installMdp()
                    }
                    catch (e: Exception){
                        installError.append("${ERR_MDP_INSTALL}: Try-catch - ${e.message}")
                    }
                }, {
                    if (Misc.isPackageInstalled(MDP_Constants.MDP_PACKAGE_NAME)) {
                        setInstallDone()
                        setSuInProgress()
                        askSu()
                    } else {
                        setInstallCancel()
                        sendResult(false, "${ERR_MDP_INSTALL}: $installError")
                    }
                })

            }
        }

        LBM.registerReceiver(suStatusReceiver, IntentFilter(MDP_Constants.ACTION_TO_MDP_SETUP))
    }

    private fun setInstallDone(){
        View.GONE.let {
            mdp_install_bullet.visibility = it

            mdp_install_progress_bar.visibility = it
            mdp_install_cancel.visibility = it
            mdp_install_done.visibility = View.VISIBLE

            mdp_install_button.visibility = it
        }
    }

    private fun setInstallCancel(){
        View.GONE.let {
            mdp_install_bullet.visibility = it

            mdp_install_progress_bar.visibility = it
            mdp_install_cancel.visibility = View.VISIBLE
            mdp_install_done.visibility = it

            mdp_install_button.visibility = View.VISIBLE
        }
    }

    private fun setInstallInProgress(){
        View.GONE.let {
            mdp_install_bullet.visibility = it

            mdp_install_progress_bar.visibility = View.VISIBLE
            mdp_install_cancel.visibility = it
            mdp_install_done.visibility = it

            mdp_install_button.visibility = it
        }
    }

    private fun setSuDone(){
        View.GONE.let {
            mdp_su_bullet.visibility = it
            mdp_su_please_wait.visibility = it

            mdp_su_progress_bar.visibility = it
            mdp_su_cancel.visibility = it
            mdp_su_done.visibility = View.VISIBLE
        }
    }

    private fun setSuCancel(){
        View.GONE.let {
            mdp_su_bullet.visibility = it
            mdp_su_please_wait.visibility = it

            mdp_su_progress_bar.visibility = it
            mdp_su_cancel.visibility = View.VISIBLE
            mdp_su_done.visibility = it
        }
    }

    private fun setSuInProgress(){
        View.GONE.let {
            mdp_su_bullet.visibility = it
            mdp_su_please_wait.visibility = View.VISIBLE

            mdp_su_progress_bar.visibility = View.VISIBLE
            mdp_su_cancel.visibility = it
            mdp_su_done.visibility = it
        }
    }

    private fun askSu(){
        doBackgroundTask({
            Thread.sleep(MDP_Constants.MDP_DELAY)
        }, {
            startActivity(MDP_Constants.getMdpIntent(Bundle(), MDP_Constants.EXTRA_OPERATION_DUMMY_SU))
        })

    }

    private fun installMdp(){
        val mdpApkPath = unpackAssetToInternal("mdp.apk")

        Runtime.getRuntime().exec("su").let {
            val writer = BufferedWriter(OutputStreamWriter(it.outputStream))
            val errorStream = BufferedReader(InputStreamReader(it.errorStream))

            writer.write("mkdir -p /data/local/tmp/\n")
            writer.write("\n")
            writer.write("verification_state=\"\$(settings get global package_verifier_enable)\"\n")
            writer.write("if [[ -n \${verification_state} && \${verification_state} != \"null\" && \${verification_state} != \"0\" ]]; then\n")
            writer.write("    settings put global package_verifier_enable 0\n")
            writer.write("fi\n")
            writer.write("\n")
            writer.write("cp $mdpApkPath /data/local/tmp/mdp.apk\n")
            writer.write("chmod 777 /data/local/tmp/mdp.apk\n")
            writer.write("pm install /data/local/tmp/mdp.apk\n")
            writer.write("\n")
            writer.write("settings put global package_verifier_enable \${verification_state}\n")

            writer.write("exit\n")
            writer.flush()

            while (true) {
                val line: String? = errorStream.readLine()
                if (line == null) break
                else {
                    installError.append("$line\n")
                }
            }
        }
    }

    private fun sendResult(success: Boolean, error: String? = null){
        setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().apply {
            if (error != null) putExtra(MDP_Constants.EXTRA_ERRORS, error)
        })
        finish()
    }

    override fun onBackPressed() {
        // do not close
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { LBM.unregisterReceiver(suStatusReceiver) }
    }
}