package info.aalmoghalis.inventory

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.DisposableEffect
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
import java.math.BigDecimal

@Composable
fun InvoiceManagementScreen(type: String) {
    val context = LocalContext.current
    val store = remember { AccountingStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    var query by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var itemQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("0") }
    var selectedItem by remember { mutableStateOf<ItemRecord?>(null) }
    var lines by remember { mutableStateOf(listOf<InvoiceLineDraft>()) }
    var showForm by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    val records = remember(query, refresh, type) { store.searchInvoices(type, query) }
    var pdfInvoiceId by remember { mutableStateOf<Long?>(null) }
    var pdfStatus by remember { mutableStateOf("") }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val id = pdfInvoiceId
        if (uri != null && id != null) {
            try {
                val details = store.getInvoiceDetails(id) ?: throw IllegalStateException("الفاتورة غير موجودة")
                writeInvoicePdf(context, uri, details)
                pdfStatus = "تم تصدير الفاتورة إلى PDF."
            } catch (e: Exception) {
                pdfStatus = e.message ?: "تعذر إنشاء ملف PDF."
            }
        }
        pdfInvoiceId = null
    }
    val suggestions = remember(itemQuery, selectedItem) {
        if (itemQuery.isBlank() || selectedItem != null) emptyList() else store.searchItems(itemQuery).take(5)
    }
    val totalPreview = lines.fold(BigDecimal.ZERO) { sum, line ->
        sum.add(BigDecimal(line.quantity).multiply(BigDecimal(line.unitPrice)))
    }.stripTrailingZeros().toPlainString()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (type == "بيع") "فواتير البيع" else "فواتير الشراء", fontWeight = FontWeight.Bold)
                Text("المسودات المحفوظة محلياً", color = Color(0xFF64748B))
            }
            Button(onClick = {
                number = ""; date = ""; itemQuery = ""; quantity = "1"; unitPrice = "0"
                selectedItem = null; lines = emptyList(); error = ""; showForm = true
            }) { Text("فاتورة جديدة") }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("بحث برقم الفاتورة") }, singleLine = true)
        if (pdfStatus.isNotBlank()) Text(pdfStatus, color = Color(0xFF64748B))
        if (records.isEmpty()) Text(if (query.isBlank()) "لا توجد فواتير محفوظة بعد." else "لا توجد فواتير بهذا الرقم.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { invoice ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("فاتورة رقم ${invoice.number}", fontWeight = FontWeight.Bold)
                        Text("التاريخ: ${invoice.date}   |   الإجمالي: ${invoice.total}", color = Color(0xFF64748B))
                        Text(invoice.status, color = Color(0xFFA67C00))
                        TextButton(onClick = {
                            pdfInvoiceId = invoice.id
                            pdfLauncher.launch("invoice-${invoice.number}.pdf")
                        }) { Text("تصدير PDF") }
                    }
                }
            }
        }
        Text("الفواتير تحفظ كمسودات؛ لا تُرحّل قيوداً محاسبية ولا تغيّر المخزون في هذه المرحلة.", color = Color(0xFF64748B))
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (type == "بيع") "فاتورة بيع جديدة" else "فاتورة شراء جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(number, { number = it; error = "" }, label = { Text("رقم الفاتورة") }, singleLine = true)
                    OutlinedTextField(date, { date = it; error = "" }, label = { Text("التاريخ (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(itemQuery, {
                        itemQuery = it
                        selectedItem = null
                        error = ""
                    }, label = { Text("اكتب بداية اسم الصنف للبحث") }, singleLine = true)
                    if (suggestions.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(suggestions, key = { it.id }) { item ->
                                TextButton(onClick = {
                                    selectedItem = item
                                    itemQuery = item.name
                                    unitPrice = item.price
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("${item.name}  •  ${item.price}")
                                }
                            }
                        }
                    }
                    OutlinedTextField(quantity, { quantity = it; error = "" }, label = { Text("الكمية") }, singleLine = true)
                    OutlinedTextField(unitPrice, { unitPrice = it; error = "" }, label = { Text("سعر الوحدة") }, singleLine = true)
                    Button(onClick = {
                        val item = selectedItem
                        try {
                            require(item != null) { "اختر صنفاً من نتائج البحث" }
                            val qty = BigDecimal(quantity)
                            val price = BigDecimal(unitPrice)
                            require(qty > BigDecimal.ZERO && price >= BigDecimal.ZERO) { "تحقق من الكمية والسعر" }
                            lines = lines + InvoiceLineDraft(item.id, item.name, qty.toPlainString(), price.toPlainString())
                            itemQuery = ""; quantity = "1"; unitPrice = "0"; selectedItem = null; error = ""
                        } catch (e: Exception) {
                            error = e.message ?: "تعذر إضافة الصنف"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("إضافة الصنف إلى الفاتورة") }
                    if (lines.isNotEmpty()) {
                        Text("الأصناف المضافة: ${lines.size}", fontWeight = FontWeight.Bold)
                        lines.forEachIndexed { index, line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${line.itemName} × ${line.quantity}", color = Color(0xFF334155))
                                TextButton(onClick = { lines = lines.filterIndexed { i, _ -> i != index } }) { Text("حذف") }
                            }
                        }
                    }
                    Text("الإجمالي المحسوب: $totalPreview", fontWeight = FontWeight.Bold)
                    if (error.isNotBlank()) Text(error, color = Color(0xFFDC2626))
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        store.addInvoiceDraft(number, type, date, lines)
                        query = ""; refresh++; showForm = false
                    } catch (e: Exception) {
                        error = e.message ?: "تعذر حفظ الفاتورة"
                    }
                }) { Text("حفظ كمسودة") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("إلغاء") } }
        )
    }
}


private fun writeInvoicePdf(context: Context, uri: Uri, details: InvoiceDetails) {
    val document = PdfDocument()
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }

    fun newPage() {
        if (page != null) document.finishPage(page!!)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        y = 42f
    }

    fun drawLine(text: String, size: Float = 13f, bold: Boolean = false) {
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        val layout = StaticLayout(
            text, 0, text.length, paint, 500, Layout.Alignment.ALIGN_OPPOSITE,
            TextDirectionHeuristics.RTL, 1.0f, 0.0f, false
        )
        if (y + layout.height > 790f) newPage()
        val canvas = page!!.canvas
        canvas.save()
        canvas.translate(48f, y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + 10f
    }

    try {
        newPage()
        drawLine("محاسب سوفت", 22f, true)
        drawLine(if (details.invoice.type == "بيع") "فاتورة بيع" else "فاتورة شراء", 18f, true)
        drawLine("رقم الفاتورة: ${details.invoice.number}")
        drawLine("التاريخ: ${details.invoice.date}")
        drawLine("الحالة: ${details.invoice.status}")
        drawLine("تفاصيل الأصناف", 15f, true)
        details.lines.forEachIndexed { index, line ->
            drawLine("${index + 1}. ${line.itemName} | الكمية: ${line.quantity} | سعر الوحدة: ${line.unitPrice} | الإجمالي: ${line.total}")
        }
        drawLine("الإجمالي النهائي: ${details.invoice.total}", 18f, true)
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("تعذر فتح ملف PDF للحفظ")
        output.use { document.writeTo(it) }
    } finally {
        if (page != null) document.finishPage(page!!)
        document.close()
    }
}
