package com.vkozhemi.bleapp.di

import com.vkozhemi.bleapp.model.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeneralModule {
    @Provides
    @Singleton
    fun provideRepository(): Repository {
        return Repository()
    }
}