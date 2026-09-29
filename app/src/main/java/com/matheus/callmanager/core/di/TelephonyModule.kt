package com.matheus.callmanager.core.di

import com.matheus.callmanager.core.telephony.AndroidContactsLookup
import com.matheus.callmanager.core.telephony.AndroidRecentOutgoingLookup
import com.matheus.callmanager.core.telephony.ContactsLookup
import com.matheus.callmanager.core.telephony.RecentOutgoingLookup
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TelephonyModule {
    @Binds
    abstract fun bindContactsLookup(impl: AndroidContactsLookup): ContactsLookup

    @Binds
    abstract fun bindRecentOutgoingLookup(impl: AndroidRecentOutgoingLookup): RecentOutgoingLookup
}
