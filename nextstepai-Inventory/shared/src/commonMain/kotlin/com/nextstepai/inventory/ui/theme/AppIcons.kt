package com.nextstepai.inventory.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * كائن مركزي لإدارة كافة أيقونات التطبيق باستخدام حزمة أيقونات Material الرسمية الموسعة (Material Icons Extended).
 */
object AppIcons {
    val Warehouse: ImageVector = Icons.Default.Warehouse
    val Management: ImageVector = Icons.Default.BusinessCenter
    val Settings: ImageVector = Icons.Default.Settings
    val Dashboard: ImageVector = Icons.Default.Dashboard
    val Parts: ImageVector = Icons.Default.Category
    val Bom: ImageVector = Icons.Default.AccountTree
    val Stock: ImageVector = Icons.Default.Inventory2
    val Companies: ImageVector = Icons.Default.CorporateFare
    val Orders: ImageVector = Icons.Default.ShoppingCart
    val Builds: ImageVector = Icons.Default.PrecisionManufacturing
    val Search: ImageVector = Icons.Default.Search
    val Add: ImageVector = Icons.Default.Add
    val Star: ImageVector = Icons.Default.StarOutline
    val StarFilled: ImageVector = Icons.Default.Star
    val Filter: ImageVector = Icons.Default.FilterList
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Logout: ImageVector = Icons.AutoMirrored.Filled.ExitToApp
    val Theme: ImageVector = Icons.Default.Palette
}
