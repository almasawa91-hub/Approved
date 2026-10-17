package info.aalmoghalis.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InvoiceManagementScreen(type: String) {
    val context = LocalContext.current
    val store = remember { AccountingStore(context) }
    var query by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("0") }
    var showForm by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    val records = remember(query, refresh, type) { store.searchInvoices(type, query) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (type == "بيع") "فواتير البيع" else "فواتير الشراء", fontWeight = FontWeight.Bold)
                Text("تُحفظ حالياً كمسودات محلية.", color = Color(0xFF64748B))
            }
            Button(onClick = { number = ""; date = ""; total = "0"; error = ""; showForm = true }) { Text("فاتورة جديدة") }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("بحث برقم الفاتورة") }, singleLine = true)
        if (records.isEmpty()) Text(if (query.isBlank()) "لا توجد فواتير محفوظة بعد." else "لا توجد فواتير بهذا الرقم.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { invoice ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("فاتورة رقم ${invoice.number}", fontWeight = FontWeight.Bold)
                        Text("التاريخ: ${invoice.date}   |   الإجمالي: ${invoice.total}", color = Color(0xFF64748B))
                        Text(invoice.status, color = Color(0xFFA67C00))
                    }
                }
            }
        }
        Text("تنبيه: هذه المرحلة تحفظ رأس الفاتورة كمسودة فقط؛ لا تُرحّل قيوداً ولا تغيّر المخزون.", color = Color(0xFF64748B))
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (type == "بيع") "فاتورة بيع جديدة" else "فاتورة شراء جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(number, { number = it; error = "" }, label = { Text("رقم الفاتورة") }, singleLine = true)
                    OutlinedTextField(date, { date = it; error = "" }, label = { Text("التاريخ (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(total, { total = it; error = "" }, label = { Text("الإجمالي") }, singleLine = true)
                    if (error.isNotBlank()) Text(error, color = Color(0xFFDC2626))
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        store.addInvoiceDraft(number, type, date, total)
                        query = ""
                        refresh++
                        showForm = false
                    } catch (e: IllegalArgumentException) {
                        error = e.message ?: "تحقق من البيانات المدخلة"
                    } catch (_: Exception) {
                        error = "تعذر حفظ الفاتورة. تحقق من رقمها وبياناتها."
                    }
                }) { Text("حفظ كمسودة") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("إلغاء") } }
        )
    }
}
