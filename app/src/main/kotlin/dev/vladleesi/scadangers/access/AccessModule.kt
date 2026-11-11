package dev.vladleesi.scadangers.access

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vladleesi.scadangers.core.security.capability.AccessRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccessModule {
    @Provides
    @Singleton
    fun repository(source: AndroidAccessSource, store: AndroidAccessStore): AccessRepository =
        AccessRepository(source, store)
}
