package fr.cockpit.auto

import android.app.Application
import fr.cockpit.auto.data.Prefs

class CockpitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
