package info.aalmoghalis.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val Gold = Color(0xFFD4AF37)
private val Navy = Color(0xFF0F172A)
private val Page = Color(0xFFF8FAFC)

private data class AppModule(val title: String, val subtitle: String, val icon: ImageVector)

private val modules = listOf(
    AppModule("المبيعات", "الفواتير والمرتجعات", Icons.Default.PointOfSale),
    AppModule("المشتريات", "فواتير الموردين", Icons.Default.ShoppingCart),
    AppModule("الحسابات", "العملاء والموردون", Icons.Default.People),
    AppModule("المخزون", "الأصناف وحركة المخزون", Icons.Default.Inventory2),
    AppModule("السندات", "قبض وصرف", Icons.Default.ReceiptLong),
    AppModule("الصناديق والبنوك", "الأرصدة والحركات", Icons.Default.AccountBalanceWallet),
    AppModule("التقارير", "تقارير الحسابات والمبيعات", Icons.Default.Assessment),
    AppModule("الإعدادات", "إعدادات التطبيق", Icons.Default.Settings)
)

@Composable
fun DashboardScreen() {
    var selected by remember { mutableStateOf("الرئيسية") }
    CompositionDirectionRtl {
        Scaffold(
            containerColor = Page,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("الرئيسية", "المبيعات", "المشتريات", "الحسابات", "المزيد").forEach { label ->
                        NavigationBarItem(
                            selected = selected == label,
                            onClick = { selected = label },
                            icon = { Text(when (label) {
                                "الرئيسية" -> "⌂"
                                "المبيعات" -> "↗"
                                "المشتريات" -> "↙"
                                "الحسابات" -> "♙"
                                else -> "⋯"
                            }) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).background(Page).padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("محاسب سوفت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
                        Text(if (selected == "الرئيسية") "نظام إدارة الحسابات والمخزون" else "قسم $selected", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(color = Gold.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp)) {
                        Text("الذهبي", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (selected == "الحسابات") {
                    AccountManagementScreen()
                } else if (selected == "المخزون") {
                    ItemManagementScreen()
                } else if (selected == "المبيعات") {
                    InvoiceManagementScreen("بيع")
                } else if (selected == "المشتريات") {
                    InvoiceManagementScreen("شراء")
                } else if (selected == "التقارير") {
                    ReportsScreen()
                } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Navy),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ملخص النشاط", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryValue("إجمالي المبيعات", "—")
                            SummaryValue("إجمالي المشتريات", "—")
                            SummaryValue("صافي الرصيد", "—")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("ستظهر الأرقام بعد ربط قاعدة البيانات الفعلية.", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الأقسام الرئيسية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
                    Text("٨ أقسام", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(modules) { module ->
                        Card(
                            onClick = { selected = module.title },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                Surface(color = Gold.copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp)) {
                                    Icon(module.icon, contentDescription = module.title, tint = Navy, modifier = Modifier.padding(10.dp).size(24.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(module.title, color = Navy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(module.subtitle, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { selected = module.title }) { Text("فتح القسم", color = Color(0xFFA67C00)) }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen() {
    val store = remember { AccountingStore(androidx.compose.ui.platform.LocalContext.current) }
    var refresh by remember { mutableIntStateOf(0) }
    val counts = remember(refresh) { store.getCounts() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("إحصاءات السجلات", fontWeight = FontWeight.Bold)
            androidx.compose.material3.Button(onClick = { refresh++ }) { Text("تحديث") }
        }
        listOf("الحسابات النشطة" to counts.accounts, "الأصناف النشطة" to counts.items, "مسودات البيع" to counts.salesDrafts, "مسودات الشراء" to counts.purchaseDrafts).forEach { row ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.first)
                    Text(row.second.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("هذه إحصاءات للسجلات المحلية فقط؛ التقارير المحاسبية تحتاج إلى استكمال ترحيل القيود.")
    }
}

@Composable
private fun SummaryValue(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CompositionDirectionRtl(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        content()
    }
}
