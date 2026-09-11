package com.nextstepai.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * الشاشة الرئيسية للتطبيق التي تنتقل إليها الواجهة فور تسجيل الدخول بنجاح.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    loginUiState: LoginUiState,
    onLogoutClick: () -> Unit,
    onOpenPartsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نظام إدارة المخزون") },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text(
                            "تسجيل الخروج",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "مرحباً بك! 👋",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم التحقق وتسجيل الدخول بنجاح.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // زر الانتقال المباشر إلى تحليل وتصفح جدول إدارة القطع والمكونات (Part)
            ElevatedCard(
                onClick = onOpenPartsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚙️ جدول إدارة القطع والمكونات (Part)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عرض وتصفية وبحث وحفظ بيانات جدول Part المستوحي من نظام InvenTree.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "فتح الجدول ⬅",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // تفاصيل بيانات سجل الجلسة من جدول الدخول (Login Table)
            val session = loginUiState.currentSession
            if (session != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "بيانات الجلسة الحالية (Login Table Record):",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "رقم السجل: #${session.id}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "اسم المستخدم: ${session.username}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "حالة التحقق: ${session.authStatus}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "الحالة: نشط ومسجل الدخول", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
