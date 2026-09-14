package com.cross.sync.notifications.di

import com.cross.sync.notifications.data.NotificationRepositoryImpl
import com.cross.sync.notifications.domain.repository.NotificationRepository
import com.cross.sync.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cross.sync.notifications.domain.usecase.ClearNotificationHistoryUseCase
import com.cross.sync.notifications.domain.usecase.ObserveNotificationsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule: Module = module {
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    singleOf(::ObserveNotificationsUseCase)
    singleOf(::MarkNotificationReadUseCase)
    singleOf(::ClearNotificationHistoryUseCase)
}
