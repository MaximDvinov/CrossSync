package com.cross.sync.notifications.di

import com.cross.sync.notifications.presentation.NotificationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val desktopNotificationModule: Module = module {
    includes(notificationModule)
    viewModelOf(::NotificationViewModel)
}
