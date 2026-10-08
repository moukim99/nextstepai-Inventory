package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.LoginRecord
import com.nextstepai.inventory.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            val tokens = repository.getTokens()
            val session = repository.getCurrentSession()
            if (tokens != null && session?.isLoggedIn == true) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        currentSession = session
                    )
                }
            }
        }
    }

    /**
     * تنفيذ التحقق والدخول بنقرة زر واحدة.
     * يتحقق صراحة من نجاح الجلسة وتوافر الرموز قبل ضبط حالة الدخول.
     */
    fun performSimpleLogin() {
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            try {
                val session = repository.verifyAndLogin()
                if (session.isLoggedIn) {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            currentSession = session,
                            isLoading = false,
                            message = "تم تسجيل الدخول بنجاح"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = false,
                            currentSession = null,
                            isLoading = false,
                            message = "فشل التحقق: جلسة الدخول غير مفعلة"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = false,
                        currentSession = null,
                        isLoading = false,
                        message = "خطأ أثناء تسجيل الدخول: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * تسجيل الخروج وإعادة تعيين الجلسة ومسح الرموز.
     */
    fun performLogout() {
        viewModelScope.launch {
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
}
