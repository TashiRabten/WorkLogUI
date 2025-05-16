package com.example.worklogui;

import javafx.scene.control.TextArea;

/**
 * Manages status messages in the UI
 */
public class StatusManager {

    private final TextArea statusArea;
    private String statusMessage = null;
    private final WarningManager warningManager;
    
    public StatusManager(TextArea statusArea, WarningManager warningManager) {
        this.statusArea = statusArea;
        this.warningManager = warningManager;
    }
    
    /**
     * Set a status message
     */
    public void setStatusMessage(String message) {
        this.statusMessage = message;
        updateStatusArea();
    }
    
    /**
     * Get the current status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * Show time summary from service 
     */
    public void showTimeSummary(CompanyManagerService service) {
        // Save existing warning
        String savedWarning = warningManager.getCurrentWarning();
        setStatusMessage(service.calculateTimeTotal());
        // Restore the warning
        warningManager.setWarning(savedWarning);
    }
    
    /**
     * Show earnings summary from service
     */
    public void showEarningsSummary(CompanyManagerService service) {
        // Save existing warning
        String savedWarning = warningManager.getCurrentWarning();
        setStatusMessage(service.calculateEarnings());
        // Restore the warning
        warningManager.setWarning(savedWarning);
    }
    
    /**
     * Show month/year summary from service
     */
    public void showSummaryByMonthAndYear(CompanyManagerService service) {
        String summary = service.getSummaryByMonthAndYear();
        setStatusMessage(summary);
        
        // Check for current month warning
        warningManager.checkWarningsAfterLogWork();
    }
    
    /**
     * Update the status area with current status and warning messages
     */
    private void updateStatusArea() {
        StringBuilder content = new StringBuilder();

        // Add warning if available (make sure warnings appear at the top)
        // Add warning if available (make sure warnings appear at the top)
        String currentWarning = warningManager.getCurrentWarning();
        if (currentWarning != null && !currentWarning.isEmpty()) {
            content.append(currentWarning);
        }

// Add status message if available
        if (statusMessage != null && !statusMessage.isEmpty()) {
            // Always put status message at top, before warning
            StringBuilder newContent = new StringBuilder();
            newContent.append(statusMessage);

            // If we have a warning, add spacing and then the warning
            if (content.length() > 0) {
                newContent.append("\n\n");
                newContent.append(content);
            }

            content = newContent;
        }

        // Update the status area
        statusArea.setText(content.toString());
    }
    
    /**
     * Set warning message handler in the warning manager
     */
    public void initialize() {
        warningManager.setWarningMessageHandler(warning -> updateStatusArea());
    }
}
