package info.nightscout.androidaps.plugins.general.automation

import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.general.automation.actions.Action
import info.nightscout.androidaps.plugins.general.automation.actions.ActionDummy
import info.nightscout.androidaps.plugins.general.automation.triggers.Trigger
import info.nightscout.androidaps.plugins.general.automation.triggers.TriggerConnector
import info.nightscout.androidaps.plugins.general.automation.triggers.TriggerDummy
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.T
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

class AutomationEvent(private val mainApp: MainApp) {
    @Inject lateinit var aapsLogger: AAPSLogger

    var trigger: Trigger = TriggerConnector(mainApp)
    val actions: MutableList<Action> = ArrayList()
    var title: String? = null
    var isEnabled = true
    var lastRun: Long = 0

    init {
        mainApp.androidInjector().inject(this)
    }

    fun getPreconditions(): TriggerConnector {
        val trigger = TriggerConnector(mainApp, TriggerConnector.Type.AND)
        for (action in actions) {
            action.precondition?.let { trigger.list.add(it) }
        }
        return trigger
    }

    fun addAction(action: Action) = actions.add(action)

    fun toJSON(): String {
        val array = JSONArray()
        for (a in actions) array.put(a.toJSON())
        return JSONObject()
            .put("title", title)
            .put("enabled", isEnabled)
            .put("trigger", trigger.toJSON())
            .put("actions", array)
            .toString()
    }

    fun fromJSON(data: String?): AutomationEvent {
        val d = JSONObject(data)
        title = d.optString("title", "")
        isEnabled = d.optBoolean("enabled", true)
        trigger = TriggerDummy(mainApp).instantiate(JSONObject(d.getString("trigger")))
            ?: TriggerConnector(mainApp)
        val array = d.getJSONArray("actions")
        actions.clear()
        for (i in 0 until array.length()) {
            ActionDummy(mainApp).instantiate(JSONObject(array.getString(i)))?.let {
                actions.add(it)
            }
        }
        return this
    }

    fun shouldRun() : Boolean{
        return lastRun <= DateUtil.now() - T.mins(5).msecs()
    }
}