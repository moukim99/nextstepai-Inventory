package com.nextstepai.inventory.ui

import com.nextstepai.inventory.data.db.ActivationCodeEntity
import com.nextstepai.inventory.data.db.AppUserEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.ui.components.PrintableStaffLabelBottomSheet

/**
 * شاشة المالك لإدارة وتوليد أكواد التفعيل المخصصة وتعيين الصلاحيات للموظفين.
 * مصممة بشرط تنظيف كامل للواجهة 100% (Pure Clean LazyColumn Page) بدون أدنى تداخل أو اقتصاص.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: StaffManagementViewModel = remember { StaffManagementViewModel() },
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddStaffModal by remember { mutableStateOf(false) }
    var showPermissionsMatrixSheet by remember { mutableStateOf(false) }
    var pendingCodesExpanded by remember { mutableStateOf(true) }

    var userToDelete by remember { mutableStateOf<AppUserEntity?>(null) }
    var codeToDelete by remember { mutableStateOf<ActivationCodeEntity?>(null) }
    var editingCode by remember { mutableStateOf<ActivationCodeEntity?>(null) }
    var editingUser by remember { mutableStateOf<AppUserEntity?>(null) }
    var qrCodeToShow by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val clipboardManager = LocalClipboardManager.current

    val filteredUsers = remember(uiState.users, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.users
        } else {
            uiState.users.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.role.contains(searchQuery, ignoreCase = true) ||
                        user.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredCodes = remember(uiState.activeInvitationCodes, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.activeInvitationCodes
        } else {
            uiState.activeInvitationCodes.filter { code ->
                code.code.contains(searchQuery, ignoreCase = true) ||
                        code.label.contains(searchQuery, ignoreCase = true) ||
                        code.assignedRole.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            StaffTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
            ) {
            // 1. شريط البحث
            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "البحث باسم الموظف، الوصف، أو المعرف...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // 3. قائمة الموظفين - مسطحة ونظيفة 100%
            if (filteredUsers.isEmpty()) {
                item {
                    val emptyTitle: String
                    val emptySubtitle: String
                    val emptyIcon: ImageVector

                    if (searchQuery.isNotBlank()) {
                        emptyTitle = "لا توجد نتائج مطابقة للبحث"
                        emptySubtitle = "جرب البحث باسم آخر، مسمى وظيفي، أو كود تفعيل"
                        emptyIcon = Icons.Default.SearchOff
                    } else if (uiState.activeInvitationCodes.isNotEmpty()) {
                        emptyTitle = "في انتظار تسجيل الموظفين (أكواد معلقة)"
                        emptySubtitle = "تم توليد كود التفعيل بنجاح. أرسل الكود المولد بالأسفل للموظف ليقوم بدخوله وتسجيل حسابه."
                        emptyIcon = Icons.Default.HourglassEmpty
                    } else {
                        emptyTitle = "لا يوجد موظفون مسجلون حالياً"
                        emptySubtitle = "اضغط على زر \"إضافة موظف\" بالأسفل لتوليد كود تفعيل ومشاركته مع فريق العمل"
                        emptyIcon = Icons.Default.Badge
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = emptyIcon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = if (uiState.activeInvitationCodes.isNotEmpty() && searchQuery.isBlank()) Color(0xFFD97706) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptySubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(filteredUsers) { _, user ->
                    val linkedCode = uiState.activeInvitationCodes.find { it.usedByUuid == user.uuid }
                    val displayCode = linkedCode?.code ?: user.phoneNumber.ifBlank { user.uuid.take(8) }
                    val displayRole = linkedCode?.label?.ifBlank { user.role } ?: user.role

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Icon Box
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (user.active) Color(0xFFEEF2FF)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (user.active) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    // Primary Title: Registered User Name
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Subtitle: Job Title & Code/ID underneath
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEEF2FF),
                                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                        ) {
                                            Text(
                                                text = "المهمة: $displayRole",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color(0xFF3730A3),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = "الكود/المعرف: $displayCode",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (user.active) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (user.active) "● نشط" else "● معطل",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = if (user.active) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 2. Action Operations Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Right Side: Active Status Switch Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Switch(
                                        checked = user.active,
                                        onCheckedChange = { viewModel.toggleUserActiveStatus(user.uuid, user.active) }
                                    )
                                    Text(
                                        text = if (user.active) "تفعيل الصلاحيات" else "إيقاف الصلاحيات",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (user.active) Color(0xFF4F46E5) else MaterialTheme.colorScheme.error
                                    )
                                }

                                // Left Side: REVERSED 3 Circular Buttons (Barcode 🔳 -> Edit ✏️ -> Delete 🗑️)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Barcode/QR Circle Button (Purple)
                                    Surface(
                                        onClick = { qrCodeToShow = Triple(displayCode, user.name, user.role.ifBlank { linkedCode?.assignedRole ?: "OPERATOR" }) },
                                        shape = CircleShape,
                                        color = Color(0xFFF3E8FF),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode,
                                                contentDescription = "رمز QR للموظف",
                                                tint = Color(0xFF7E22CE),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 2. Edit Circle Button (Blue)
                                    Surface(
                                        onClick = { editingUser = user },
                                        shape = CircleShape,
                                        color = Color(0xFFEEF2FF),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل الموظف",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 3. Delete Circle Button (Red)
                                    Surface(
                                        onClick = { userToDelete = user },
                                        shape = CircleShape,
                                        color = Color(0xFFFEE2E2),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف الموظف",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. قسم الأكواد المعلقة غير المستخدمة (يظهر فقط عند وجود أكواد معلقة)
            if (filteredCodes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingCodesExpanded = !pendingCodesExpanded }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "أكواد التفعيل المعلقة غير المستخدمة (${filteredCodes.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Icon(
                                imageVector = if (pendingCodesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = pendingCodesExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                filteredCodes.forEach { codeEntity ->
                                    val linkedUser = uiState.users.find { it.uuid == codeEntity.usedByUuid }
                                    val isActivated = codeEntity.isUsed || linkedUser != null
                                    val isActive = linkedUser?.active ?: true

                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Icon Box (Key icon when pending, Person icon/avatar when activated)
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            when {
                                                                !isActivated -> Color(0xFFFEF3C7)
                                                                isActive -> Color(0xFFEEF2FF)
                                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isActivated) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = if (isActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.VpnKey,
                                                            contentDescription = null,
                                                            tint = Color(0xFFD97706),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    // Primary Title: Job Title when pending, User Name when activated
                                                    Text(
                                                        text = if (isActivated) (linkedUser?.name ?: codeEntity.label.ifBlank { "اسم الموظف" }) else codeEntity.label.ifBlank { "كود موظف غير مفعّل" },
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp
                                                        ),
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // Subtitle: Code number when pending, Job Title + Code number underneath when activated
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        if (isActivated) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFEEF2FF),
                                                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                                            ) {
                                                                Text(
                                                                    text = "المهمة: ${codeEntity.label.ifBlank { linkedUser?.role ?: codeEntity.assignedRole }}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 11.sp
                                                                    ),
                                                                    color = Color(0xFF3730A3),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }

                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFF1F5F9)
                                                            ) {
                                                                Text(
                                                                    text = "الكود: ${codeEntity.code}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                                    color = Color(0xFF475569),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        } else {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFFEF3C7)
                                                            ) {
                                                                Text(
                                                                    text = "الكود: ${codeEntity.code}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 11.sp,
                                                                        letterSpacing = 0.5.sp
                                                                    ),
                                                                    color = Color(0xFF92400E),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }

                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFF1F5F9)
                                                            ) {
                                                                Text(
                                                                    text = "الدور: ${codeEntity.assignedRole}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                                    color = Color(0xFF475569),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = when {
                                                        !isActivated -> Color(0xFFFEF3C7)
                                                        isActive -> Color(0xFFDCFCE7)
                                                        else -> Color(0xFFFEE2E2)
                                                    }
                                                ) {
                                                    Text(
                                                        text = when {
                                                            !isActivated -> "● غير مفعّل"
                                                            isActive -> "● نشط"
                                                            else -> "● معطل"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                        color = when {
                                                            !isActivated -> Color(0xFFD97706)
                                                            isActive -> Color(0xFF15803D)
                                                            else -> Color(0xFFB91C1C)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Primary Action Switch Toggle on Right (RTL Right)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Switch(
                                                        checked = isActive,
                                                        onCheckedChange = {
                                                            if (linkedUser != null) {
                                                                viewModel.toggleUserActiveStatus(linkedUser.uuid, isActive)
                                                            }
                                                        }
                                                    )
                                                    Text(
                                                        text = if (isActive) "تفعيل الصلاحيات" else "إيقاف الصلاحيات",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.error
                                                    )
                                                }

                                                // Secondary Action Circle Buttons on Left (RTL Left): Barcode 🔳 -> Edit ✏️ -> Delete 🗑️ (REVERSED ORDER)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // 1. Barcode/QR Circle Button (Purple)
                                                    Surface(
                                                        onClick = {
                                                            qrCodeToShow = Triple(
                                                                codeEntity.code,
                                                                codeEntity.label.ifBlank { linkedUser?.name ?: codeEntity.code },
                                                                codeEntity.assignedRole.ifBlank { linkedUser?.role ?: "OPERATOR" }
                                                            )
                                                        },
                                                        shape = CircleShape,
                                                        color = Color(0xFFF3E8FF),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.QrCode,
                                                                contentDescription = "رمز QR الكود",
                                                                tint = Color(0xFF7E22CE),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    // 2. Edit Circle Button (Blue)
                                                    Surface(
                                                        onClick = {
                                                            viewModel.prepareEditCode(codeEntity)
                                                            showAddStaffModal = true
                                                        },
                                                        shape = CircleShape,
                                                        color = Color(0xFFEEF2FF),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "تعديل",
                                                                tint = Color(0xFF4F46E5),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    // 3. Delete Circle Button (Red)
                                                    Surface(
                                                        onClick = {
                                                            if (linkedUser != null) {
                                                                userToDelete = linkedUser
                                                            } else {
                                                                codeToDelete = codeEntity
                                                            }
                                                        },
                                                        shape = CircleShape,
                                                        color = Color(0xFFFEE2E2),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.DeleteOutline,
                                                                contentDescription = "حذف",
                                                                tint = Color(0xFFDC2626),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.prepareAddNewStaff()
                    showAddStaffModal = true
                },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "إضافة موظف",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إضافة موظف",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                )
            }
        }

        // الصفحة المنبثقة (Modal Bottom Sheet) لإضافة موظف جديد وتعيين الصلاحيات
        if (showAddStaffModal) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            val permissionsSummaryText = remember(uiState.permissionMatrix) {
                val activeModules = uiState.permissionMatrix.filter { it.canRead || it.canCreate || it.canUpdate || it.canDelete }
                if (activeModules.isEmpty()) {
                    "لا توجد صلاحيات مخصصة (اضغط لضبط الصلاحيات)"
                } else if (activeModules.size == uiState.permissionMatrix.size && activeModules.all { it.canRead && it.canCreate && it.canUpdate && it.canDelete }) {
                    "كامل الصلاحيات (جميع الأقسام الأربعة)"
                } else {
                    "${activeModules.size} أقسام مفعّلة: " + activeModules.joinToString("، ") { it.moduleNameAr.replace("قسم ", "") }
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showAddStaffModal = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (uiState.editingCodeCode != null) "تعديل الصلاحيات والمسمى الوظيفي" else "إضافة موظف جديد وتخصيص الصلاحيات",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                        }

                        IconButton(onClick = { showAddStaffModal = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    OutlinedTextField(
                        value = uiState.newStaffLabel,
                        onValueChange = { viewModel.onLabelChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("المسمى الوظيفي / المهمة") },
                        placeholder = { Text("مثال: فني خط إنتاج، مسؤول جرد، أمين مستودع...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Text(
                        text = "سيدخل الموظف اسمه الشخصي ورقم هاتفه وكلمة مروره بنفسه عند كتابة الكود في جهازه.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // حقل اختيار مصفوفة الصلاحيات كعنصر قابل للنقر يفتح نافذة منبثقة ثانية
                    Text(
                        text = "الصلاحيات الممنوحة للموظف:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        onClick = { showPermissionsMatrixSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "مصفوفة الصلاحيات التفصيلية (Permission Matrix)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = permissionsSummaryText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }

                            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "فتح الصلاحيات",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(if (isRtl) -1f else 1f, 1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddStaffModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                viewModel.generateActivationCode()
                                showAddStaffModal = false
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.editingCodeCode != null) "حفظ التغييرات" else "توليد كود التفعيل",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // النافذة المنبثقة الثانية (Modal Bottom Sheet) لضبط وتفصيل مصفوفة الصلاحيات
        if (showPermissionsMatrixSheet) {
            val permSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val expandedModules = remember { mutableStateMapOf<String, Boolean>() }

            ModalBottomSheet(
                onDismissRequest = { showPermissionsMatrixSheet = false },
                sheetState = permSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مصفوفة الصلاحيات التفصيلية",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "حدد مستويات الوصول للوحدات الأربع للموظف الجديد",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showPermissionsMatrixSheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    uiState.permissionMatrix.forEach { perm ->
                        val moduleIcon = when (perm.moduleKey) {
                            "WAREHOUSE" -> Icons.Default.Warehouse
                            "PRODUCTION" -> Icons.Default.Build
                            "SALES_ORDERS" -> Icons.Default.Storefront
                            "SETTINGS" -> Icons.Default.Settings
                            else -> Icons.Default.Layers
                        }

                        val isAllSelected = perm.canRead && perm.canCreate && perm.canUpdate && perm.canDelete
                        val isExpanded = expandedModules.getOrElse(perm.moduleKey) { true }

                        val activePermsSummary = remember(perm) {
                            val list = mutableListOf<String>()
                            if (perm.canRead) list.add("قراءة")
                            if (perm.canCreate) list.add("إضافة")
                            if (perm.canUpdate) list.add("تعديل")
                            if (perm.canDelete) list.add("حذف")
                            when {
                                list.size == 4 -> "كامل الصلاحيات"
                                list.isEmpty() -> "بدون صلاحيات"
                                else -> list.joinToString("، ")
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedModules[perm.moduleKey] = !isExpanded }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "طي" else "توسيع",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Icon(
                                            imageVector = moduleIcon,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = perm.moduleNameAr,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF0F172A)
                                            )
                                            if (!isExpanded) {
                                                Text(
                                                    text = activePermsSummary,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = if (isAllSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }

                                    TextButton(
                                        onClick = { viewModel.toggleAllPermissionsForModule(perm.moduleKey) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAllSelected) "إلغاء الكل" else "كامل القسم",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PermissionCheckbox("قراءة", perm.canRead) {
                                                viewModel.togglePermission(perm.moduleKey, "READ")
                                            }
                                            PermissionCheckbox("إضافة", perm.canCreate) {
                                                viewModel.togglePermission(perm.moduleKey, "CREATE")
                                            }
                                            PermissionCheckbox("تعديل", perm.canUpdate) {
                                                viewModel.togglePermission(perm.moduleKey, "UPDATE")
                                            }
                                            PermissionCheckbox("حذف", perm.canDelete, isWarning = true) {
                                                viewModel.togglePermission(perm.moduleKey, "DELETE")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showPermissionsMatrixSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأكيد وإغلاق الصلاحيات",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Dialog إظهار الكود المولد
        uiState.generatedCodeDialog?.let { codeEntity ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissGeneratedDialog() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("تم توليد كود التفعيل المخصص بنجاح")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "استخدم الكود أدناه لتفعيل حساب الموظف (${codeEntity.label}):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(2.dp, Color(0xFF4F46E5))
                        ) {
                            Text(
                                text = codeEntity.code,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                ),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(codeEntity.code))
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الكود للحافظة")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissGeneratedDialog() }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // 1. نافذة تأكيد حذف حساب موظف مسجل
        userToDelete?.let { user ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = { Text("تأكيد حذف حساب الموظف", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف حساب الموظف (${user.name})؟ لن يتمكن الموظف من استخدام حسابه بعد الحذف.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteUser(user.uuid)
                            userToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الحذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // 2. نافذة تأكيد إلغاء كود تفعيل معلق
        codeToDelete?.let { codeEntity ->
            AlertDialog(
                onDismissRequest = { codeToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = { Text("تأكيد إلغاء كود التفعيل", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من إلغاء كود التفعيل (${codeEntity.code}) المخصص لـ (${codeEntity.label.ifBlank { "الموظف" }})؟ لن يمكن استخدامه بعد الحذف.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteInvitationCode(codeEntity.code)
                            codeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الإلغاء")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { codeToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // 3. نافذة تعديل وصف كود التفعيل المعلق
        editingCode?.let { codeEntity ->
            var tempLabel by remember(codeEntity) { mutableStateOf(codeEntity.label) }
            AlertDialog(
                onDismissRequest = { editingCode = null },
                title = { Text("تعديل مسمى كود التفعيل (${codeEntity.code})", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        label = { Text("المسمى الوظيفي / المهمة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateActivationCodeLabel(codeEntity.code, tempLabel)
                            editingCode = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("حفظ التغييرات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCode = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // 4. نافذة تعديل بيانات موظف مسجل
        editingUser?.let { user ->
            var tempName by remember(user) { mutableStateOf(user.name) }
            var tempRole by remember(user) { mutableStateOf(user.role) }
            AlertDialog(
                onDismissRequest = { editingUser = null },
                title = { Text("تعديل بيانات الموظف", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("اسم الموظف") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = tempRole,
                            onValueChange = { tempRole = it },
                            label = { Text("الدور الوظيفي") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateAppUser(user.uuid, tempName, tempRole)
                            editingUser = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("حفظ التغييرات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingUser = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // 5. الصفحة المنبثقة لإظهار ملصق باركود الموظف وتوليد الـ PDF والمشاركة
        qrCodeToShow?.let { (codeVal, titleLabel, roleVal) ->
            PrintableStaffLabelBottomSheet(
                codeStr = codeVal,
                labelTitle = titleLabel,
                assignedRole = roleVal,
                onDismiss = { qrCodeToShow = null }
            )
        }
    }
}

@Composable
private fun PermissionCheckbox(
    label: String,
    checked: Boolean,
    isWarning: Boolean = false,
    onCheckedChange: () -> Unit
) {
    val activeColor = if (isWarning && checked) Color(0xFFDC2626) else Color(0xFF4F46E5)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable { onCheckedChange() }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = activeColor
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (checked && isWarning) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (checked && isWarning) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * الترويسة العلوية لشاشة إدارة فريق العمل والصلاحيات بنصف القطر المعياري 25.dp ومحاذاة مركزية مطابقة لباقي الشاشات.
 */
@Composable
private fun StaffTopBar(
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "إدارة فريق العمل والصلاحيات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "إدارة حسابات الموظفين وصلاحيات الوصول",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}
