package com.nextstepai.inventory.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.nextstepai.inventory.repository.*
import com.nextstepai.inventory.ui.*

/**
 * حاوية حقن التبعيات وإدارة دورة حياة الـ ViewModels المشتركة (Dependency Injection Container).
 * تضمن مشاركة نُسخ المستودعات (Repository Singletons) بين كافة الشاشات والنماذج لمنع تضارب الكاش وتقليل استهلاك الذاكرة.
 */
class AppContainer(
    val partRepository: PartRepository = PartRepository(),
    val stockRepository: StockRepository = StockRepository(),
    val bomRepository: BomRepository = BomRepository(),
    val companyRepository: CompanyRepository = CompanyRepository(),
    val poRepository: PurchaseOrderRepository = PurchaseOrderRepository(),
    val buildRepository: BuildOrderRepository = BuildOrderRepository(),
    val userRepository: UserRepository = UserRepository(),
    val allocationRepository: PartAllocationRepository = PartAllocationRepository(),
    val settingsRepository: AppSettingsRepository = AppSettingsRepository(),
    val loginRepository: LoginRepository = LoginRepository(),
    val salesRepository: SalesOrderRepository = SalesOrderRepository(),
    val partCategoryRepository: PartCategoryRepository = partRepository.categoryRepository,
    val stockLocationRepository: StockLocationRepository = stockRepository.locationRepository
) {
    fun createSettingsViewModel() = SettingsViewModel(
        repository = settingsRepository
    )

    fun createLoginViewModel() = LoginViewModel(
        repository = loginRepository
    )

    fun createPartViewModel() = PartViewModel(
        repository = partRepository,
        bomRepository = bomRepository,
        companyRepository = companyRepository,
        stockRepository = stockRepository,
        userRepository = userRepository,
        allocationRepository = allocationRepository
    )

    fun createBomViewModel() = BomViewModel(
        bomRepository = bomRepository,
        partRepository = partRepository
    )

    fun createStockViewModel() = StockViewModel(
        stockRepository = stockRepository,
        partRepository = partRepository,
        poRepository = poRepository,
        companyRepository = companyRepository,
        userRepository = userRepository
    )

    fun createCompanyViewModel() = CompanyViewModel(
        repository = companyRepository,
        poRepository = poRepository,
        partRepository = partRepository
    )

    fun createPurchaseOrderViewModel() = PurchaseOrderViewModel(
        poRepository = poRepository,
        companyRepository = companyRepository,
        partRepository = partRepository,
        buildRepository = buildRepository,
        salesRepository = salesRepository
    )

    fun createBuildOrderViewModel() = BuildOrderViewModel(
        repository = buildRepository,
        partRepository = partRepository,
        userRepository = userRepository
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided in CompositionLocal")
}
