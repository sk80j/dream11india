package com.example.dream11india

object Lang {
    var isHindi = false
    val enterMobile get() = if (isHindi) "Mobile Number Dalein" else "Enter Mobile Number"
    val otpSent get() = if (isHindi) "OTP bheja jayega" else "OTP will be sent"
    val getOtp get() = if (isHindi) "OTP Prapt Karein" else "GET OTP"
    val verifyOtp get() = if (isHindi) "OTP Verify Karein" else "VERIFY OTP"
    val otpSentTo get() = if (isHindi) "OTP bheja gaya" else "OTP sent to"
    val verify get() = if (isHindi) "Verify Karein" else "VERIFY"
    val back get() = if (isHindi) "Wapas" else "Back"
    val selectLang get() = "Bhasha Chunein / Select Language"
    val numError get() = if (isHindi) "10 ank ka number dalein" else "Enter 10 digit number"
    val otpError get() = if (isHindi) "6 ank ka OTP dalein" else "Enter 6 digit OTP"
    val wrongOtp get() = if (isHindi) "Galat OTP" else "Wrong OTP"
}
