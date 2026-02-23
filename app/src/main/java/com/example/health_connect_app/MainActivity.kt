package com.example.health_connect_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.registerForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.example.health_connect_app.ui.theme.Health_connect_appTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    // เพิ่ม Permission สำหรับ HeartRateRecord
    private val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        val healthConnectClient = if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(this)
        } else {
            null
        }

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(permissions)) {
                // เมื่อได้สิทธิ์แล้ว อาจจะเรียกฟังก์ชันดึงข้อมูลที่นี่
            }
        }

        setContent {
            Health_connect_appTheme {
                var connectionStatus by remember { mutableStateOf("กำลังตรวจสอบ...") }
                // สร้างตัวแปรเก็บรายการ Heart Rate
                var heartRateLogs by remember { mutableStateOf<List<HeartRateRecord>>(emptyList()) }

                LaunchedEffect(Unit) {
                    connectionStatus = when (availabilityStatus) {
                        HealthConnectClient.SDK_AVAILABLE -> "พร้อมเชื่อมต่อ"
                        else -> "ไม่รองรับหรือต้องอัปเดตแอป"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "สถานะ: $connectionStatus", style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(onClick = {
                            lifecycleScope.launch {
                                val granted = healthConnectClient?.permissionController?.getGrantedPermissions()
                                if (granted?.containsAll(permissions) == true) {
                                    connectionStatus = "กำลังดึงข้อมูล Heart Rate..."

                                    // ดึงข้อมูลย้อนหลัง 24 ชั่วโมง
                                    val now = Instant.now()
                                    val startOfDay = now.minus(1, ChronoUnit.DAYS)

                                    val response = healthConnectClient.readRecords(
                                        ReadRecordsRequest(
                                            recordType = HeartRateRecord::class,
                                            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                                        )
                                    )
                                    heartRateLogs = response.records
                                    connectionStatus = "ดึงข้อมูลสำเร็จ (${heartRateLogs.size} รายการ)"
                                } else {
                                    requestPermissions.launch(permissions)
                                }
                            }
                        }) {
                            Text("ดึงข้อมูล Heart Rate")
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // ส่วนแสดงผลรายการ Heart Rate
                        Text(text = "รายการอัตราการเต้นของหัวใจ (24 ชม. ล่าสุด):")

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(heartRateLogs) { record ->
                                HeartRateItem(record)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeartRateItem(record: HeartRateRecord) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    // HeartRateRecord จะเก็บเป็น List ของ Samples (เพราะใน 1 ช่วงเวลาอาจมีหลายค่า)
    val avgBpm = record.samples.map { it.beatsPerMinute }.average().toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "เวลา: ${formatter.format(record.startTime)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "เฉลี่ย: $avgBpm BPM", style = MaterialTheme.typography.bodyLarge)
            }
            Text(text = "❤️", style = MaterialTheme.typography.headlineSmall)
        }
    }
}