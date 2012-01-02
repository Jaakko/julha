package fi.avoindata.julha.history;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

public final class DBHelper extends SQLiteOpenHelper {

  private static final int DB_VERSION = 1;
  private static final String DB_NAME = "call_history.db";
  public static final String TABLE_NAME = "history";
  public static final String ID_COL = "id";
  public static final String FULLNAME_COL = "fullname";
  public static final String NUMBER_COL = "number";
  public static final String GIVENNAME_COL = "givenname";
  public static final String SN_COL = "sn";
  public static final String ORG_COL = "org";
  public static final String STATUS_COL = "status";
  public static final String TIMESTAMP_COL = "timestamp";

  public DBHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(
            "CREATE TABLE " + TABLE_NAME + " (" +
            ID_COL + " INTEGER PRIMARY KEY, " +
            FULLNAME_COL + " TEXT, " +
            GIVENNAME_COL + " TEXT, " +
            SN_COL + " TEXT, " +
            ORG_COL + " TEXT, " +
            NUMBER_COL + " TEXT, " +
            STATUS_COL + " INTEGER, " +
            TIMESTAMP_COL + " INTEGER" +
            ");");
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(sqLiteDatabase);
  }

}