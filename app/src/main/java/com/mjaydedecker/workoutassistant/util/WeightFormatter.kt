package com.mjaydedecker.workoutassistant.util

import com.mjaydedecker.workoutassistant.data.model.WeightUnit

object WeightFormatter {

    fun toDisplay(weightKg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> weightKg
        WeightUnit.LB -> weightKg * 2.20462
    }

    fun toKg(displayValue: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> displayValue
        WeightUnit.LB -> displayValue / 2.20462
    }

    fun format(weightKg: Double, unit: WeightUnit): String =
        "%.1f".format(toDisplay(weightKg, unit))

    fun label(unit: WeightUnit): String = when (unit) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }
}
