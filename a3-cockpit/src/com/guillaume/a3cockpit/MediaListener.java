package com.guillaume.a3cockpit;

import android.service.notification.NotificationListenerService;

/** Nécessaire pour lire le titre en cours (accès aux notifications accordé par l'utilisateur). */
public class MediaListener extends NotificationListenerService {
    @Override public void onListenerConnected() {
        MediaHub.refresh(this);
    }
}
