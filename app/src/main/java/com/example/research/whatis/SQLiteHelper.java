package com.example.research.whatis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Whatis.db";
    public static final String WORDS_TABLE_NAME = "extracts";
    public static final String WORDS_COLUMN_ID = "id";
    public static final String WORDS_COLUMN_NAME = "word";
    public static final String WORDS_SYN_COLUMN = "synonym";
    public static final String WORDS_REL_COLUMN = "relevant_synonyms";
    public static final String WORDS_REF_COLUMN = "reference_site";
    private HashMap hp;

    public SQLiteHelper(Context context)
    {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table extracts " +
                        "(id integer primary key, word text,synonym text,relevant_synonyms text, reference_site text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(db);
    }

    public boolean insertWord(String word, String synonym, String relevant_synonyms, String reference_site)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WORDS_COLUMN_NAME, word);
        contentValues.put(WORDS_SYN_COLUMN, synonym);
        contentValues.put(WORDS_REL_COLUMN, relevant_synonyms);
        contentValues.put(WORDS_REF_COLUMN, reference_site);
        db.insert(WORDS_TABLE_NAME, null, contentValues);
        return true;
    }

    public Cursor getData(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + DATABASE_NAME + " where id="+id+"", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, WORDS_TABLE_NAME);
        return numRows;
    }

    public boolean updateWord(Integer id, String word, String synonym, String relevant_synonyms, String reference_site)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WORDS_COLUMN_NAME, word);
        contentValues.put(WORDS_SYN_COLUMN, synonym);
        contentValues.put(WORDS_REL_COLUMN, relevant_synonyms);
        contentValues.put(WORDS_REF_COLUMN, reference_site);
        db.update(WORDS_TABLE_NAME, contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }

    public Integer deleteWord(Integer id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(WORDS_TABLE_NAME,
                "id = ? ",
                new String[] { Integer.toString(id) });
    }

    public ArrayList<String> getAllWords()
    {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + WORDS_TABLE_NAME, null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex(WORDS_COLUMN_NAME)));
            res.moveToNext();
        }
        return array_list;
    }
}