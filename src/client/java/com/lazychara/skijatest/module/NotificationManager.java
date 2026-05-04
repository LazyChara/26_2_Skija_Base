package com.lazychara.skijatest.module;
public class NotificationManager {
    public static String currentNotification = "";
    public static float notifTimer = 0f;
    public static void show(String message) {
        currentNotification = message;
        notifTimer = 1.3f;
    }
}
