package info.aalmoghalis.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val CategoryNames = listOf("العملاء", "الموردون", "الأصول", "الصناديق", "البنوك", "المصروفات", "الإيرادات", "رأس المال", "أخرى")

@Composable
fun AccountManagementScreen() {
    val context = LocalContext.current
    val store = remember { AccountingStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CategoryNames.first()) }
    var categoryFilter by remember { mutableStateOf(CategoryNames.first()) }
    var showForm by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var pendingDisable by remember { mutableStateOf<AccountRecord?>(null) }
    var editingAccount by remember { mutableStateOf<AccountRecord?>(null) }
    val records = remember(query, refresh, categoryFilter) { store.searchAccounts(query, categoryFilter) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("دليل الحسابات", fontWeight = FontWeight.Bold)
            Button(onClick = { editingAccount = null; name = ""; phone = ""; category = categoryFilter; showForm = true }) { Text("إضافة حساب") }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("بحث باسم الحساب") }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(CategoryNames) { label ->
                TextButton(onClick = { categoryFilter = label }) {
                    Text(if (categoryFilter == label) "✓ $label" else label, color = if (categoryFilter == label) Color(0xFFA67C00) else Color(0xFF64748B))
                }
            }
        }
        Text("يعرض هذا التبويب حسابات التصنيف المحدد فقط.", color = Color(0xFF64748B))
        if (records.isEmpty()) Text(if (query.isBlank()) "لا توجد حسابات محفوظة بعد." else "لا توجد نتائج تبدأ بهذه الأحرف.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { account ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(account.category, color = Color(0xFFA67C00))
                        if (account.phone.isNotBlank()) Text(account.phone, color = Color(0xFF64748B))
                        Row {
                            TextButton(onClick = { editingAccount = account; name = account.name; phone = account.phone; category = account.category; showForm = true }) { Text("تعديل") }
                            TextButton(onClick = { pendingDisable = account }) { Text("تعطيل الحساب") }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingAccount == null) "إضافة حساب جديد" else "تعديل الحساب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("اسم الحساب") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text("رقم الهاتف") }, singleLine = true)
                    Text("التصنيف: $category")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { category = CategoryNames[(CategoryNames.indexOf(category) + CategoryNames.size - 1) % CategoryNames.size] }) { Text("السابق") }
                        TextButton(onClick = { category = CategoryNames[(CategoryNames.indexOf(category) + 1) % CategoryNames.size] }) { Text("التالي") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val current = editingAccount
                        if (current == null) store.addAccount(name, category, phone) else store.updateAccount(current.id, name, category, phone)
                        editingAccount = null
                        query = ""
                        refresh++
                        showForm = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("إلغاء") } }
        )
    }
    if (pendingDisable != null) {
        AlertDialog(
            onDismissRequest = { pendingDisable = null },
            title = { Text("تعطيل الحساب؟") },
            text = { Text("سيبقى السجل محفوظاً ولن يظهر في القائمة النشطة.") },
            confirmButton = {
                Button(onClick = {
                    pendingDisable?.let { store.deactivateAccount(it.id) }
                    pendingDisable = null
                    refresh++
                }) { Text("تأكيد التعطيل") }
            },
            dismissButton = { TextButton(onClick = { pendingDisable = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun ItemManagementScreen() {
    val context = LocalContext.current
    val store = remember { AccountingStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("0") }
    var price by remember { mutableStateOf("0") }
    var costPrice by remember { mutableStateOf("0") }
    var itemCategory by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var pendingDisable by remember { mutableStateOf<ItemRecord?>(null) }
    var editingItem by remember { mutableStateOf<ItemRecord?>(null) }
    val records = remember(query, refresh) { store.searchItems(query) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الأصناف والمخزون", fontWeight = FontWeight.Bold)
            Button(onClick = { editingItem = null; name = ""; code = ""; quantity = "0"; price = "0"; costPrice = "0"; itemCategory = ""; unit = ""; showForm = true }) { Text("إضافة صنف") }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("بحث باسم الصنف أو رمزه") }, singleLine = true)
        if (records.isEmpty()) Text(if (query.isBlank()) "لا توجد أصناف محفوظة بعد." else "لا توجد نتائج تبدأ بهذه الأحرف.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text("التصنيف: ${item.category.ifBlank { "عام" }}   |   الوحدة: ${item.unit.ifBlank { "—" }}", color = Color(0xFF64748B))
                        Text("الرمز: ${item.code.ifBlank { "—" }}   |   الكمية: ${item.quantity}   |   البيع: ${item.price}   |   التكلفة: ${item.costPrice}", color = Color(0xFF64748B))
                        Row {
                            TextButton(onClick = { editingItem = item; name = item.name; code = item.code; quantity = item.quantity; price = item.price; costPrice = item.costPrice; itemCategory = item.category; unit = item.unit; showForm = true }) { Text("تعديل") }
                            TextButton(onClick = { pendingDisable = item }) { Text("تعطيل الصنف") }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingItem == null) "إضافة صنف جديد" else "تعديل الصنف") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("اسم الصنف") }, singleLine = true)
                    OutlinedTextField(code, { code = it }, label = { Text("الرمز / الباركود") }, singleLine = true)
                    OutlinedTextField(itemCategory, { itemCategory = it }, label = { Text("تصنيف الصنف") }, singleLine = true)
                    OutlinedTextField(unit, { unit = it }, label = { Text("وحدة القياس") }, singleLine = true)
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("الكمية الافتتاحية") }, singleLine = true)
                    OutlinedTextField(price, { price = it }, label = { Text("سعر البيع") }, singleLine = true)
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text("سعر التكلفة") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val current = editingItem
                        if (current == null) store.addItem(name, code, quantity, price, itemCategory, unit, costPrice)
                        else store.updateItem(current.id, name, code, quantity, price, itemCategory, unit, costPrice)
                        editingItem = null
                        query = ""
                        refresh++
                        showForm = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("إلغاء") } }
        )
    }
    if (pendingDisable != null) {
        AlertDialog(
            onDismissRequest = { pendingDisable = null },
            title = { Text("تعطيل الصنف؟") },
            text = { Text("سيبقى السجل محفوظاً ولن يظهر في قائمة المخزون النشطة.") },
            confirmButton = {
                Button(onClick = {
                    pendingDisable?.let { store.deactivateItem(it.id) }
                    pendingDisable = null
                    refresh++
                }) { Text("تأكيد التعطيل") }
            },
            dismissButton = { TextButton(onClick = { pendingDisable = null }) { Text("إلغاء") } }
        )
    }
}
