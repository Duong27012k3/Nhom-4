package com.example.nhom4.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhom4.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val sessionManager = SessionManager(application)

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val orderDao = database.orderDao()
    private val orderDetailDao = database.orderDetailDao()

    val categories = categoryDao.getAllCategories().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    val products = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) {
            productDao.getAllProducts()
        } else {
            productDao.getProductsByCategory(categoryId)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow(sessionManager.getUserId())
    val currentUserId = _currentUserId.asStateFlow()

    private val _loginError = MutableSharedFlow<String>()
    val loginError = _loginError.asSharedFlow()

    fun selectCategory(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user != null && user.password == password) {
                sessionManager.saveLoginSession(user.id)
                _isLoggedIn.value = true
                _currentUserId.value = user.id
                onSuccess()
            } else {
                _loginError.emit("Tên đăng nhập hoặc mật khẩu không đúng")
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _isLoggedIn.value = false
        _currentUserId.value = -1
    }

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    fun addToCart(product: Product, quantity: Int, onLoginRequired: () -> Unit) {
        if (!sessionManager.isLoggedIn()) {
            onLoginRequired()
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            var order = orderDao.getUnpaidOrder(userId)
            
            if (order == null) {
                val newOrderId = orderDao.insertOrder(Order(userId = userId))
                order = orderDao.getOrderById(newOrderId.toInt())
            }

            order?.let {
                orderDetailDao.insertOrderDetail(
                    OrderDetail(
                        orderId = it.id,
                        productId = product.id,
                        quantity = quantity,
                        priceAtOrder = product.price
                    )
                )
            }
        }
    }

    fun getOrderDetails(orderId: Int): Flow<List<OrderDetail>> = orderDetailDao.getOrderDetails(orderId)

    fun getCurrentUnpaidOrder(): Flow<Order?> = _currentUserId.flatMapLatest { userId ->
        if (userId != -1) {
            flow { emit(orderDao.getUnpaidOrder(userId)) }
        } else {
            flow { emit(null) }
        }
    }

    fun checkout(order: Order, onComplete: () -> Unit) {
        viewModelScope.launch {
            orderDao.updateOrder(order.copy(status = "Paid"))
            onComplete()
        }
    }
}
