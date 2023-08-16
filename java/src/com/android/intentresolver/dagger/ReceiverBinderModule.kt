package com.android.intentresolver.dagger

import android.content.BroadcastReceiver
import dagger.Module
import dagger.multibindings.Multibinds

/** Injection instructions for injectable [BroadcastReceivers][BroadcastReceiver] */
@Module
interface ReceiverBinderModule {

    @Multibinds fun bindReceivers(): Map<Class<*>, @JvmSuppressWildcards BroadcastReceiver>
}
