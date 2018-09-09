package balti.migrate;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

class DatabaseContext extends ContextWrapper {

    File dbFile;

    public DatabaseContext(Context base, String dbFilePath) {
        super(base);
        dbFile = new File(dbFilePath);

    }

    @Override
    public File getDatabasePath(String name) {

        if (!dbFile.getParentFile().exists())
            dbFile.getParentFile().mkdirs();

        return dbFile;
    }
}

public class DatabaseHelper extends SQLiteOpenHelper {

    String SQL_CREATE_TABLE;
    String SQL_DROP_TABLE;

    DatabaseHelper(Context context, String dbFilePath, String SQL_CREATE_TABLE, String SQL_DROP_TABLE){
        super(new DatabaseContext(context, dbFilePath), dbFilePath.substring(dbFilePath.lastIndexOf('/') + 1), null, 1);
        this.SQL_CREATE_TABLE = SQL_CREATE_TABLE;
        this.SQL_DROP_TABLE = SQL_DROP_TABLE;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_DROP_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        onCreate(db);
    }
}
