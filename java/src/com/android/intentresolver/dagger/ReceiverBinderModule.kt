package com.android.intentresolver.dagger

import android.content.BroadcastReceiver
import com.android.intentresolver.ChooserActivityReEnabler
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/** Injection instructions for injectable [BroadcastReceivers][BroadcastReceiver] */
@Module
interface ReceiverBinderModule {

    @Binds
    @IntoMap
    @ClassKey(ChooserActivityReEnabler::class)
    fun bindChooserActivityReEnabler(receiver: ChooserActivityReEnabler): BroadcastReceiver
}
