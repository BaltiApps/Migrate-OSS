package balti.migrate;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CommonTools {

    Context context;
    public static String DEBUG_TAG = "migrate_tag";

    public CommonTools(Context context) {
        this.context = context;
    }

    String unpackAssetToInternal(String assetFileName, String targetFileName){

        AssetManager assetManager = context.getAssets();
        File unpackFile = new File(context.getFilesDir(), targetFileName);
        String path = "";

        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open(assetFileName);
            FileOutputStream writer = new FileOutputStream(unpackFile);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
            unpackFile.setExecutable(true);
            path = unpackFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            path = "";
        }

        return path;
    }

    boolean isServiceRunning(String name){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (name.equals(service.service.getClassName()))
                return true;
        }
        return false;
    }
}
