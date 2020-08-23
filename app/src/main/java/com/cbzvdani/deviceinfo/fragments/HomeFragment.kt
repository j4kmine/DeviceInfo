package com.cbzvdani.deviceinfo.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Camera
import android.hardware.camera2.CameraManager
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cbzvdani.deviceinfo.R
import com.cbzvdani.deviceinfo.utils.Methods
import kotlinx.android.synthetic.main.battery_info.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.internal_storage.*
import java.text.DecimalFormat
import kotlin.jvm.internal.Intrinsics


/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {
    var camera: Camera? = null
    var cameraManager: CameraManager? = null
    var activityManager: ActivityManager? = null
    var health = 0
    var icon_small = 0
    var level = 0
    var plugged = 0
    var present = false
    var scale = 0
    var status = 0
    var technology: String? = null
    var temperature = 0
    var voltage = 0
    var deviceStatus = 0
    var batteryCapacity = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val totalRamValue = totalRamMemorySize()
        val freeRamValue = freeRamMemorySize()
        val usedRamValue = totalRamValue - freeRamValue
        arc_ram.setProgress(
            Methods.calculatePercentage(
                usedRamValue.toDouble(),
                totalRamValue.toDouble()
            )
        )
        initUI()
    }
    //get ram info total
    private fun totalRamMemorySize(): Long {
        activityManager =
            activity!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

    //get ram info which free
    private fun freeRamMemorySize(): Long {
        activityManager =
            activity!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }
    private fun initUI() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        activity!!.registerReceiver(mBatInfoReceiver, filter)
        getMemoryInfo()
        getInternalStroge()

    }
    val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            deviceStatus = intent!!.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
            icon_small = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0)

            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            present = intent.extras!!.getBoolean(BatteryManager.EXTRA_PRESENT)

            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0)

            technology = intent.extras!!.getString(BatteryManager.EXTRA_TECHNOLOGY)
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10

            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            try {
                getBatteryInfo()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun getBatteryInfo(){
        var capacityMah = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mBatteryManager =
                activity!!.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val chargeCounter =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            val capacity =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (chargeCounter != null && capacity != null) {
                capacityMah = (chargeCounter.toFloat() / capacity.toFloat() * 100f).toInt()
            }
            txtBatteryMah.setText(
                " $capacityMah " + "mAh"
            )
        }

        txtUserPersentBattery.setText("$level%")
        progressBarInternalBattery.setProgress(level)
    }
  
    @SuppressLint("SetTextI18n")
    private fun getInternalStroge()
    {
        val totalInternalValue: Long = getTotalInternalMemorySize()
        val freeInternalValue: Long = getAvailableInternalMemorySize()
        val usedInternalValue = totalInternalValue - freeInternalValue
        val value: Int = Methods.calculatePercentage(usedInternalValue.toDouble(), totalInternalValue.toDouble())
        progressBarInternalStroge.setProgress(value)
        txtUserPersent.setText("$value%")
        txtStrogeSpace.setText(
            resources.getString(R.string.total) + "  " + formatSize(
                totalInternalValue
            ) + " ,  " + resources.getString(
                R.string.free
            ) + " " + formatSize(freeInternalValue)
        )
    }
    private fun getTotalInternalMemorySize(): Long {
        val path = Environment.getDataDirectory()
        Intrinsics.checkExpressionValueIsNotNull(path, "path")
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        return totalBlocks * blockSize
    }
    private fun getAvailableInternalMemorySize(): Long {
        val path = Environment.getDataDirectory()
        Intrinsics.checkExpressionValueIsNotNull(path, "path")
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.availableBlocksLong
        return totalBlocks * blockSize
    }
    @SuppressLint("SetTextI18n")
    private fun getMemoryInfo()
    {
        val totalRamValue = totalRamMemorySize()
        val freeRamValue = freeRamMemorySize()
        val usedRamValue = totalRamValue - freeRamValue
        tv_system_apps_memory.text =
            resources.getString(R.string.system_and_apps) + " : " + formatSize(
                usedRamValue
            )
        tv_available_ram.text = resources.getString(R.string.available_ram) + " : " + formatSize(
            freeRamValue
        )
        tv_total_ram_space.text =
            resources.getString(R.string.total_ram_space) + " : " + formatSize(
                totalRamValue
            )
    }
    private fun formatSize(size: Long): String {
        return if (size <= 0L) {
            "0"
        } else {
            val units =
                arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups =
                (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            DecimalFormat("#,##0.#").format(
                size.toDouble() / Math.pow(
                    1024.0,
                    digitGroups.toDouble()
                )
            ) + " " + units[digitGroups]
        }
    }
}
