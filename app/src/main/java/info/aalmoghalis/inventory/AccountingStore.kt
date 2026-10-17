package info.aalmoghalis.inventory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.math.BigDecimal

data class AccountRecord(val id: Long, val name: String, val category: String, val phone: String)
data class ItemRecord(val id: Long, val name: String, val code: String, val quantity: String, val price: String, val category: String, val unit: String, val costPrice: String)
data class InvoiceRecord(val id: Long, val number: String, val type: String, val date: String, val total: String, val status: String)
data class InvoiceLineRecord(val itemName: String, val quantity: String, val unitPrice: String, val total: String)
data class InvoiceDetails(val invoice: InvoiceRecord, val lines: List<InvoiceLineRecord>)
data class InvoiceLineDraft(val itemId: Long, val itemName: String, val quantity: String, val unitPrice: String)
data class LocalCounts(val accounts: Int, val items: Int, val salesDrafts: Int, val purchaseDrafts: Int, val journalEntries: Int)
data class JournalRecord(val id: Long, val kind: String, val date: String, val debitAccount: String, val creditAccount: String, val amount: String, val description: String)
data class AccountBalance(val id: Long, val name: String, val category: String, val debit: String, val credit: String)
private data class MutableAccountBalance(val id: Long, val name: String, val category: String, var debit: BigDecimal = BigDecimal.ZERO, var credit: BigDecimal = BigDecimal.ZERO)

