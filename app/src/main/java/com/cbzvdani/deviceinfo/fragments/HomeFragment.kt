package com.cbzvdani.deviceinfo.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Camera
import android.hardware.Camera.open
import android.hardware.camera2.CameraManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cbzvdani.deviceinfo.R
import com.cbzvdani.deviceinfo.models.FeaturesHWModel
import com.cbzvdani.deviceinfo.utils.KeyUtil
import com.cbzvdani.deviceinfo.utils.Methods
import kotlinx.android.synthetic.main.battery_info.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.internal_storage.*
import kotlinx.android.synthetic.main.os_info_processor_info.*
import java.io.File
import java.nio.channels.AsynchronousFileChannel.open
import java.nio.channels.AsynchronousServerSocketChannel.open
import java.nio.channels.DatagramChannel.open
import java.text.DateFormatSymbols
import java.text.DecimalFormat
import java.util.*
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
    var manufacture: String?=null
    var model: String?=null
    var serial:String?= null

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
        getDeviceName()
        getOSInfo()
        getProcessorName()

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
    private fun checkCameraPermission(ids: String) {
        //if build version is higher than marshmallow
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasWriteCameraPermission = activity!!.checkSelfPermission(Manifest.permission.CAMERA)
            if (hasWriteCameraPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), KeyUtil.KEY_CAMERA_CODE)
            } else {
                fetchCameraCharacteristics(cameraManager!!, ids)
            }
        } else {
            fetchCameraCharacteristics(cameraManager!!, ids)
        }
    }

    private fun fetchCameraCharacteristics(cameraManager: CameraManager, ids: String) {
        val lists = ArrayList<FeaturesHWModel>()
        val characteristics = cameraManager.getCameraCharacteristics(ids)
        for (key in characteristics.keys) {

        }

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



    @SuppressLint("SetTextI18n")
    private fun getDeviceName() {
        manufacture = Build.MANUFACTURER
        model = Build.MANUFACTURER
        val osInstalledDate = Date(Build.TIME)
        val calendar: Calendar = GregorianCalendar()
        calendar.time = osInstalledDate
        if (model?.toLowerCase()!!.startsWith(manufacture!!.toLowerCase())) {
            txtModel.text =  capitalize(model)
        }else{
            txtModel.text = capitalize(manufacture) + " " + model
        }
        var month  = calendar.get(Calendar.MONTH) + 1
        var months  = DateFormatSymbols().getMonths().get(month - 1)
        txtDateOsInstalled.text  =  months.toString() + " " + calendar.get(Calendar.YEAR).toString()
    }
    private fun getProcessorName(){
        try {
            val s = Scanner(File("/proc/cpuinfo"))
            while (s.hasNextLine()) {
                val vals = s.nextLine().split(": ")
                if (vals.size > 1){
                   // Log.d("karir",vals[0])
                    if(vals[0].contains("model name")){
                        txt_user_processor.text = vals[1].toString()

                    }
                }
            }
        } catch (e: Exception) {
            Log.e("getCpuInfoMap", Log.getStackTraceString(e))
        }

    }

    @SuppressLint("SetTextI18n")
    private fun getOSInfo() {

        //get from os library
        val CVersion = Build.VERSION.SDK_INT
        when (CVersion) {
            11 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.honeycomb)} ${Build.VERSION.RELEASE}"""

                )

            }
            12 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.honeycomb)} ${Build.VERSION.RELEASE}"""
                )

            }
            13 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.honeycomb)} ${Build.VERSION.RELEASE}"""
                )

            }
            14 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.ics)} ${Build.VERSION.RELEASE}"""
                )

            }
            15 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.ics)} ${Build.VERSION.RELEASE}"""
                )

            }
            16 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.jellybean)} ${Build.VERSION.RELEASE}"""
                )

            }
            17 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.jellybean)} ${Build.VERSION.RELEASE}"""
                )

            }
            18 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.jellybean)} ${Build.VERSION.RELEASE}"""
                )

            }
            19 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.kitkat)} ${Build.VERSION.RELEASE}"""
                )

            }
            21 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.lollipop)} ${Build.VERSION.RELEASE}"""
                )

            }
            22 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.lollipop)} ${Build.VERSION.RELEASE}"""
                )

            }
            23 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.marshmallow)} ${Build.VERSION.RELEASE}"""
                )

            }
            24 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.nougat)} ${Build.VERSION.RELEASE}"""
                )

            }
            25 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.nougat)} ${Build.VERSION.RELEASE}"""
                )

            }
            26 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.oreo)} ${Build.VERSION.RELEASE}"""
                )

            }
            27 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.oreo)} ${Build.VERSION.RELEASE}"""
                )

            }
            28 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.pie)} ${Build.VERSION.RELEASE}"""
                )

            }
            29 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.ten)} ${Build.VERSION.RELEASE}"""
                )

            }
            30 -> {
                tvVersionName.setText(
                    """${resources.getString(R.string.eleven)} ${Build.VERSION.RELEASE}"""
                )

            }
            else -> {
                tvVersionName.setText(resources.getString(R.string.unknown_version))

            }
        }

    }
    private fun capitalize(s: String?): String? {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
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
