package com.mogas.timemanager

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private val BG = Color.parseColor("#0F1115")
    private val CARD = Color.parseColor("#1A1D24")
    private val LINE = Color.parseColor("#262B36")
    private val ACC = Color.parseColor("#4FC3F7")
    private val ACC2 = Color.parseColor("#8E7CFF")
    private val TXT = Color.WHITE
    private val DIM = Color.parseColor("#8A92A0")
    private val MP = ViewGroup.LayoutParams.MATCH_PARENT
    private val WC = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var list: LinearLayout
    private lateinit var heroBig: TextView
    private lateinit var heroSmall: TextView
    private lateinit var dot: View
    private lateinit var fab: TextView
    private var pulse: ObjectAnimator? = null
    private var dlg: AlertDialog? = null
    private var askedNotif = false
    private var skipped = false
    private var firstShow = true
    private val tick = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateHero()
            tick.postDelayed(this, 20000L)
        }
    }

    private fun shape(fill: Int, r: Int, stroke: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(r).toFloat()
        if (stroke != 0) g.setStroke(dp(1), stroke)
        return g
    }

    private fun cardBg(on: Boolean) = shape(CARD, 22, if (on) Color.argb(110, 79, 195, 247) else LINE)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Schedule.seedOnce(this)
        window.statusBarColor = Color.parseColor("#12151C")
        window.navigationBarColor = Color.parseColor("#0B0D11")

        val frame = FrameLayout(this)
        frame.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#12151C"), Color.parseColor("#0B0D11"))
        )
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        frame.addView(col, FrameLayout.LayoutParams(MP, MP))

        // Cabeçalho
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(dp(22), dp(24), dp(18), dp(12))
        val title = TextView(this)
        title.text = "Alarmes"
        title.textSize = 30f
        title.setTextColor(TXT)
        title.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        head.addView(title, LinearLayout.LayoutParams(0, WC, 1f))
        val test = TextView(this)
        test.text = "Testar"
        test.textSize = 13f
        test.setTextColor(DIM)
        test.setPadding(dp(14), dp(8), dp(14), dp(8))
        test.background = shape(Color.TRANSPARENT, 20, LINE)
        test.pressable()
        test.setOnClickListener {
            Scheduler.at(this, 0, System.currentTimeMillis() + 10000L, false)
            Toast.makeText(this, "Fecha o ecrã e espera 10 segundos", Toast.LENGTH_LONG).show()
        }
        val sched = TextView(this)
        sched.text = "Horário"
        sched.textSize = 13f
        sched.setTextColor(ACC)
        sched.setPadding(dp(14), dp(8), dp(14), dp(8))
        sched.background = shape(Color.TRANSPARENT, 20, Color.argb(110, 79, 195, 247))
        sched.pressable()
        sched.setOnClickListener { confirmSchedule() }
        head.addView(sched, LinearLayout.LayoutParams(WC, WC).also { it.rightMargin = dp(8) })
        head.addView(test)
        col.addView(head)

        // Cartão principal: próximo alarme
        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.VERTICAL
        hero.setPadding(dp(22), dp(18), dp(22), dp(20))
        hero.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(ACC, ACC2)).also {
            it.cornerRadius = dp(26).toFloat()
        }
        val heroTop = LinearLayout(this)
        heroTop.orientation = LinearLayout.HORIZONTAL
        heroTop.gravity = Gravity.CENTER_VERTICAL
        dot = View(this)
        dot.background = GradientDrawable().also { it.shape = GradientDrawable.OVAL; it.setColor(BG) }
        heroTop.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)).also { it.rightMargin = dp(8) })
        val lbl = TextView(this)
        lbl.text = "PRÓXIMO ALARME"
        lbl.textSize = 12f
        lbl.letterSpacing = 0.12f
        lbl.setTextColor(Color.argb(190, 15, 17, 21))
        heroTop.addView(lbl)
        hero.addView(heroTop)
        heroBig = TextView(this)
        heroBig.textSize = 34f
        heroBig.setTextColor(BG)
        heroBig.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        heroBig.setPadding(0, dp(6), 0, 0)
        hero.addView(heroBig)
        heroSmall = TextView(this)
        heroSmall.textSize = 15f
        heroSmall.setTextColor(Color.argb(200, 15, 17, 21))
        hero.addView(heroSmall)
        val hlp = LinearLayout.LayoutParams(MP, WC)
        hlp.setMargins(dp(18), dp(4), dp(18), dp(10))
        col.addView(hero, hlp)
        hero.enter(0)

        pulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.25f).also {
            it.duration = 900
            it.repeatMode = ValueAnimator.REVERSE
            it.repeatCount = ValueAnimator.INFINITE
            it.start()
        }

        // Lista
        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.clipToPadding = false
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(18), dp(4), dp(18), dp(110))
        scroll.addView(list)
        col.addView(scroll, LinearLayout.LayoutParams(MP, 0, 1f))

        // Botão flutuante
        fab = TextView(this)
        fab.text = "+"
        fab.textSize = 34f
        fab.gravity = Gravity.CENTER
        fab.setTextColor(BG)
        fab.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(ACC, ACC2)).also {
            it.shape = GradientDrawable.OVAL
        }
        fab.elevation = dp(10).toFloat()
        fab.pressable()
        fab.setOnClickListener {
            fab.animate().rotation(90f).setDuration(180).start()
            edit(null)
        }
        val flp = FrameLayout.LayoutParams(dp(66), dp(66), Gravity.BOTTOM or Gravity.END)
        flp.setMargins(0, 0, dp(24), dp(28))
        frame.addView(fab, flp)
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.animate().scaleX(1f).scaleY(1f).setStartDelay(350).setDuration(450)
            .setInterpolator(OvershootInterpolator(2.2f)).start()

        setContentView(frame)
    }

    override fun onResume() {
        super.onResume()
        Scheduler.rescheduleAll(this)
        refresh(firstShow)
        firstShow = false
        tick.removeCallbacks(ticker)
        tick.postDelayed(ticker, 20000L)
        checkPerms()
    }

    override fun onPause() {
        tick.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }

    private fun updateHero() {
        val n = Scheduler.nextAlarm(this)
        if (n == null) {
            heroBig.text = "Sem alarmes ativos"
            heroBig.textSize = 26f
            heroSmall.text = "Toca em + para criar um"
            dot.visibility = View.INVISIBLE
        } else {
            heroBig.text = "Daqui a " + Scheduler.fmtIn(n.second - System.currentTimeMillis())
            heroBig.textSize = 30f
            val a = n.first
            heroSmall.text = a.timeText() + (if (a.label.isNotEmpty()) " · " + a.label else "")
            dot.visibility = View.VISIBLE
        }
    }

    private fun refresh(animate: Boolean = false) {
        val l = Store.all(this).sortedWith(compareBy({ it.hour }, { it.minute }))
        list.removeAllViews()
        if (l.isEmpty()) {
            val e = TextView(this)
            e.text = "⏰\n\nSem alarmes.\nToca em + para criar o primeiro."
            e.setTextColor(DIM)
            e.textSize = 16f
            e.gravity = Gravity.CENTER
            e.setPadding(0, dp(70), 0, 0)
            list.addView(e)
            if (animate) e.enter(200)
        }
        for ((i, a) in l.withIndex()) {
            val r = row(a)
            list.addView(r)
            if (animate) r.enter(120L + minOf(i, 8) * 70L)
        }
        updateHero()
    }

    private fun row(a: Alarm): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = cardBg(a.enabled)
        card.setPadding(dp(20), dp(16), dp(16), dp(16))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.alpha = if (a.enabled) 1f else 0.5f
        val t = TextView(this)
        t.text = a.timeText()
        t.textSize = 40f
        t.setTextColor(TXT)
        t.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val sub = TextView(this)
        sub.text = (if (a.label.isNotEmpty()) a.label + " · " else "") + a.daysText()
        sub.textSize = 14f
        sub.setTextColor(DIM)
        col.addView(t)
        col.addView(sub)

        val sw = Switch(this)
        sw.isChecked = a.enabled
        sw.thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(ACC, DIM)
        )
        sw.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.argb(120, 79, 195, 247), LINE)
        )
        sw.setOnCheckedChangeListener { _, on ->
            a.enabled = on
            Store.upsert(this, a)
            Scheduler.schedule(this, a)
            card.background = cardBg(on)
            col.animate().alpha(if (on) 1f else 0.5f).setDuration(220).start()
            updateHero()
            if (on) toastNext(a)
        }

        card.addView(col, LinearLayout.LayoutParams(0, WC, 1f))
        card.addView(sw)
        card.pressable()
        card.setOnClickListener { edit(a) }
        val lp = LinearLayout.LayoutParams(MP, WC)
        lp.setMargins(0, dp(6), 0, dp(6))
        card.layoutParams = lp
        return card
    }

    private fun confirmSchedule() {
        val d = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Repor horário LEE1TA")
            .setMessage("Apaga os alarmes atuais e cria os ${Schedule.count()} alarmes do horário de aulas e estudo.")
            .setPositiveButton("Repor") { _, _ ->
                Schedule.install(this, true)
                refresh(true)
            }
            .setNegativeButton("Cancelar", null)
            .show()
        d.window?.setBackgroundDrawable(shape(CARD, 26))
    }

    private fun toastNext(a: Alarm) {
        val ms = Scheduler.next(a) - System.currentTimeMillis()
        Toast.makeText(this, "Toca daqui a " + Scheduler.fmtIn(ms), Toast.LENGTH_SHORT).show()
    }

    private fun edit(old: Alarm?) {
        val v = layoutInflater.inflate(R.layout.dialog_alarm, null)
        val tp = v.findViewById<TimePicker>(R.id.tp)
        val et = v.findViewById<EditText>(R.id.label)
        val daysRow = v.findViewById<LinearLayout>(R.id.days)
        val sp = v.findViewById<Spinner>(R.id.snooze)

        tp.setIs24HourView(true)
        val now = Calendar.getInstance()
        tp.hour = old?.hour ?: now.get(Calendar.HOUR_OF_DAY)
        tp.minute = old?.minute ?: now.get(Calendar.MINUTE)
        et.setText(old?.label ?: "")

        var mask = old?.days ?: 0
        val letters = listOf("S", "T", "Q", "Q", "S", "S", "D")
        for (i in 0..6) {
            val tv = TextView(this)
            tv.text = letters[i]
            tv.gravity = Gravity.CENTER
            tv.textSize = 15f
            tv.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            fun paint() {
                val on = (mask and (1 shl i)) != 0
                tv.background = GradientDrawable().also {
                    it.shape = GradientDrawable.OVAL
                    it.setColor(if (on) ACC else LINE)
                }
                tv.setTextColor(if (on) BG else TXT)
            }
            paint()
            tv.setOnClickListener {
                mask = mask xor (1 shl i)
                paint()
                tv.animate().scaleX(0.75f).scaleY(0.75f).setDuration(70).withEndAction {
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(220)
                        .setInterpolator(OvershootInterpolator(3f)).start()
                }.start()
            }
            val lp = LinearLayout.LayoutParams(0, dp(40), 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            daysRow.addView(tv, lp)
        }

        val snoozes = intArrayOf(5, 10, 15)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("5 min", "10 min", "15 min"))
        val si = snoozes.indexOf(old?.snooze ?: 10)
        sp.setSelection(if (si >= 0) si else 1)

        val b = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(if (old == null) "Novo alarme" else "Editar alarme")
            .setView(v)
            .setPositiveButton("Guardar") { _, _ ->
                tp.clearFocus()
                val a = old ?: Alarm(Store.newId(this), 0, 0)
                a.hour = tp.hour
                a.minute = tp.minute
                a.label = et.text.toString().trim()
                a.days = mask
                a.snooze = snoozes[sp.selectedItemPosition]
                a.enabled = true
                Store.upsert(this, a)
                Scheduler.schedule(this, a)
                toastNext(a)
                refresh(true)
            }
            .setNegativeButton("Cancelar", null)
        if (old != null) {
            b.setNeutralButton("Apagar") { _, _ ->
                Scheduler.cancel(this, old.id)
                Store.remove(this, old.id)
                refresh(true)
            }
        }
        val d = b.show()
        d.window?.setBackgroundDrawable(shape(CARD, 26))
        d.setOnDismissListener { fab.animate().rotation(0f).setDuration(180).start() }
    }

    // ---------- Permissões ----------
    private fun ask(msg: String, intent: Intent) {
        dlg = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Permissão necessária")
            .setMessage(msg)
            .setPositiveButton("Abrir definições") { _, _ ->
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
            }
            .setNegativeButton("Agora não") { _, _ -> skipped = true }
            .show()
        dlg?.window?.setBackgroundDrawable(shape(CARD, 26))
    }

    @SuppressLint("BatteryLife")
    private fun checkPerms() {
        if (dlg?.isShowing == true) return
        val pkg = Uri.parse("package:$packageName")

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!askedNotif) {
                askedNotif = true
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                return
            }
            if (!skipped) {
                ask(
                    "Ativa as notificações para o alarme aparecer no ecrã.",
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
            return
        }
        if (skipped) return

        if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            ask("Permite “Alarmes e lembretes” para os alarmes tocarem à hora certa.", Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            return
        }
        if (Build.VERSION.SDK_INT >= 34 && !getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
            ask("Permite notificações em ecrã inteiro para o alarme abrir com o telemóvel bloqueado.", Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
            return
        }
        val prefs = getSharedPreferences("mtm", MODE_PRIVATE)
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName) && !prefs.getBoolean("batAsked", false)) {
            prefs.edit().putBoolean("batAsked", true).apply()
            ask("Desativa a otimização de bateria para esta app, para o alarme nunca ser bloqueado.", Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
        }
    }
}
