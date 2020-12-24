package com.digiapt.digiaptvideoapp

import android.app.Application
import com.digiapt.digiaptvideoapp.database.AppDatabase
import com.digiapt.digiaptvideoapp.network.Api
import com.digiapt.digiaptvideoapp.network.VdocipherApi
import com.digiapt.digiaptvideoapp.preferences.PreferenceProvider
import com.digiapt.digiaptvideoapp.repositories.HomeRepository
import com.digiapt.digiaptvideoapp.repositories.VdocipherRepository
import com.digiapt.digiaptvideoapp.viewmodels.HomeViewModelFactory
import com.digiapt.digiaptvideoapp.viewmodels.VdocipherViewModelFactory
import com.example.mvvm.data.network.NetworkConnectionInterceptor
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class DigiaptVideoApplication : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidXModule(this@DigiaptVideoApplication))

        bind() from singleton { NetworkConnectionInterceptor(instance()) }
        bind() from singleton { Api() }
        bind() from singleton { VdocipherApi() }
        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { PreferenceProvider(instance()) }

        bind() from singleton { HomeRepository(instance(),instance(),instance(),instance()) }
        bind() from singleton { VdocipherRepository(instance()) }

        bind() from provider {
            HomeViewModelFactory(
                instance()
            )
        }
        bind() from provider {
            VdocipherViewModelFactory(
                instance()
            )
        }
    }
}