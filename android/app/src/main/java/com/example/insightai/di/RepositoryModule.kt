package com.example.insightai.di

import com.example.insightai.data.remote.InsightApi
import com.example.insightai.data.repository.ResearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideResearchRepository(
        api: InsightApi
    ): ResearchRepository {
        return ResearchRepository(api)
    }
}