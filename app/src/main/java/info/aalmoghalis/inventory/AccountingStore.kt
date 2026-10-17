package info.aalmoghalis.inventory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AccountRecord(val id: Long, val name: String, val category: String, val phone: String)
data class ItemRecord(val id: Long, val name: String, val code: String, val quantity: String, val price: String)

class AccountingStore(context: Context) : SQLiteOpenHelper(context, "muhasib_rebuild.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE account_categories (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX accounts_name_idx ON accounts(name)")
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL DEFAULT '', quantity TEXT NOT NULL DEFAULT '0', price TEXT NOT NULL DEFAULT '0', active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX items_name_idx ON items(name)")
        db.execSQL("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT NOT NULL, type TEXT NOT NULL, account_id INTEGER, date TEXT NOT NULL, total TEXT NOT NULL DEFAULT '0', status TEXT NOT NULL DEFAULT 'draft')")
        db.execSQL("CREATE TABLE invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL, item_id INTEGER, item_name TEXT NOT NULL, quantity TEXT NOT NULL, unit_price TEXT NOT NULL, total TEXT NOT NULL)")
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, date TEXT NOT NULL, account_id INTEGER, debit TEXT NOT NULL DEFAULT '0', credit TEXT NOT NULL DEFAULT '0', note TEXT NOT NULL DEFAULT '')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Keep existing records; schema changes must use explicit migrations.
    }

    fun addAccount(name: String, category: String, phone: String = ""): Long {
        require(name.isNotBlank()) { "اسم الحساب مطلوب" }
        val values = ContentValues().apply {
            put("name", name.trim())
            put("category", category)
            put("phone", phone.trim())
        }
        return writableDatabase.insertOrThrow("accounts", null, values)
    }

    fun searchAccounts(prefix: String): List<AccountRecord> {
        val rows = mutableListOf<AccountRecord>()
        readableDatabase.rawQuery(
            "SELECT id,name,category,phone FROM accounts WHERE active=1 AND name LIKE ? ORDER BY name LIMIT 30",
            arrayOf(prefix.trim() + "%")
        ).use { cursor ->
            while (cursor.moveToNext()) rows += AccountRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3))
        }
        return rows
    }

    fun addItem(name: String, code: String = "", quantity: String = "0", price: String = "0"): Long {
        require(name.isNotBlank()) { "اسم الصنف مطلوب" }
        val values = ContentValues().apply {
            put("name", name.trim())
            put("code", code.trim())
            put("quantity", quantity)
            put("price", price)
        }
        return writableDatabase.insertOrThrow("items", null, values)
    }

    fun searchItems(prefix: String): List<ItemRecord> {
        val rows = mutableListOf<ItemRecord>()
        readableDatabase.rawQuery(
            "SELECT id,name,code,quantity,price FROM items WHERE active=1 AND (name LIKE ? OR code LIKE ?) ORDER BY name LIMIT 30",
            arrayOf(prefix.trim() + "%", prefix.trim() + "%")
        ).use { cursor ->
            while (cursor.moveToNext()) rows += ItemRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4))
        }
        return rows
    }
}
