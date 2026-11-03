package com.mogas.timemanager

import android.content.Context

/** Horário LEE1TA (Turno Tarde) · 1º Ano / 1º Semestre · 2026-27 */
object Schedule {
    // Dias: Seg=1 Ter=2 Qua=4 Qui=8 Sex=16 Sáb=32 Dom=64
    private class S(val h: Int, val m: Int, val label: String, val days: Int, val snooze: Int)

    private val items = listOf(
        // 06:00–07:30 Estudo
        S(6, 0, "Estudo · Análise Matemática I", 1 or 2, 10),
        S(6, 0, "Estudo · Intro. à Computação + Metodologia", 4, 10),
        S(6, 0, "Estudo · Intro. Electrónica e Telecom. + Inglês", 8 or 16, 10),
        // 13:00–14:30 Aulas
        S(13, 0, "Aula · Análise Matemática I", 1, 5),
        S(13, 0, "Aula · Introdução à Computação", 2 or 4, 5),
        S(13, 0, "Aula · Intro. à Electrónica e Telecom.", 8, 5),
        S(13, 0, "Aula · Física I", 16, 5),
        // 14:40–16:10 Aulas
        S(14, 40, "Aula · Análise Matemática I", 1, 5),
        S(14, 40, "Aula · Metodologia de Inv. Científica", 2 or 4, 5),
        S(14, 40, "Aula · Língua Portuguesa", 8, 5),
        S(14, 40, "Aula · Física I", 16, 5),
        // 16:20–17:40 Aulas
        S(16, 20, "Aula · Inglês Instrumental", 1 or 2, 5),
        S(16, 20, "Aula · Intro. à Electrónica e Telecom.", 4, 5),
        S(16, 20, "Aula · Língua Portuguesa", 8 or 16, 5),
        // 19:00–19:45
        S(19, 0, "Leitura da matéria", 31, 10),
        // 21:00–22:00
        S(21, 0, "Preparação do dia seguinte", 15, 10),
        S(21, 0, "Resumo da semana", 16, 10),
        // Fim de semana
        S(15, 0, "Estudo · Física I + Análise Matemática I", 32, 10),
        S(15, 0, "Estudo · Língua Portuguesa + dúvidas da semana", 64, 10),
        S(21, 0, "Preparação da semana", 32 or 64, 10)
    )

    fun install(c: Context, replace: Boolean) {
        if (replace) {
            for (a in Store.all(c)) {
                Scheduler.cancel(c, a.id)
                Store.remove(c, a.id)
            }
        }
        for (s in items) {
            val a = Alarm(Store.newId(c), s.h, s.m, s.label, s.days, true, s.snooze)
            Store.upsert(c, a)
            Scheduler.schedule(c, a)
        }
        c.getSharedPreferences("mtm", Context.MODE_PRIVATE).edit().putBoolean("seeded", true).apply()
    }

    fun seedOnce(c: Context) {
        if (!c.getSharedPreferences("mtm", Context.MODE_PRIVATE).getBoolean("seeded", false)) install(c, false)
    }

    fun count() = items.size
}
