package app.flipday

/** Contact details shown on the "About the developer" screen. Provided by the developer for this purpose. */
object DeveloperInfo {
    /** Wrapped in left-to-right isolates so the number does not scramble inside Arabic (RTL) text. */
    const val WHATSAPP_DISPLAY = "\u2066+963 941 874 763\u2069"
    const val WHATSAPP_URL = "https://wa.me/963941874763"
    const val EMAIL = "damermahmoud@gmail.com"
    /** Same isolate wrapping for display of the address. */
    const val EMAIL_DISPLAY = "\u2066$EMAIL\u2069"
}
