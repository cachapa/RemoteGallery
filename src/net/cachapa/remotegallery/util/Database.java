/*
 * Copyright 2008-2010 Daniel Cachapa <cachapa@gmail.com>
 * 
 * This program is distributed under the terms of the GNU General Public License Version 3
 * The license can be read in its entirety in the LICENSE.txt file accompanying this source code,
 * or at: http://www.gnu.org/copyleft/gpl.html
 * 
 * This file is part of Libra.
 *
 * WeightWatch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * WeightWatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the WeightWatch source code. If not, see: http://www.gnu.org/licenses
 */

package net.cachapa.remotegallery.util;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "RemoteGallery";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "'Servers'";
	
	private static Database instance = null;
//	private Context context;
	private ServerConf cachedServerConf;
	
	protected Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		this.context = context;
	}
	
	public static Database getInstance(Context context) {
		if (instance == null) {
			instance = new Database(context);
		}
		return instance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name STRING, "
				+ "address STRING, "
				+ "port int, "
				+ "username STRING, "
				+ "key_path STRING, "
				+ "remote_path STRING"
				+ ");");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	public void insertServerConf(ServerConf serverConf) {
		SQLiteDatabase db = getWritableDatabase();
		
		ContentValues cv = new ContentValues();
		cv.put("name", serverConf.name);
		cv.put("address", serverConf.address);
		cv.put("port", serverConf.port);
		cv.put("username", serverConf.username);
		cv.put("key_path", serverConf.keyPath);
		cv.put("remote_path", serverConf.remotePath);
		
		try {
			db.insert(TABLE_NAME, "0", cv);
		} catch (SQLiteConstraintException e) {
			// The value already exists on the database
		}
	}
	
	public void updateServerConf(ServerConf serverConf) {
		cachedServerConf = null;
		SQLiteDatabase db = getWritableDatabase();
		
		ContentValues cv = new ContentValues();
		cv.put("name", serverConf.name);
		cv.put("address", serverConf.address);
		cv.put("port", serverConf.port);
		cv.put("username", serverConf.username);
		cv.put("key_path", serverConf.keyPath);
		cv.put("remote_path", serverConf.remotePath);
		
		db.update(TABLE_NAME, cv, "id = " + serverConf.id, null);
	}
	
	public ArrayList<ServerConf> getAllServerConfs() {
		SQLiteDatabase db = getReadableDatabase();
		ArrayList<ServerConf> serverConfs = new ArrayList<ServerConf>();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null,
				null, "name ASC");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			serverConfs.add(readServerConf(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		
		return serverConfs;
	}
	
	public ServerConf getServerConf(long id) {
		if (cachedServerConf != null && cachedServerConf.id == id) {
			return cachedServerConf;
		}
		
		SQLiteDatabase db = getReadableDatabase();
		String selection = "id = " + id;
		Cursor cursor = db.query(TABLE_NAME, null, selection, null, null, null, null, "1");
		if (cursor.getCount() != 0) {
			cursor.moveToFirst();
			cachedServerConf = readServerConf(cursor);
		}
		cursor.close();
		return cachedServerConf;
	}
	
	private ServerConf readServerConf(Cursor cursor) {
		String name = cursor.getString(1);
		return new ServerConf(
				cursor.getLong(0),		// id
				name,					// name
				cursor.getString(2),	// address
				cursor.getInt(3),		// port
				cursor.getString(4),	// username
				cursor.getString(5),	// key path
				cursor.getString(6)		// remote path
				);
	}
	
	public void deleteServerConf(ServerConf serverConf) {
		cachedServerConf = null;
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE id = " + serverConf.id);
	}
}
