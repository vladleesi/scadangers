package dev.scadangers.scanner

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.scadangers.core.database.AppScanDatabase
import dev.scadangers.core.database.RoomAppScanStore
import dev.scadangers.core.security.apps.AppScanner
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ScannerModule {
    @Provides
    @Singleton
    fun scanner(
        @ApplicationContext context: Context,
        source: AndroidAppInventorySource
    ): AppScanner {
        val database = lazy {
            Room.databaseBuilder(context, AppScanDatabase::class.java, "app-inventory.db").build()
        }
        return AppScanner(source, RoomAppScanStore { database.value })
    }
}
