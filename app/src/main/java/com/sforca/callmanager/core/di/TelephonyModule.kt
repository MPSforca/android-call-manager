package com.sforca.callmanager.core.di

import com.sforca.callmanager.core.telephony.AndroidCallLogDataSource
import com.sforca.callmanager.core.telephony.AndroidContactsLookup
import com.sforca.callmanager.core.telephony.AndroidRecentOutgoingLookup
import com.sforca.callmanager.core.telephony.CallLogDataSource
import com.sforca.callmanager.core.telephony.ContactsLookup
import com.sforca.callmanager.core.telephony.RecentOutgoingLookup
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

    @Binds
    abstract fun bindCallLogDataSource(impl: AndroidCallLogDataSource): CallLogDataSource
}
