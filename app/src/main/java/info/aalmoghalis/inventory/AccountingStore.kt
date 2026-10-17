package info.aalmoghalis.inventory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AccountRecord(val id: Long, val name: String, val category: String, val phone: String)
data class ItemRecord(val id: Long, val name: String, val code: String, val quantity: String, val price: String, val category: String, val unit: String, val costPrice: String)

class AccountingStore(context: Context) : SQLiteOpenHelper(context, "muhasib_rebuild.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE account_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)")
        createAccountsTable(db)
        db.execSQL("CREATE INDEX accounts_name_idx ON accounts(name)")
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT '', unit TEXT NOT NULL DEFAULT '', quantity TEXT NOT NULL DEFAULT '0', price TEXT NOT NULL DEFAULT '0', cost_price TEXT NOT NULL DEFAULT '0', active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX items_name_idx ON items(name)")
        db.execSQL("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT NOT NULL, type TEXT NOT NULL, account_id INTEGER, date TEXT NOT NULL, total TEXT NOT NULL DEFAULT '0', status TEXT NOT NULL DEFAULT 'draft')")
        db.execSQL("CREATE TABLE invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL, item_id INTEGER, item_name TEXT NOT NULL, quantity TEXT NOT NULL, unit_price TEXT NOT NULL, total TEXT NOT NULL)")
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, date TEXT NOT NULL, account_id INTEGER, debit TEXT NOT NULL DEFAULT '0', credit TEXT NOT NULL DEFAULT '0', note TEXT NOT NULL DEFAULT '')")
    }

    private fun createAccountsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category_id INTEGER NOT NULL, phone TEXT NOT NULL DEFAULT '', active INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(category_id) REFERENCES account_categories(id))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP INDEX IF EXISTS accounts_name_idx")
            db.execSQL("ALTER TABLE accounts RENAME TO accounts_old")
            createAccountsTable(db)
            db.execSQL("INSERT OR IGNORE INTO account_categories(name) SELECT DISTINCT category FROM accounts_old WHERE TRIM(category) <> ''")
            db.execSQL("INSERT INTO accounts(id,name,category_id,phone,active) SELECT a.id,a.name,c.id,a.phone,a.active FROM accounts_old a JOIN account_categories c ON c.name=a.category")
            db.execSQL("DROP TABLE accounts_old")
            db.execSQL("CREATE INDEX accounts_name_idx ON accounts(name)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE items ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE items ADD COLUMN unit TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE items ADD COLUMN cost_price TEXT NOT NULL DEFAULT '0'")
        }
    }

    fun addAccount(name: String, category: String, phone: String = ""): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "اسم الحساب مطلوب" }
        require(category in ACCOUNT_CATEGORIES) { "تصنيف الحساب غير صالح" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("INSERT OR IGNORE INTO account_categories(name) VALUES(?)", arrayOf(category))
            val categoryId = db.rawQuery("SELECT id FROM account_categories WHERE name = ?", arrayOf(category)).use { cursor ->
                check(cursor.moveToFirst()) { "تعذر تحديد تصنيف الحساب" }
                cursor.getLong(0)
            }
            val values = ContentValues().apply {
                put("name", cleanName)
                put("category_id", categoryId)
                put("phone", phone.trim())
            }
            val id = db.insertOrThrow("accounts", null, values)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun searchAccounts(prefix: String, category: String? = null): List<AccountRecord> {
        val rows = mutableListOf<AccountRecord>()
        readableDatabase.rawQuery(
            "SELECT a.id,a.name,c.name,a.phone FROM accounts a JOIN account_categories c ON c.id=a.category_id WHERE a.active=1 AND a.name LIKE ? AND (? = '' OR c.name = ?) ORDER BY a.name COLLATE NOCASE LIMIT 30",
            arrayOf(prefix.trim() + "%", category.orEmpty(), category.orEmpty())
        ).use { cursor ->
            while (cursor.moveToNext()) rows += AccountRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3))
        }
        return rows
    }

    fun addItem(name: String, code: String = "", quantity: String = "0", price: String = "0", category: String = "", unit: String = "", costPrice: String = "0"): Long {
        require(name.isNotBlank()) { "اسم الصنف مطلوب" }
        val values = ContentValues().apply {
            put("name", name.trim())
            put("code", code.trim())
            put("category", category.trim())
            put("unit", unit.trim())
            put("quantity", quantity.trim().ifEmpty { "0" })
            put("price", price.trim().ifEmpty { "0" })
            put("cost_price", costPrice.trim().ifEmpty { "0" })
        }
        return writableDatabase.insertOrThrow("items", null, values)
    }

    fun searchItems(prefix: String): List<ItemRecord> {
        val rows = mutableListOf<ItemRecord>()
        readableDatabase.rawQuery(
            "SELECT id,name,code,quantity,price,category,unit,cost_price FROM items WHERE active=1 AND (name LIKE ? OR code LIKE ?) ORDER BY name COLLATE NOCASE LIMIT 30",
            arrayOf(prefix.trim() + "%", prefix.trim() + "%")
        ).use { cursor ->
            while (cursor.moveToNext()) rows += ItemRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7))
        }
        return rows
    }

    companion object {
        val ACCOUNT_CATEGORIES = listOf("العملاء", "الموردون", "الأصول", "الصناديق", "البنوك", "المصروفات", "الإيرادات", "رأس المال", "أخرى")
    }
}
