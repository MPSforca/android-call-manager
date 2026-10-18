package com.matheus.callmanager.core.di

import com.matheus.callmanager.feature.onboarding.AndroidSystemPermissionStatus
import com.matheus.callmanager.feature.onboarding.SystemPermissionStatus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionsModule {
    @Binds
    abstract fun bindSystemPermissionStatus(impl: AndroidSystemPermissionStatus): SystemPermissionStatus
}