class AccountingStore(context: Context) : SQLiteOpenHelper(context, "muhasib_rebuild.db", null, 5) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE account_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)")
        createAccountsTable(db)
        db.execSQL("CREATE INDEX accounts_name_idx ON accounts(name)")
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT '', unit TEXT NOT NULL DEFAULT '', quantity TEXT NOT NULL DEFAULT '0', price TEXT NOT NULL DEFAULT '0', cost_price TEXT NOT NULL DEFAULT '0', active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX items_name_idx ON items(name)")
        db.execSQL("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT NOT NULL, type TEXT NOT NULL, account_id INTEGER, date TEXT NOT NULL, total TEXT NOT NULL DEFAULT '0', status TEXT NOT NULL DEFAULT 'draft')")
        db.execSQL("CREATE TABLE invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL, item_id INTEGER, item_name TEXT NOT NULL, quantity TEXT NOT NULL, unit_price TEXT NOT NULL, total TEXT NOT NULL)")
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, date TEXT NOT NULL, account_id INTEGER, debit TEXT NOT NULL DEFAULT '0', credit TEXT NOT NULL DEFAULT '0', note TEXT NOT NULL DEFAULT '')")
        createJournalTables(db)
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
        if (oldVersion < 4) createJournalTables(db)
        if (oldVersion == 4) db.execSQL("ALTER TABLE journal_entries ADD COLUMN kind TEXT NOT NULL DEFAULT 'قيد يومي'")
    }

    private fun createJournalTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL DEFAULT 'قيد يومي', date TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_id INTEGER NOT NULL, account_id INTEGER NOT NULL, side TEXT NOT NULL, amount TEXT NOT NULL, FOREIGN KEY(entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE, FOREIGN KEY(account_id) REFERENCES accounts(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS journal_lines_entry_idx ON journal_lines(entry_id)")
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

    fun addInvoiceDraft(number: String, type: String, date: String, lines: List<InvoiceLineDraft>): Long {
        require(number.isNotBlank()) { "رقم الفاتورة مطلوب" }
        require(type == "بيع" || type == "شراء") { "نوع الفاتورة غير صالح" }
        require(date.isNotBlank()) { "تاريخ الفاتورة مطلوب" }
        require(lines.isNotEmpty()) { "أضف صنفاً واحداً على الأقل" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            var total = BigDecimal.ZERO
            val prepared = lines.map { line ->
                val qty = BigDecimal(line.quantity)
                val price = BigDecimal(line.unitPrice)
                require(qty > BigDecimal.ZERO && price >= BigDecimal.ZERO)
                total = total.add(qty.multiply(price))
                Triple(line, qty, price)
            }
            val header = ContentValues().apply {
                put("number", number.trim()); put("type", type); put("date", date.trim())
                put("total", total.stripTrailingZeros().toPlainString()); put("status", "مسودة")
            }
            val id = db.insertOrThrow("invoices", null, header)
            prepared.forEach { (line, qty, price) ->
                val values = ContentValues().apply {
                    put("invoice_id", id); put("item_id", line.itemId); put("item_name", line.itemName)
                    put("quantity", qty.stripTrailingZeros().toPlainString())
                    put("unit_price", price.stripTrailingZeros().toPlainString())
                    put("total", qty.multiply(price).stripTrailingZeros().toPlainString())
                }
                db.insertOrThrow("invoice_lines", null, values)
            }
            db.setTransactionSuccessful()
            return id
        } finally { db.endTransaction() }
    }

    fun getInvoiceDetails(invoiceId: Long): InvoiceDetails? {
        val invoice = readableDatabase.rawQuery(
            "SELECT id,number,type,date,total,status FROM invoices WHERE id=?",
            arrayOf(invoiceId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else InvoiceRecord(
                cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getString(5)
            )
        } ?: return null
        val lines = mutableListOf<InvoiceLineRecord>()
        readableDatabase.rawQuery(
            "SELECT item_name,quantity,unit_price,total FROM invoice_lines WHERE invoice_id=? ORDER BY id",
            arrayOf(invoiceId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) lines += InvoiceLineRecord(
                cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)
            )
        }
        return InvoiceDetails(invoice, lines)
    }

    fun searchInvoices(type: String, prefix: String = ""): List<InvoiceRecord> {
        val rows = mutableListOf<InvoiceRecord>()
        readableDatabase.rawQuery(
            "SELECT id,number,type,date,total,status FROM invoices WHERE type=? AND number LIKE ? ORDER BY id DESC LIMIT 100",
            arrayOf(type, prefix.trim() + "%")
        ).use { cursor ->
            while (cursor.moveToNext()) rows += InvoiceRecord(
                cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getString(5)
            )
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

    fun updateAccount(id: Long, name: String, category: String, phone: String): Int {
        require(name.isNotBlank()) { "اسم الحساب مطلوب" }
        require(category in ACCOUNT_CATEGORIES) { "تصنيف الحساب غير صالح" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("INSERT OR IGNORE INTO account_categories(name) VALUES(?)", arrayOf(category))
            val categoryId = db.rawQuery("SELECT id FROM account_categories WHERE name = ?", arrayOf(category)).use { cursor ->
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
            val values = ContentValues().apply {
                put("name", name.trim()); put("category_id", categoryId); put("phone", phone.trim())
            }
            val changed = db.update("accounts", values, "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            return changed
        } finally { db.endTransaction() }
    }

    fun updateItem(id: Long, name: String, code: String, quantity: String, price: String, category: String, unit: String, costPrice: String): Int {
        require(name.isNotBlank()) { "اسم الصنف مطلوب" }
        val values = ContentValues().apply {
            put("name", name.trim()); put("code", code.trim()); put("quantity", quantity.trim().ifEmpty { "0" })
            put("price", price.trim().ifEmpty { "0" }); put("category", category.trim())
            put("unit", unit.trim()); put("cost_price", costPrice.trim().ifEmpty { "0" })
        }
        return writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun listActiveAccounts(): List<AccountRecord> {
        val rows = mutableListOf<AccountRecord>()
        readableDatabase.rawQuery(
            "SELECT a.id,a.name,c.name,a.phone FROM accounts a JOIN account_categories c ON c.id=a.category_id WHERE a.active=1 ORDER BY a.name COLLATE NOCASE LIMIT 500",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) rows += AccountRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3))
        }
        return rows
    }

    fun postBalancedJournal(date: String, debitAccountId: Long, creditAccountId: Long, amountText: String, description: String, kind: String = "قيد يومي"): Long {
        require(date.isNotBlank()) { "التاريخ مطلوب" }
        require(debitAccountId != creditAccountId) { "اختر حسابين مختلفين" }
        val amount = try { BigDecimal(amountText.trim()) } catch (_: Exception) {
            throw IllegalArgumentException("المبلغ غير صالح")
        }
        require(amount > BigDecimal.ZERO) { "المبلغ يجب أن يكون أكبر من صفر" }
        val normalizedAmount = amount.stripTrailingZeros().toPlainString()
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf(debitAccountId, creditAccountId).forEach { id ->
                db.rawQuery("SELECT id FROM accounts WHERE id=? AND active=1", arrayOf(id.toString())).use { cursor ->
                    require(cursor.moveToFirst()) { "الحساب غير موجود أو معطل" }
                }
            }
            val header = ContentValues().apply {
                put("kind", kind)
                put("date", date.trim())
                put("description", description.trim())
                put("created_at", System.currentTimeMillis())
            }
            val entryId = db.insertOrThrow("journal_entries", null, header)
            listOf(debitAccountId to "مدين", creditAccountId to "دائن").forEach { line ->
                val values = ContentValues().apply {
                    put("entry_id", entryId)
                    put("account_id", line.first)
                    put("side", line.second)
                    put("amount", normalizedAmount)
                }
                db.insertOrThrow("journal_lines", null, values)
            }
            db.setTransactionSuccessful()
            return entryId
        } finally { db.endTransaction() }
    }

    fun searchJournalEntries(kind: String? = null): List<JournalRecord> {
        val rows = mutableListOf<JournalRecord>()
        readableDatabase.rawQuery(
            "SELECT j.id,j.kind,j.date,a1.name,a2.name,l1.amount,j.description FROM journal_entries j JOIN journal_lines l1 ON l1.entry_id=j.id AND l1.side='مدين' JOIN accounts a1 ON a1.id=l1.account_id JOIN journal_lines l2 ON l2.entry_id=j.id AND l2.side='دائن' JOIN accounts a2 ON a2.id=l2.account_id WHERE (? = '' OR j.kind = ?) ORDER BY j.id DESC LIMIT 100",
            arrayOf(kind.orEmpty(), kind.orEmpty())
        ).use { cursor ->
            while (cursor.moveToNext()) rows += JournalRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6))
        }
        return rows
    }

    fun getTrialBalance(): List<AccountBalance> {
        val totals = linkedMapOf<Long, MutableAccountBalance>()
        readableDatabase.rawQuery(
            "SELECT a.id,a.name,c.name,l.side,l.amount FROM journal_lines l JOIN accounts a ON a.id=l.account_id JOIN account_categories c ON c.id=a.category_id ORDER BY a.name COLLATE NOCASE",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val row = totals.getOrPut(id) { MutableAccountBalance(id, cursor.getString(1), cursor.getString(2)) }
                val amount = BigDecimal(cursor.getString(4))
                if (cursor.getString(3) == "مدين") row.debit = row.debit.add(amount)
                else row.credit = row.credit.add(amount)
            }
        }
        return totals.values.map { row ->
            AccountBalance(row.id, row.name, row.category, row.debit.stripTrailingZeros().toPlainString(), row.credit.stripTrailingZeros().toPlainString())
        }
    }

    fun deactivateAccount(id: Long): Int {
        val values = ContentValues().apply { put("active", 0) }
        return writableDatabase.update("accounts", values, "id = ?", arrayOf(id.toString()))
    }

    fun deactivateItem(id: Long): Int {
        val values = ContentValues().apply { put("active", 0) }
        return writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun getCounts(): LocalCounts {
        fun count(table: String, where: String = ""): Int {
            val sql = if (where.isBlank()) "SELECT COUNT(*) FROM $table" else "SELECT COUNT(*) FROM $table WHERE $where"
            readableDatabase.rawQuery(sql, null).use { cursor ->
                return if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
        return LocalCounts(
            count("accounts", "active=1"),
            count("items", "active=1"),
            count("invoices", "type='بيع' AND status='مسودة'"),
            count("invoices", "type='شراء' AND status='مسودة'"),
            count("journal_entries")
        )
    }

    companion object {
        val ACCOUNT_CATEGORIES = listOf("العملاء", "الموردون", "الأصول", "الصناديق", "البنوك", "المصروفات", "الإيرادات", "رأس المال", "أخرى")
    }
}
