package com.auracast.weather.data.notifications

// Phase 8 — FCM future-proofing (no backend needed for MVP)
// Uncomment Firebase Messaging dependency and extend FirebaseMessagingService when enabling.

// class AuraFcmService : com.google.firebase.messaging.FirebaseMessagingService() {
//     override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
//         // Route via WeatherNotifier
//     }
//     override fun onNewToken(token: String) {
//         // Store token for future targeted campaigns via Firebase console
//     }
// }

// Stub so Hilt graph doesn't require firebase-messaging yet
class FcmServiceStub
