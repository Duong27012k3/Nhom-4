package com.example.nhom4.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Category::class, Product::class, Order::class, OrderDetail::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun orderDetailDao(): OrderDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val userDao = db.userDao()
            val categoryDao = db.categoryDao()
            val productDao = db.productDao()

            // Add sample user
            userDao.insertUser(User(username = "admin", password = "123", name = "Admin User"))

            // Add sample categories
            val categories = listOf(
                Category(id = 1, name = "Điện thoại"),
                Category(id = 2, name = "Laptop"),
                Category(id = 3, name = "Đồng hồ"),
                Category(id = 4, name = "Phụ kiện")
            )
            categoryDao.insertCategories(categories)

            // Add sample products
            val products = listOf(
                Product(categoryId = 1, name = "iPhone 15 Pro", price = 28990000.0, description = "Chip A17 Pro mạnh mẽ nhất."),
                Product(categoryId = 1, name = "Samsung Galaxy S24 Ultra", price = 29990000.0, description = "Camera 200MP, AI tích hợp."),
                Product(categoryId = 1, name = "Xiaomi 14", price = 15990000.0, description = "Ống kính Leica chuyên nghiệp."),
                
                Product(categoryId = 2, name = "MacBook Air M3", price = 27990000.0, description = "Siêu mỏng, siêu mạnh."),
                Product(categoryId = 2, name = "Dell XPS 13", price = 35000000.0, description = "Màn hình vô cực, thiết kế sang trọng."),
                Product(categoryId = 2, name = "ASUS ROG Zephyrus", price = 42000000.0, description = "Laptop gaming đỉnh cao."),
                
                Product(categoryId = 3, name = "Apple Watch Series 9", price = 10490000.0, description = "Theo dõi sức khỏe chuyên sâu."),
                Product(categoryId = 3, name = "Samsung Galaxy Watch 6", price = 6990000.0, description = "Thiết kế cổ điển, tính năng hiện đại."),
                
                Product(categoryId = 4, name = "AirPods Pro 2", price = 5990000.0, description = "Chống ồn chủ động gấp 2 lần."),
                Product(categoryId = 4, name = "Sạc dự phòng 20000mAh", price = 850000.0, description = "Sạc nhanh 22.5W."),
                Product(categoryId = 4, name = "Chuột Logitech MX Master 3S", price = 2490000.0, description = "Công thái học, cuộn siêu nhanh.")
            )
            productDao.insertProducts(products)
        }
    }
}
