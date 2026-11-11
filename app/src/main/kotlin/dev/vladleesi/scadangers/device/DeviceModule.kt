package dev.vladleesi.scadangers.device

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vladleesi.scadangers.core.security.device.DeviceObservationSource
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DeviceEvaluationDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DeviceModule {
    @Provides
    fun source(source: AndroidDeviceObservationSource): DeviceObservationSource = source

    @Provides
    @DeviceEvaluationDispatcher
    fun dispatcher(): CoroutineDispatcher = Dispatchers.Default
}
