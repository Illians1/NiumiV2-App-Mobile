package com.niumi.feature.session.di

import com.niumi.feature.session.home.ClockMinuteTicker
import com.niumi.feature.session.home.MinuteTicker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Accueil (étape 45) : le battement de la minute du cadran et des comptes à rebours. */
@Module
@InstallIn(SingletonComponent::class)
interface HomeModule {
    @Binds
    fun bindMinuteTicker(ticker: ClockMinuteTicker): MinuteTicker
}
