package com.rhtsystem.randevuhastatakip.repository;

import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.AppointmentStatus;
import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Hasta için randevular
    List<Appointment> findByPatientOrderByAppointmentDateTimeDesc(Patient patient);
    List<Appointment> findByPatientAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(Patient patient, LocalDateTime dateTime); // Gelecek randevular
    List<Appointment> findByPatientAndAppointmentDateTimeBeforeOrderByAppointmentDateTimeDesc(Patient patient, LocalDateTime dateTime); // Geçmiş randevular

    // Doktor için randevular
    List<Appointment> findByDoctorOrderByAppointmentDateTimeAsc(Doctor doctor);
    List<Appointment> findByDoctorAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(Doctor doctor, LocalDateTime startDateTime, LocalDateTime endDateTime); // Belirli tarih aralığındaki randevular
    List<Appointment> findByDoctorAndStatusOrderByAppointmentDateTimeAsc(Doctor doctor, AppointmentStatus status); // Durumuna göre doktorun randevuları

    // Çakışan randevu kontrolü için
    Optional<Appointment> findByDoctorAndAppointmentDateTime(Doctor doctor, LocalDateTime appointmentDateTime);

    // Daha karmaşık bir çakışma kontrolü (örneğin, randevu süresi varsa) için JPQL örneği:
    // Bu örnek, randevu süresinin 30 dakika olduğunu varsayar.
    // Yeni randevunun başlangıç veya bitiş saati, mevcut bir randevunun içine denk gelmemeli
    // veya yeni randevu, mevcut bir randevuyu tamamen kapsamamalı.
    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor " +
           "AND a.status != 'CANCELLED' AND a.status != 'REJECTED' " + // Sadece aktif/onaylı randevuları kontrol et
           "AND ( (a.appointmentDateTime <= :newAppointmentStart AND a.appointmentDateTime > :newAppointmentStartMinusDuration) OR " + // Yeni randevu, mevcut bir randevunun bitimine yakın başlarsa
           "      (a.appointmentDateTime >= :newAppointmentStart AND a.appointmentDateTime < :newAppointmentEnd) )") // Yeni randevu, mevcut bir randevunun başlangıcına yakın başlarsa veya içine girerse
    List<Appointment> findOverlappingAppointments(@Param("doctor") Doctor doctor,
                                                  @Param("newAppointmentStart") LocalDateTime newAppointmentStart,
                                                  @Param("newAppointmentEnd") LocalDateTime newAppointmentEnd,
                                                  @Param("newAppointmentStartMinusDuration") LocalDateTime newAppointmentStartMinusDuration);


    // Sadece belirli bir doktorun belirli bir saatteki randevusunu kontrol etmek için (üstteki findByDoctorAndAppointmentDateTime daha basit ve yeterli olabilir)
    boolean existsByDoctorAndAppointmentDateTimeAndStatusIn(Doctor doctor, LocalDateTime appointmentDateTime, List<AppointmentStatus> statuses);

}