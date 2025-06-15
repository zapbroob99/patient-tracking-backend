package com.rhtsystem.randevuhastatakip.model;

public enum AppointmentStatus {
    PENDING,    // Beklemede
    CONFIRMED,  // Onaylandı
    CANCELLED,  // İptal Edildi (Hasta veya Sistem tarafından)
    REJECTED    // Reddedildi (Doktor tarafından)
    // COMPLETED // Tamamlandı (Opsiyonel, randevu gerçekleştikten sonra)
}