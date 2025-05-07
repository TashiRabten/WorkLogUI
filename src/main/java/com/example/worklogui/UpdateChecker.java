package com.example.worklogui;

public class UpdateChecker {
    // Esta classe agora serve apenas como wrapper para o novo AutoUpdater
    // para manter a compatibilidade com código existente

    public static void checkForUpdates() {
        AutoUpdater.checkForUpdates();
    }
}