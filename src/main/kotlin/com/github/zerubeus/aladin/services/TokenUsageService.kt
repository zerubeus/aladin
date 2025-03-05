package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.AppExecutorUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Service for tracking token usage and managing API limits.
 * This service ensures the plugin doesn't exceed configured usage limits.
 */
@Service(Service.Level.APP)
class TokenUsageService {
    private val logger = logger<TokenUsageService>()
    private var lastResetDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    private var resetTask: ScheduledFuture<*>? = null
    
    // Statistics tracking
    private var totalRequests: Long = 0
    private var failedRequests: Long = 0
    
    /**
     * Records token usage from an API request
     * @param tokenCount the number of tokens used in the request
     * @return true if the request was within limits, false if it exceeded limits
     */
    fun recordTokenUsage(tokenCount: Int): Boolean {
        val settings = service<ApiSettingsState>()
        
        // Ensure scheduler is running
        ensureSchedulerInitialized()
        
        // Check if we need to reset (safeguard in case scheduled task fails)
        checkAndResetIfNewDay()
        
        // Check if this would exceed the daily limit
        if (settings.tokenUsage + tokenCount > settings.dailyTokenLimit) {
            logger.warn("Token limit exceeded! ${settings.tokenUsage} + $tokenCount > ${settings.dailyTokenLimit}")
            failedRequests++
            return false
        }
        
        // Record the usage
        settings.recordTokenUsage(tokenCount)
        totalRequests++
        
        return true
    }
    
    /**
     * Checks if a request can be made without exceeding limits
     * @param estimatedTokens estimated token usage for the request
     * @return true if the request is within limits
     */
    fun canMakeRequest(estimatedTokens: Int): Boolean {
        val settings = service<ApiSettingsState>()
        
        // Ensure scheduler is running
        ensureSchedulerInitialized()
        
        checkAndResetIfNewDay()
        
        return settings.tokenUsage + estimatedTokens <= settings.dailyTokenLimit
    }
    
    /**
     * Gets current token usage statistics
     * @return a map with usage statistics
     */
    fun getUsageStatistics(): Map<String, Any> {
        val settings = service<ApiSettingsState>()
        
        // Ensure scheduler is running
        ensureSchedulerInitialized()
        
        return mapOf(
            "tokenUsage" to settings.tokenUsage,
            "dailyLimit" to settings.dailyTokenLimit,
            "percentUsed" to calculatePercentUsed(),
            "totalRequests" to totalRequests,
            "failedRequests" to failedRequests,
            "resetDate" to lastResetDate.toString()
        )
    }
    
    /**
     * Calculates the percentage of the daily limit that has been used
     */
    fun calculatePercentUsed(): Double {
        val settings = service<ApiSettingsState>()
        return if (settings.dailyTokenLimit > 0) {
            (settings.tokenUsage.toDouble() / settings.dailyTokenLimit.toDouble()) * 100.0
        } else {
            0.0
        }
    }
    
    /**
     * Checks if the current usage is approaching the configured limit
     */
    fun isApproachingLimit(): Boolean {
        return service<ApiSettingsState>().isApproachingLimit()
    }
    
    /**
     * Ensures the scheduler is initialized. This is called lazily on first use.
     */
    private fun ensureSchedulerInitialized() {
        if (resetTask == null) {
            scheduleUsageReset()
        }
    }
    
    /**
     * Checks if the current usage is approaching the configured limit
     */
    
    /**
     * Checks and resets the usage counter if it's a new day
     */
    private fun checkAndResetIfNewDay() {
        val today = LocalDate.now(ZoneId.systemDefault())
        if (!today.isEqual(lastResetDate)) {
            resetUsage()
            lastResetDate = today
        }
    }
    
    /**
     * Resets all usage counters
     */
    fun resetUsage() {
        logger.info("Resetting token usage counters")
        service<ApiSettingsState>().resetTokenUsage()
        lastResetDate = LocalDate.now(ZoneId.systemDefault())
    }
    
    /**
     * Schedules a daily task to reset the usage counter
     */
    private fun scheduleUsageReset() {
        // Calculate time until midnight
        val now = System.currentTimeMillis()
        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val initialDelay = midnight - now
        
        // Schedule the task
        val executor = AppExecutorUtil.getAppScheduledExecutorService()
        resetTask = executor.scheduleWithFixedDelay(
            { resetUsage() },
            initialDelay,
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MILLISECONDS
        )
        
        logger.info("Scheduled daily token usage reset")
    }
    
    /**
     * Cleanup when the service is disposed
     */
    fun dispose() {
        resetTask?.cancel(false)
        resetTask = null
    }
} 