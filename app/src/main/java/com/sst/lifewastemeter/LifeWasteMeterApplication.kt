package com.sst.lifewastemeter

import android.app.Application
import com.sst.lifewastemeter.data.repository.UsageRepository

class LifeWasteMeterApplication : Application() {
    val repository: UsageRepository by lazy {
        UsageRepository(this)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}





