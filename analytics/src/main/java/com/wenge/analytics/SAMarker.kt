package com.wenge.analytics

import android.content.Context
import android.text.TextUtils
import android.view.View
import com.sensorsdata.analytics.android.sdk.*
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils
import org.json.JSONException
import org.json.JSONObject

/**
 * @Author gewe
 * @create 2021/4/9 15:13
 * @description：
 */
object SAMarker {
    /**
     * @param url  神策地址   正式地址、测试地址
     * @param app_tag App 唯一标识  62
     * @param config   BuildConfig.DEBUG
     * @param ctx App Context
     * @param uid
     */
    @JvmStatic
    fun initSensors(url: String, app_tag: String, config: Boolean, ctx: Context, uid: String?) {
        // 初始化配置
        val saConfigOptions = SAConfigOptions(url)
        // 开启全埋点
        saConfigOptions.setAutoTrackEventType(
            SensorsAnalyticsAutoTrackEventType.APP_CLICK or
                    SensorsAnalyticsAutoTrackEventType.APP_START or
                    SensorsAnalyticsAutoTrackEventType.APP_END or
                    SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN
        )
            .enableLog(config)
            .enableTrackAppCrash()
        // 需要在主线程初始化神策 SDK
        SensorsDataAPI.startWithConfigOptions(ctx, saConfigOptions)
        // 初始化 SDK 之后，开启自动采集 Fragment 页面浏览事件
        SensorsDataAPI.sharedInstance().trackFragmentAppViewScreen()

        // 将应用名称作为事件公共属性，后续所有 track() 追踪的事件都会自动带上 "AppName" 属性
        try {
            val properties = JSONObject()
            properties.put("use_name", app_tag) //应用名称
            properties.put("Platform_type", "Android") //平台类型
            SensorsDataAPI.sharedInstance().registerSuperProperties(properties)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        //初始化 SDK 之后，开启点击图
        SensorsDataAPI.sharedInstance().enableHeatMap()

        /**
         * 匿名 ID 和登录  uid 关联
         */
        if (!uid.isNullOrEmpty()) {
            bindUid(uid)
        }
    }

    @JvmStatic
    fun markClick(view: View, tag: String) {
        try {
            val properties = JSONObject()
            properties.put("\$element_content", tag)
            SensorsDataAPI.sharedInstance().setViewProperties(view, properties)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun markClickIcon(function_id: String, icon_name: String, title: String) {
        val hashMap = HashMap<String, Any>()
        hashMap["function_id"] = function_id
        hashMap["icon_name"] = icon_name
        hashMap["${'$'}title"] = title
        trackCustom("ClickIcon", hashMap)
    }

    /**
     * 3. 追踪事件
     *
     * @param eventName 属性名 必需是 String 类型
     * @param map       属性的值，支持 String、Number、Boolean、JSONArray 和 Date，对于 JSONArray，其中所有元素必须是 String 类型
     */
    @JvmStatic
    fun trackCustom(eventName: String, map: Map<String, Any>) {
        try {
            val properties = JSONObject()
            for ((key, value) in map) {
                properties.put(key, value)
            }
            SensorsDataAPI.sharedInstance().track(eventName, properties)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 匿名 ID 和登录 ID 关联
     */
    @JvmStatic
    fun bindUid(uid: String) {
        //注册成功、登录成功、初始化SDK后  调用 login 传入登录 ID
        //"你们服务端分配给用户具体的登录 ID"
        SensorsDataAPI.sharedInstance().login(uid.toString())
    }

    /**
     * 记录激活事件
     * @param download_channel
     * @param download_appname
     * @param download_appstore
     * @param search
     */
    @JvmStatic
    fun trackInstallation(
        download_channel: String?,
        download_appname: String?,
        download_appstore: String?,
        search: String?
    ) {
        try {
            val properties = JSONObject()
            //这里的 DownloadChannel 负责记录下载商店的渠道，值应传入具体应用商店包的标记。如果没有为不同商店打多渠道包，则可以忽略该属性的代码示例。
            properties.put("download_channel", "$download_channel")
            properties.put("channel_source", "app")
            properties.put("download_appname", "$download_appname")
            properties.put("download_appstore", "$download_appstore")
            properties.put("search", search)
            // 触发激活事件
            SensorsDataAPI.sharedInstance().trackInstallation("AppInstall", properties)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 手动出发 页面浏览事件
     * @param aorf  Activity  or  Fragment
     * @param title   $title
     */
    @JvmStatic
    fun trackViewScreen(aorf: Any, title: String) {
        val propertyJSON = JSONObject()
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, aorf.javaClass.canonicalName)
            if (!TextUtils.isEmpty(title)) {
                propertyJSON.put(AopConstants.TITLE, title)
            }
            if (aorf is ScreenAutoTracker) {
                val screenAutoTracker = aorf as ScreenAutoTracker
                val trackProperties = screenAutoTracker.trackProperties
                if (trackProperties != null) {
                    SensorsDataUtils.mergeJSONObject(trackProperties, propertyJSON)
                }
            }
            SensorsDataAPI.sharedInstance()
                .trackViewScreen(SensorsDataUtils.getScreenUrl(aorf), propertyJSON)
        } catch (ex: java.lang.Exception) {
            SALog.printStackTrace(ex)
        }
    }

    /**
     * https://manual.sensorsdata.cn/sa/latest/tech_sdk_client_android_super-22256121.html
     *  该方法并不会真正发送事件；在事件结束时，调用 trackTimerEnd("Event", properties)，
     *  SDK 会触发 "Event" 事件，并自动将事件持续时间记录在事件属性 "$event_duration" 中
     *  @param event 事件英文变量名 (finish_game)
     */
    @JvmStatic
    fun trackTimerStart(event: String) {
        SensorsDataAPI.sharedInstance().trackTimerStart(event)
    }

    @JvmStatic
    fun trackTimerEnd(event: String, map: HashMap<String, Any>) {
        try {
            // 事件特有属性
            map.apply {
                val properties = JSONObject()
                forEach {
                    properties.put(it.key, it.value)
                }
                SensorsDataAPI.sharedInstance().trackTimerEnd(event, properties)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}