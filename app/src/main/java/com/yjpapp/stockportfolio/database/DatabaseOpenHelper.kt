package com.yjpapp.stockportfolio.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseOpenHelper(context: Context): SQLiteOpenHelper(context,"database.db",null,1) {
    private val DB_VERSION = 1

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Databases.CREATE_MEMO_TABLE)
        db?.execSQL(Databases.CREATE_PORTFOLIO_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}