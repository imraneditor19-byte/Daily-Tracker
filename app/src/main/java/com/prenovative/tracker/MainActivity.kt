package com.prenovative.tracker

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var head: TextView
    private var h = 9
    private var m = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun hm(h: Int, m: Int) =
        String.format("%d:%02d %s", if (h % 12 == 0) 12 else h % 12, m, if (h >= 12) "PM" else "AM")

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(16))
        }
        head = TextView(this).apply { textSize = 22f; setPadding(0, 0, 0, dp(12)) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sv = ScrollView(this).apply { addView(list) }

        val name = EditText(this).apply { hint = "New daily task"; setSingleLine() }
        val timeBtn = Button(this).apply {
            text = hm(h, m)
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, a, b -> h = a; m = b; text = hm(a, b) }, h, m, false).show()
            }
        }
        val addBtn = Button(this).apply {
            text = "Add"
            setOnClickListener {
                val title = name.text.toString().trim()
                if (title.isNotEmpty()) {
                    val all = Store.load(context)
                    val task = Task((System.currentTimeMillis() % 1000000000L).toInt(), title, h, m, "")
                    all.add(task)
                    Store.save(context, all)
                    Reminder.schedule(context, task)
                    name.setText("")
                    render()
                }
            }
        }
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(name, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(timeBtn)
            addView(addBtn)
        }
        root.addView(head)
        root.addView(sv, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(bar)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val tasks = Store.load(this).sortedBy { it.h * 60 + it.m }
        val td = Store.today()
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        head.text = "Today: ${tasks.count { it.doneOn == td }} of ${tasks.size} done"
        list.removeAllViews()
        for (t in tasks) {
            val done = t.doneOn == td
            val late = !done && t.h * 60 + t.m <= now
            val cb = CheckBox(this).apply {
                isChecked = done
                setOnCheckedChangeListener { _, on ->
                    val all = Store.load(context)
                    val cur = all.first { it.id == t.id }
                    cur.doneOn = if (on) td else ""
                    Store.save(context, all)
                    Reminder.schedule(context, cur)
                    render()
                }
            }
            val tv = TextView(this).apply {
                textSize = 17f
                text = "${t.title}\n${hm(t.h, t.m)}${if (late) "  ·  OVERDUE" else ""}"
                if (late) setTextColor(0xFFC2410C.toInt())
            }
            val del = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    Store.save(context, Store.load(context).filter { it.id != t.id })
                    Reminder.cancel(context, t.id)
                    render()
                }
            }
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(cb)
                addView(tv, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                addView(del)
            }
            list.addView(row)
        }
    }
}
