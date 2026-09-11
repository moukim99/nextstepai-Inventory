package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.LoginRecord
import com.nextstepai.inventory.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة المستخدم لصفحة الدخول والجلسة.
 */
data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val currentSession: LoginRecord? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * نموذج العرض (ViewModel) الخاص بإدارة حالة وصفحة الدخول للربط بين الواجهة وجدول الدخول.
 */
class LoginViewModel(
    private val repository: LoginRepository = LoginRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isLoggedIn = repository.isLoggedIn(),
            currentSession = repository.getCurrentSession()
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * تنفيذ التحقق الدخول البسيط بنقرة زر واحدة.
     * يتم إنشاء وتسجيل حالة الدخول في جدول الدخول مباشرة.
     */
    fun performSimpleLogin() {
        _uiState.update { it.copy(isLoading = true) }

        val session = repository.verifyAndLogin()

        _uiState.update {
            it.copy(
                isLoggedIn = true,
                currentSession = session,
                isLoading = false,
                message = "تم تسجيل الدخول بنجاح"
            )
        }
    }

    /**
     * تسجيل الخروج وإعادة تعيين الجلسة.
     */
    fun performLogout() {
        repository.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentSession = null,
                message = "تم تسجيل الخروج"
            )
        }
    }
}
