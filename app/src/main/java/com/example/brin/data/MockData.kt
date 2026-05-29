package com.example.brin.data

enum class BinStatus { CRITICAL, NEED_PICKUP, NORMAL }
enum class TaskStatus { PENDING, IN_PROGRESS, DONE }

data class BinData(
    val id: String,
    val nodeId: String     = id,    // display ID (API nodeId); defaults to id for backward compat
    val location: String,
    val area: String       = "Bandung",
    val capacity: Int,
    val status: BinStatus,
    val lastUpdate: String,
    val battery: Int       = 80,
    val gas: Double        = 0.0,   // gas sensor ppm (replaces temperature)
    val lat: Double        = -6.9175,
    val lng: Double        = 107.6191,
    val alertText: String  = ""
)

data class PickupRoute(
    val id: String,
    val zone: String,
    val binCount: Int,
    val startTime: String,
    val status: TaskStatus,
    val completedCount: Int   = 0,
    val bins: List<BinData>   = emptyList()
)

data class NotificationItem(
    val id: String,
    val binId: String,
    val title: String,
    val message: String,
    val time: String,
    val type: BinStatus,
    val isRead: Boolean = false
)

object MockData {
    val bins = listOf(
        BinData("BIN-042", "BIN-042", "Darmo Kali", "Bandung", 96, BinStatus.CRITICAL,
            "2 menit yang lalu", 60, 48.0, -6.9100, 107.6050, "Segera dikosongkan"),
        BinData("BIN-089", "BIN-089", "Gubeng",     "Bandung", 78, BinStatus.NEED_PICKUP,
            "5 menit yang lalu", 75, 42.0, -6.9050, 107.6200),
        BinData("BIN-055", "BIN-055", "Rungkut",    "Bandung",  0, BinStatus.NORMAL,
            "1 jam yang lalu",   90, 35.0, -6.9300, 107.6100),
        BinData("BIN-103", "BIN-103", "Cibuntu",    "Bandung", 45, BinStatus.NORMAL,
            "30 menit yang lalu", 85, 36.0, -6.9200, 107.5900),
        BinData("BIN-128", "BIN-128", "Sukajadi",   "Bandung", 20, BinStatus.NORMAL,
            "45 menit yang lalu", 70, 34.0, -6.8950, 107.5950),
        BinData("BIN-145", "BIN-145", "Pasteur",    "Bandung", 10, BinStatus.NORMAL,
            "1 jam yang lalu",   95, 33.0, -6.8900, 107.6000)
    )

    val routes = listOf(
        PickupRoute("TRK-01", "Zona A", 6, "08.00", TaskStatus.PENDING, 0,
            bins.filter { it.id in listOf("BIN-042","BIN-089","BIN-103","BIN-128","BIN-145","BIN-055") }),
        PickupRoute("TRK-02", "Zona B", 4, "09.30", TaskStatus.IN_PROGRESS, 2,
            bins.filter { it.id in listOf("BIN-089","BIN-042","BIN-103","BIN-055") }),
        PickupRoute("TRK-03", "Zona C", 6, "07.45", TaskStatus.DONE, 6, bins),
        PickupRoute("TRK-04", "Zona D", 5, "11.00", TaskStatus.PENDING, 0,
            bins.filter { it.id in listOf("BIN-128","BIN-145","BIN-055","BIN-103","BIN-089") }),
        PickupRoute("TRK-05", "Zona E", 3, "13.00", TaskStatus.PENDING, 0,
            bins.filter { it.id in listOf("BIN-042","BIN-128","BIN-145") })
    )

    val notifications = listOf(
        NotificationItem("N-001", "BIN-042", "Bin Kritis",     "Kapasitas 96% — segera dikosongkan",   "2 mnt lalu",  BinStatus.CRITICAL),
        NotificationItem("N-002", "BIN-089", "Perlu Pickup",   "Kapasitas 78% — area Gubeng",          "5 mnt lalu",  BinStatus.NEED_PICKUP),
        NotificationItem("N-003", "BIN-042", "Gas Tinggi",     "Gas 48 ppm melebihi batas normal",     "8 mnt lalu",  BinStatus.CRITICAL),
        NotificationItem("N-004", "BIN-017", "Bin Kritis",     "Kapasitas 91% — area Dago",            "15 mnt lalu", BinStatus.CRITICAL),
        NotificationItem("N-005", "BIN-103", "Perlu Pickup",   "Kapasitas 75% — area Cibuntu",         "22 mnt lalu", BinStatus.NEED_PICKUP),
        NotificationItem("N-006", "BIN-056", "Baterai Lemah",  "Baterai lemah 12%",                    "30 mnt lalu", BinStatus.NEED_PICKUP),
        NotificationItem("N-007", "BIN-128", "Perlu Pickup",   "Kapasitas 70% — area Sukajadi",        "35 mnt lalu", BinStatus.NEED_PICKUP),
        NotificationItem("N-008", "BIN-021", "Bin Kritis",     "Kapasitas 88% — area Coblong",         "45 mnt lalu", BinStatus.CRITICAL),
        NotificationItem("N-009", "BIN-074", "Perlu Pickup",   "Kapasitas 65% — area Antapani",        "1 jam lalu",  BinStatus.NEED_PICKUP),
        NotificationItem("N-010", "BIN-055", "Pickup Selesai", "TRK-03 menyelesaikan pickup Zona C",   "2 jam lalu",  BinStatus.NORMAL,      isRead = true),
        NotificationItem("N-011", "BIN-145", "Perlu Pickup",   "Kapasitas 62% — area Pasteur",         "2 jam lalu",  BinStatus.NEED_PICKUP, isRead = true),
    )

    fun getBinById(id: String)   = bins.find { it.id == id }
    fun getRouteById(id: String) = routes.find { it.id == id }
}
