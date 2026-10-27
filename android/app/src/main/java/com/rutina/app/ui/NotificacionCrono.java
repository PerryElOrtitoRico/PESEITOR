package com.rutina.app.ui;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.rutina.app.R;
import com.rutina.app.logica.TextosSesion;

import java.util.Objects;

/**
 * Notificación en curso del cronómetro de una sesión. El sistema la actualiza solo ({@code setUsesChronometer}), sin servicio
 * en primer plano: se arma con la hora en que "habría arrancado" (ahora menos lo contado).
 */
final class NotificacionCrono {

    private static final String CANAL = "cronometro";

    private NotificacionCrono() {
    }

    /** true si se pueden mostrar notificaciones (en Android 13 o más, si se dio el permiso). */
    static boolean permitido(Context ctx) {
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    private static int idDe(long plan, int micro, int dia) {
        return Objects.hash(plan, micro, dia);
    }

    /** Muestra o actualiza la notificación del cronómetro; si no hay permiso no hace nada. */
    @SuppressLint("MissingPermission")
    static void mostrar(Context ctx, long plan, int micro, int dia, long msContados) {
        if (!permitido(ctx)) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel(CANAL, "Cronómetro", NotificationManager.IMPORTANCE_LOW);
            canal.setDescription("Tiempo de la sesión en curso");
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
        Intent abrir = new Intent(ctx, SesionActivity.class);
        abrir.putExtra(SesionActivity.EXTRA_PLAN, plan);
        abrir.putExtra(SesionActivity.EXTRA_MICRO, micro);
        abrir.putExtra(SesionActivity.EXTRA_DIA, dia);
        PendingIntent toque = TaskStackBuilder.create(ctx).addNextIntentWithParentStack(abrir)
                .getPendingIntent(idDe(plan, micro, dia), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(TextosSesion.tituloSesion(micro, dia))
                .setContentText("Cronómetro en marcha")
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - msContados)
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(toque);
        NotificationManagerCompat.from(ctx).notify(idDe(plan, micro, dia), b.build());
    }

    /** Quita la notificación (al pausar o editar la duración). */
    static void quitar(Context ctx, long plan, int micro, int dia) {
        NotificationManagerCompat.from(ctx).cancel(idDe(plan, micro, dia));
    }
}
