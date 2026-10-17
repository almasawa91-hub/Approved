package info.aalmoghalis.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun JournalScreen(kind: String = "قيد يومي") {
    val context = LocalContext.current
    val store = remember { AccountingStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    val accounts = remember { store.listActiveAccounts() }
    var debit by remember { mutableStateOf<AccountRecord?>(null) }
    var credit by remember { mutableStateOf<AccountRecord?>(null) }
    var pickDebit by remember { mutableStateOf(false) }
    var pickCredit by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val entries = remember(refresh, kind) { store.searchJournalEntries(kind) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(when (kind) { "سند قبض" -> "سندات القبض"; "سند صرف" -> "سندات الصرف"; else -> "القيود اليومية" }, fontWeight = FontWeight.Bold)
        OutlinedTextField(date, { date = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("التاريخ (YYYY-MM-DD)") }, singleLine = true)
        Button(onClick = { pickDebit = true }, modifier = Modifier.fillMaxWidth()) {
            Text("الحساب المدين: ${debit?.name ?: "اختر حساباً"}")
        }
        Button(onClick = { pickCredit = true }, modifier = Modifier.fillMaxWidth()) {
            Text("الحساب الدائن: ${credit?.name ?: "اختر حساباً"}")
        }
        OutlinedTextField(amount, { amount = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("المبلغ") }, singleLine = true)
        OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("البيان") }, singleLine = true)
        if (error.isNotBlank()) Text(error, color = Color(0xFFDC2626))
        Button(onClick = {
            try {
                val d = debit ?: throw IllegalArgumentException("اختر الحساب المدين")
                val c = credit ?: throw IllegalArgumentException("اختر الحساب الدائن")
                store.postBalancedJournal(date, d.id, c.id, amount, description, kind)
                date = ""; amount = ""; description = ""; debit = null; credit = null
                error = ""; refresh++
            } catch (e: Exception) {
                error = e.message ?: "تعذر حفظ القيد"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (kind == "قيد يومي") "حفظ القيد المتوازن" else "حفظ السند") }
        Text("يُحفظ السجل فقط إذا كان المبلغ صالحاً والحسابان مختلفين؛ ويُسجل المدين والدائن بالقيمة نفسها.", color = Color(0xFF64748B))
        Text(if (kind == "قيد يومي") "القيود المحفوظة" else "السندات المحفوظة", fontWeight = FontWeight.Bold)
        if (entries.isEmpty()) Text("لا توجد قيود يومية بعد.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.take(30).forEach { entry ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${entry.debitAccount} ← ${entry.creditAccount}", fontWeight = FontWeight.Bold)
                        Text("التاريخ: ${entry.date}   |   المبلغ: ${entry.amount}", color = Color(0xFF64748B))
                        if (entry.description.isNotBlank()) Text(entry.description, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }

    if (pickDebit) AccountPickerDialog("اختر الحساب المدين", accounts, { pickDebit = false }) {
        debit = it; pickDebit = false
    }
    if (pickCredit) AccountPickerDialog("اختر الحساب الدائن", accounts, { pickCredit = false }) {
        credit = it; pickCredit = false
    }
}

@Composable
private fun AccountPickerDialog(
    title: String,
    accounts: List<AccountRecord>,
    onDismiss: () -> Unit,
    onSelect: (AccountRecord) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (accounts.isEmpty()) Text("أضف حساباً أولاً من دليل الحسابات.")
            else LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(accounts, key = { it.id }) { account ->
                    TextButton(onClick = { onSelect(account) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(account.name)
                            Text(account.category, color = Color(0xFFA67C00))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
