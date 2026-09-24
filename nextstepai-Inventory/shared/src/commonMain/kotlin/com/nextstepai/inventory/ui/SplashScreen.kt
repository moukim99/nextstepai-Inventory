package com.nextstepai.inventory.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.app_logo
import nextstepai_inventory.shared.generated.resources.app_name

/**
 * شاشة البداية / الواجهة الترحيبية (Splash Screen) متجاوبة بالكامل لجميع الشاشات
 * تعرض شعار التطبيق بدقة عالية مع حركة ظهور انسيابية ومؤشر تحميل خفيف قبل الانتقال التلقائي.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.72f,
        animationSpec = tween(
            durationMillis = 850,
            easing = FastOutSlowInEasing
        )
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 750
        )
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 900,
            delayMillis = 250
        )
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200) // عرض الشاشة الترحيبية لمدة 2.2 ثانية
        onSplashFinished()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isWideScreen = maxWidth >= 600.dp
            val logoSize = if (isWideScreen) 250.dp else 170.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // هالة دائرية جمالية متجاوبة خلف الشعار
                Box(
                    modifier = Modifier
                        .size(if (isWideScreen) 480.dp else 320.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            CircleShape
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                ) {
                    // الشعار بدقة عالية مع الحفاظ التام على أبعاد (Aspect Ratio)
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = stringResource(Res.string.app_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(logoSize)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(textAlpha)
                    ) {
                        Text(
                            text = "دارة المخازن",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isWideScreen) 32.sp else 26.sp,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "منظومة إدارة المخزون وتدفق التصنيع الذكي",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (isWideScreen) 16.sp else 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // مؤشر تحميل متدرج وخفيف
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(if (isWideScreen) 200.dp else 140.dp)
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
