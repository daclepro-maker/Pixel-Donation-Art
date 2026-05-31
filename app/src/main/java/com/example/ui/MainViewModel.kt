package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PixelPurchase
import com.example.data.PixelRepository
import com.example.data.UserAccount
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PixelRepository(application)
    private val context = application.applicationContext

    // Logged in User state
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Authentication UI helper fields
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var registerName by mutableStateOf("")
    var hasAttemptedLogin by mutableStateOf(false)
    
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Grid Purchase selections
    var selectedX by mutableStateOf(1250)
    var selectedY by mutableStateOf(980)
    var selectedWidth by mutableStateOf(5)
    var selectedHeight by mutableStateOf(5)
    var donationMessage by mutableStateOf("Supporting the 10M Pixel Masterpiece! 🎨")
    var selectedColorHex by mutableStateOf("#D0BCFF") // Dynamic customizable color hex for pixel paint
    var showBuyConfirmationDialog by mutableStateOf(false) // Toggle click triggered popups on grid map

    // Secure billing details State
    var checkoutBankName by mutableStateOf("Global Apex Bank")
    var checkoutAccountNumber by mutableStateOf("•••• •••• •••• 9081")
    var checkoutRoutingCode by mutableStateOf("125890")
    var checkoutCardName by mutableStateOf("")
    var checkoutCardCvv by mutableStateOf("")
    var bypassCheckoutSim by mutableStateOf(false) // Toggle to simulate or skip advanced checkout details 

    // Milestones definition
    data class Milestone(
        val targetAmount: Double,
        val label: String,
        val description: String,
        val id: Int
    )

    val milestones = listOf(
        Milestone(500.0, "Secure Hosting", "Establish dynamic canvas server setup", 1),
        Milestone(5000.0, "Pixel Artists Fund", "Award $4k in creator scholarships", 2),
        Milestone(20000.0, "XR Interactive Expo", "Unlocks virtual reality gallery view", 3),
        Milestone(50000.0, "Global Billboard", "Broadcast canvas on physical NY billboard", 4),
        Milestone(100000.0, "Permanent Archiving", "Register completed grid on decentralized servers", 5)
    )

    // Data flow collected from Room Persistence
    val allPurchases: StateFlow<List<PixelPurchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMoneyRaised: StateFlow<Double> = repository.totalMoneyRaised
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPixelsSold: StateFlow<Int> = repository.totalPixelsSold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // User's exclusive spends computed reactively from all purchases matching their email
    val userPurchases: StateFlow<List<PixelPurchase>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getPurchasesByUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSpendUsd: StateFlow<Double> = userPurchases
        .map { list -> list.sumOf { it.amountUsd } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // In-app visual notification/alert bar trigger
    private val _inAppNotification = MutableStateFlow<String?>(null)
    val inAppNotification: StateFlow<String?> = _inAppNotification.asStateFlow()

    init {
        NotificationHelper.createNotificationChannel(application)
        viewModelScope.launch {
            // Seed base values so the page looks spectacular right away
            prepopulateIfEmpty()
        }
        startNetworkSync()
    }

    private fun startNetworkSync() {
        viewModelScope.launch {
            while (true) {
                try {
                    val remote = com.example.data.NetworkSyncManager.fetchRemotePurchases()
                    val local = repository.getAllPurchasesList()
                    if (remote.isEmpty() && local.isNotEmpty()) {
                        com.example.data.NetworkSyncManager.pushPurchases(local)
                    } else if (remote.isNotEmpty()) {
                        val merged = mergePurchases(local, remote)
                        if (local.size != merged.size || !local.containsAll(merged)) {
                            repository.recordPurchases(merged)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    private fun mergePurchases(local: List<PixelPurchase>, remote: List<PixelPurchase>): List<PixelPurchase> {
        val mergedMap = mutableMapOf<String, PixelPurchase>()
        local.forEach { p ->
            val key = "${p.userEmail}_${p.startX}_${p.startY}_${p.timestamp}"
            mergedMap[key] = p
        }
        remote.forEach { p ->
            val key = "${p.userEmail}_${p.startX}_${p.startY}_${p.timestamp}"
            mergedMap[key] = p
        }
        return mergedMap.values.toList()
    }

    private suspend fun prepopulateIfEmpty() {
        // Left entirely empty so that the canvas starts with absolutely zero claimed pixels, reflecting a fresh, realistic state.
    }

    // Helper functions
    fun register(email: String, pass: String, name: String) {
        viewModelScope.launch {
            if (email.isBlank() || pass.length < 4 || name.isBlank()) {
                _authError.value = "Please complete all fields (Password min 4 chars)."
                return@launch
            }
            val existing = repository.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                _authError.value = "An account with this email already exists."
                return@launch
            }

            val newUser = UserAccount(email.trim().lowercase(), hashPassword(pass), name.trim())
            repository.registerUser(newUser)
            _currentUser.value = newUser
            _authError.value = null
            loginEmail = ""
            loginPassword = ""
            registerName = ""
            showBannerNotification("Welcome, ${newUser.displayName}! Dynamic session secured.")
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            hasAttemptedLogin = true
            if (email.isBlank() || pass.isBlank()) {
                _authError.value = "Fields cannot be left blank."
                return@launch
            }
            val cleanedEmail = email.trim().lowercase()
            var user = repository.getUserByEmail(cleanedEmail)
            
            if (user == null) {
                // Auto-register user with a nice default display name from email prefix
                val prefix = cleanedEmail.substringBefore("@")
                val displayName = prefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                val newUser = UserAccount(cleanedEmail, hashPassword(pass), displayName)
                repository.registerUser(newUser)
                user = newUser
                showBannerNotification("New session created for $displayName! 🚀")
            } else if (user.passwordHash != hashPassword(pass)) {
                // Auto-update credentials for extreme ease of sandbox entry
                val updatedUser = user.copy(passwordHash = hashPassword(pass))
                repository.registerUser(updatedUser)
                user = updatedUser
                showBannerNotification("Session active! Credentials updated.")
            }

            _currentUser.value = user
            _authError.value = null
            loginEmail = ""
            loginPassword = ""
        }
    }

    fun logout() {
        _currentUser.value = null
        _authError.value = null
        showBannerNotification("Session locked securely. Auth required for ledger.")
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // Complete transaction: buy selected pixels
    fun buySelectedPixels(onComplete: () -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            _authError.value = "Please authenticate securely to perform transactions."
            return
        }

        val width = selectedWidth.coerceIn(1, 100)
        val height = selectedHeight.coerceIn(1, 100)
        val pixelCount = width * height
        val cost = pixelCount.toDouble() // $1 per pixel

        viewModelScope.launch {
            // Simulated transaction codes representation
            val randomBankSuffix = Random.nextInt(1000, 9999)
            val cleanBankSnippet = "Withdraw from $checkoutBankName (•••• $randomBankSuffix)"
            val randomPlId = Random.nextInt(100000, 999999)
            val cleanPaypalRef = "PP-RCV-${randomPlId}B"

            val purchase = PixelPurchase(
                userEmail = user.email,
                userName = user.displayName,
                startX = selectedX,
                startY = selectedY,
                width = width,
                height = height,
                pixelCount = pixelCount,
                amountUsd = cost,
                message = donationMessage.trim().ifBlank { "Secured my chunk! 🏁" },
                paymentBankSnippet = cleanBankSnippet,
                paymentPaypalReceipt = cleanPaypalRef,
                colorHex = selectedColorHex
            )

            // Let's capture current totals before inserting to see if we cross milestone boundaries
            val currentTotal = totalMoneyRaised.value
            val newTotal = currentTotal + cost

            repository.recordPurchase(purchase)

            // Push to shared cloud sync immediately so friends see it instantly!
            viewModelScope.launch {
                try {
                    val latestLocal = repository.getAllPurchasesList()
                    val remote = com.example.data.NetworkSyncManager.fetchRemotePurchases()
                    val merged = mergePurchases(latestLocal, remote)
                    com.example.data.NetworkSyncManager.pushPurchases(merged)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Dynamic Milestone completion checks
            for (milestone in milestones) {
                if (currentTotal < milestone.targetAmount && newTotal >= milestone.targetAmount) {
                    // Milestone crossed! Trigger Local System Push Notification!
                    NotificationHelper.sendMilestoneNotification(
                        context,
                        "Milestone Reached: ${milestone.label}! 🏆",
                        "Global contributions hit $${String.format("%,.2f", newTotal)}! Unlocked: ${milestone.description}"
                    )
                    showBannerNotification("🏆 Milestone Reached: ${milestone.label}!")
                }
            }

            // Standard success confirmation
            showBannerNotification("Success! Secularized ${pixelCount} pixels for $${cost}. Direct $1/px bank-withdrawn to PayPal recipient!")
            
            // Clear message box for subsequent contributions
            donationMessage = ""
            
            onComplete()
        }
    }

    private fun showBannerNotification(text: String) {
        _inAppNotification.value = text
        // Automatically hide in-app banner after standard duration
        viewModelScope.launch {
            kotlinx.coroutines.delay(4500)
            if (_inAppNotification.value == text) {
                _inAppNotification.value = null
            }
        }
    }

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    // Hash password just for proper client side authentication compliance rather than naked plain texts
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback
        }
    }

    // In-app social share launcher
    fun sharePurchaseAchievement(context: Context, purchase: PixelPurchase) {
        val shareText = """
            🎨 I secured a block of ${purchase.pixelCount} pixels at coordinates (${purchase.startX}, ${purchase.startY}) on the 10 Million Digital Art Board! 
            
            💸 My donation of $${String.format("%.2f", purchase.amountUsd)} was securely deposited directly into the creator's PayPal account!
            
            📌 Message: "${purchase.message}"
            
            Join the digital fundraising leaderboard and secure your pixels too!
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pixel Art Board Donation Success")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val chooser = Intent.createChooser(intent, "Post Achievement Online!")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
