package com.tdminsight.app.engine

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Vancomycin TDM calculation engine.
 * Pure Kotlin, no Android/UI dependencies — ported from the reference
 * runTdmCalc.js backend function.
 *
 * Reference equations (VERIFY against lecturer-approved sources):
 *   Cockcroft-Gault CrCl; Matzke population Ke = 0.00083*CrCl + 0.0044;
 *   population Vd = 0.7 L/kg; Sawchuk-Johnson two-point individualization;
 *   one-compartment intermittent IV infusion steady-state model.
 */

enum class Workflow(val id: String, val label: String, val short: String, val description: String) {
    PRE(
        "pre", "Vancomycin – Pre-dose (trough only)", "Pre",
        "Trough-guided workflow using a single pre-dose (trough) concentration."
    ),
    POST(
        "post", "Vancomycin – Post-dose (peak only)", "Post",
        "Peak-guided workflow using a single post-dose (peak) concentration and its sampling time."
    ),
    PRE_POST(
        "prepost", "Vancomycin – Pre + Post (individualized)", "Pre + Post",
        "Two-point Sawchuk–Johnson workflow using both trough and peak concentrations."
    );

    companion object {
        fun fromId(id: String) = entries.first { it.id == id }
    }
}

data class TdmTargets(
    val troughMin: Double = 15.0,
    val troughMax: Double = 20.0,
    val aucMin: Double = 400.0,
    val aucMax: Double = 600.0,
)

val TARGETS = TdmTargets()

/** Raw form inputs, as strings, straight from the UI text fields. */
data class TdmFormInputs(
    val age: String = "",
    val weight: String = "",
    val sex: String? = null, // "male" | "female"
    val scr: String = "",
    val dose: String = "",
    val tau: String = "",
    val tInf: String = "",
    val cminMeasured: String = "",
    val cpeakMeasured: String = "",
    val tPeak: String = "",
)

/** Parsed, numeric inputs used by the engine. */
data class TdmInputs(
    val age: Double,
    val weight: Double,
    val sex: String,
    val scr: Double,
    val dose: Double,
    val tau: Double,
    val tInf: Double,
    val cminMeasured: Double? = null,
    val cpeakMeasured: Double? = null,
    val tPeak: Double? = null,
)

data class ValidationResult(val errors: List<String>, val warnings: List<String> = emptyList()) {
    val isValid get() = errors.isEmpty()
}

data class ParamValue(val value: Double, val unit: String, val label: String)

data class ExplanationStep(val step: String, val label: String, val detail: String)

data class Recommendation(val targetDose: Int, val target: String, val note: String)

data class TdmResult(
    val workflow: Workflow,
    val inputs: TdmInputs,
    val intermediate: LinkedHashMap<String, ParamValue>,
    val pk: LinkedHashMap<String, ParamValue>,
    val recommendation: Recommendation,
    val explanation: List<ExplanationStep>,
    val warnings: List<String> = emptyList(),
)

private fun Double?.orZero() = this ?: 0.0

private fun toDoubleOrNull(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()

object TdmValidator {

    /** Parses raw string form fields into a [TdmInputs], returning parse errors if any field is invalid/missing. */
    fun parse(workflow: Workflow, form: TdmFormInputs): Pair<TdmInputs?, List<String>> {
        val errors = mutableListOf<String>()

        val age = toDoubleOrNull(form.age)
        val weight = toDoubleOrNull(form.weight)
        val scr = toDoubleOrNull(form.scr)
        val dose = toDoubleOrNull(form.dose)
        val tau = toDoubleOrNull(form.tau)
        val tInf = toDoubleOrNull(form.tInf)

        if (age == null) errors += "Age is required."
        if (weight == null) errors += "Body weight is required."
        if (scr == null) errors += "Serum creatinine is required."
        if (dose == null) errors += "Dose is required."
        if (tau == null) errors += "Dosing interval is required."
        if (tInf == null) errors += "Infusion duration is required."
        if (form.sex != "male" && form.sex != "female") errors += "Sex is required."

        if (tInf != null && tau != null && tInf >= tau) {
            errors += "Infusion duration must be shorter than the dosing interval."
        }

        var cminMeasured: Double? = null
        var cpeakMeasured: Double? = null
        var tPeak: Double? = null

        when (workflow) {
            Workflow.PRE -> {
                cminMeasured = toDoubleOrNull(form.cminMeasured)
                if (cminMeasured == null || cminMeasured <= 0) errors += "Measured trough is required."
            }
            Workflow.POST -> {
                cpeakMeasured = toDoubleOrNull(form.cpeakMeasured)
                tPeak = toDoubleOrNull(form.tPeak)
                if (cpeakMeasured == null || cpeakMeasured <= 0) errors += "Measured peak is required."
                if (tPeak == null || tPeak < 0) errors += "Time to peak sample is required."
                if (tPeak != null && tau != null && tInf != null && tPeak >= tau - tInf) {
                    errors += "Peak sample time must be before the next dose."
                }
            }
            Workflow.PRE_POST -> {
                cminMeasured = toDoubleOrNull(form.cminMeasured)
                cpeakMeasured = toDoubleOrNull(form.cpeakMeasured)
                tPeak = toDoubleOrNull(form.tPeak)
                if (cminMeasured == null || cminMeasured <= 0) errors += "Measured trough is required."
                if (cpeakMeasured == null || cpeakMeasured <= 0) errors += "Measured peak is required."
                if (tPeak == null || tPeak < 0) errors += "Time to peak sample is required."
                if (cpeakMeasured != null && cminMeasured != null && cpeakMeasured <= cminMeasured) {
                    errors += "Peak must be greater than trough (Ke would be <= 0)."
                }
                if (tau != null && tInf != null && tPeak != null) {
                    val dt = tau - tInf - tPeak
                    if (dt <= 0) errors += "Timing error: tau - tInf - tPeak must be positive."
                }
            }
        }

        if (errors.isNotEmpty() || age == null || weight == null || scr == null || dose == null || tau == null || tInf == null || form.sex == null) {
            return null to errors
        }

        return TdmInputs(
            age = age, weight = weight, sex = form.sex, scr = scr,
            dose = dose, tau = tau, tInf = tInf,
            cminMeasured = cminMeasured, cpeakMeasured = cpeakMeasured, tPeak = tPeak
        ) to emptyList()
    }
}

object TdmEngine {

    private fun crCl(i: TdmInputs): Double {
        val base = ((140 - i.age) * i.weight) / (72 * i.scr)
        return if (i.sex == "female") base * 0.85 else base
    }

    private fun fmt(v: Double, digits: Int = 2) = "%.${digits}f".format(v)

    fun run(workflow: Workflow, i: TdmInputs): TdmResult = when (workflow) {
        Workflow.PRE -> runPre(i)
        Workflow.POST -> runPost(i)
        Workflow.PRE_POST -> runPrePost(i)
    }

    private fun runPre(i: TdmInputs): TdmResult {
        val cminMeasured = i.cminMeasured.orZero()
        val crClVal = crCl(i)
        val ke = 0.00083 * crClVal + 0.0044
        val tHalf = 0.693 / ke
        val r = i.dose / i.tInf
        val eKtau = 1 - exp(-ke * i.tau)
        val eKtInf = 1 - exp(-ke * i.tInf)
        val vd = (r * eKtInf * exp(-ke * (i.tau - i.tInf))) / (ke * eKtau * cminMeasured)
        val cl = ke * vd
        val cmax = cminMeasured * exp(ke * (i.tau - i.tInf))
        val aucTau = i.dose / cl
        val auc24 = aucTau * (24 / i.tau)
        val targetDose = (((i.dose * TARGETS.troughMin) / cminMeasured) / 250.0).roundToInt() * 250

        val intermediate = linkedMapOf(
            "crCl" to ParamValue(crClVal, "mL/min", "Creatinine clearance (Cockcroft-Gault)"),
            "ke" to ParamValue(ke, "h^-1", "Ke (population)"),
            "halfLife" to ParamValue(tHalf, "h", "Half-life"),
            "vd" to ParamValue(vd, "L", "Vd (individualized)"),
            "cl" to ParamValue(cl, "L/h", "Clearance"),
        )
        val pk = linkedMapOf(
            "cmax" to ParamValue(cmax, "mg/L", "Estimated peak"),
            "cmin" to ParamValue(cminMeasured, "mg/L", "Measured trough"),
            "auc24" to ParamValue(auc24, "mg*h/L", "AUC24"),
        )
        val recommendation = Recommendation(
            targetDose, "${TARGETS.troughMin.toInt()}-${TARGETS.troughMax.toInt()} mg/L",
            "To reach target trough ${TARGETS.troughMin.toInt()} mg/L at current interval, a dose of ~$targetDose mg may be considered (verify clinically)."
        )
        val explanation = listOf(
            ExplanationStep("1", "Creatinine clearance", "CrCl = ((140 - ${i.age}) x ${i.weight}) / (72 x ${i.scr})${if (i.sex == "female") " x 0.85" else ""} = ${fmt(crClVal, 1)} mL/min"),
            ExplanationStep("2", "Population Ke", "Ke = 0.00083 x ${fmt(crClVal, 1)} + 0.0044 = ${fmt(ke, 5)} h^-1"),
            ExplanationStep("3", "Half-life", "t1/2 = 0.693 / ${fmt(ke, 5)} = ${fmt(tHalf)} h"),
            ExplanationStep("4", "Individualized Vd", "Vd = (R(1-e^(-Ke t'))e^(-Ke(t-t'))) / (Ke(1-e^(-Ke t))Cmin) = ${fmt(vd)} L"),
            ExplanationStep("5", "Estimated peak", "Cmax = Cmin x e^(Ke(t-t')) = ${fmt(cmax)} mg/L"),
            ExplanationStep("6", "AUC24", "AUC24 = (Dose/CL) x (24/t) = ${fmt(auc24, 1)} mg*h/L"),
            ExplanationStep("7", "Dose recommendation", "Suggested dose ~$targetDose mg to reach trough ${TARGETS.troughMin.toInt()} mg/L"),
        )
        return TdmResult(Workflow.PRE, i, intermediate, pk, recommendation, explanation)
    }

    private fun runPost(i: TdmInputs): TdmResult {
        val cpeakMeasured = i.cpeakMeasured.orZero()
        val tPeak = i.tPeak.orZero()
        val crClVal = crCl(i)
        val ke = 0.00083 * crClVal + 0.0044
        val tHalf = 0.693 / ke
        val r = i.dose / i.tInf
        val eKtau = 1 - exp(-ke * i.tau)
        val eKtInf = 1 - exp(-ke * i.tInf)
        val cmaxTrue = cpeakMeasured * exp(ke * tPeak)
        val vd = (r * eKtInf) / (ke * eKtau * cmaxTrue)
        val cl = ke * vd
        val cmin = cmaxTrue * exp(-ke * (i.tau - i.tInf))
        val auc24 = (i.dose / cl) * (24 / i.tau)
        val targetDose = (((i.dose * TARGETS.troughMin) / cmin) / 250.0).roundToInt() * 250

        val intermediate = linkedMapOf(
            "crCl" to ParamValue(crClVal, "mL/min", "Creatinine clearance"),
            "ke" to ParamValue(ke, "h^-1", "Ke (population)"),
            "halfLife" to ParamValue(tHalf, "h", "Half-life"),
            "vd" to ParamValue(vd, "L", "Vd (individualized)"),
            "cl" to ParamValue(cl, "L/h", "Clearance"),
            "cmaxTrue" to ParamValue(cmaxTrue, "mg/L", "True peak (end of infusion)"),
        )
        val pk = linkedMapOf(
            "cmax" to ParamValue(cpeakMeasured, "mg/L", "Measured peak"),
            "cmin" to ParamValue(cmin, "mg/L", "Projected trough"),
            "auc24" to ParamValue(auc24, "mg*h/L", "AUC24"),
        )
        val recommendation = Recommendation(
            targetDose, "${TARGETS.troughMin.toInt()}-${TARGETS.troughMax.toInt()} mg/L",
            "Projected trough ${fmt(cmin, 1)} mg/L. To reach ${TARGETS.troughMin.toInt()} mg/L, dose ~$targetDose mg may be considered (verify clinically)."
        )
        val explanation = listOf(
            ExplanationStep("1", "Creatinine clearance", "CrCl = ${fmt(crClVal, 1)} mL/min"),
            ExplanationStep("2", "Population Ke", "Ke = ${fmt(ke, 5)} h^-1"),
            ExplanationStep("3", "Half-life", "t1/2 = ${fmt(tHalf)} h"),
            ExplanationStep("4", "True peak", "Cmax = $cpeakMeasured x e^(Ke x $tPeak) = ${fmt(cmaxTrue)} mg/L"),
            ExplanationStep("5", "Individualized Vd", "Vd = ${fmt(vd)} L"),
            ExplanationStep("6", "Projected trough", "Cmin = ${fmt(cmin)} mg/L"),
            ExplanationStep("7", "AUC24", "AUC24 = ${fmt(auc24, 1)} mg*h/L"),
            ExplanationStep("8", "Dose recommendation", "Suggested dose ~$targetDose mg"),
        )
        return TdmResult(Workflow.POST, i, intermediate, pk, recommendation, explanation)
    }

    private fun runPrePost(i: TdmInputs): TdmResult {
        val cminMeasured = i.cminMeasured.orZero()
        val cpeakMeasured = i.cpeakMeasured.orZero()
        val tPeak = i.tPeak.orZero()
        val crClVal = crCl(i)
        val r = i.dose / i.tInf
        val dt = i.tau - i.tInf - tPeak
        val ke = ln(cpeakMeasured / cminMeasured) / dt
        val tHalf = 0.693 / ke
        val cmaxTrue = cpeakMeasured * exp(ke * tPeak)
        val eKtau = 1 - exp(-ke * i.tau)
        val eKtInf = 1 - exp(-ke * i.tInf)
        val vd = (r * eKtInf) / (ke * eKtau * cmaxTrue)
        val cl = ke * vd
        val auc24 = (i.dose / cl) * (24 / i.tau)
        val targetDose = (((i.dose * TARGETS.troughMin) / cminMeasured) / 250.0).roundToInt() * 250

        val intermediate = linkedMapOf(
            "crCl" to ParamValue(crClVal, "mL/min", "Creatinine clearance (reference)"),
            "ke" to ParamValue(ke, "h^-1", "Ke (individualized)"),
            "halfLife" to ParamValue(tHalf, "h", "Half-life"),
            "vd" to ParamValue(vd, "L", "Vd (individualized)"),
            "cl" to ParamValue(cl, "L/h", "Clearance"),
            "cmaxTrue" to ParamValue(cmaxTrue, "mg/L", "True peak (end of infusion)"),
        )
        val pk = linkedMapOf(
            "cmax" to ParamValue(cpeakMeasured, "mg/L", "Measured peak"),
            "cmin" to ParamValue(cminMeasured, "mg/L", "Measured trough"),
            "auc24" to ParamValue(auc24, "mg*h/L", "AUC24"),
        )
        val recommendation = Recommendation(
            targetDose, "${TARGETS.troughMin.toInt()}-${TARGETS.troughMax.toInt()} mg/L",
            "Individualized Ke = ${fmt(ke, 4)} h^-1. To reach target trough ${TARGETS.troughMin.toInt()} mg/L, dose ~$targetDose mg may be considered (verify clinically)."
        )
        val explanation = listOf(
            ExplanationStep("1", "Time between peak and trough", "dt = t - t' - t_peak = ${fmt(dt)} h"),
            ExplanationStep("2", "Individualized Ke", "Ke = ln(Cpeak/Ctrough)/dt = ln($cpeakMeasured/$cminMeasured)/${fmt(dt)} = ${fmt(ke, 5)} h^-1"),
            ExplanationStep("3", "Half-life", "t1/2 = 0.693 / ${fmt(ke, 5)} = ${fmt(tHalf)} h"),
            ExplanationStep("4", "True peak", "Cmax = $cpeakMeasured x e^(Ke x $tPeak) = ${fmt(cmaxTrue)} mg/L"),
            ExplanationStep("5", "Individualized Vd", "Vd = ${fmt(vd)} L"),
            ExplanationStep("6", "Clearance", "CL = Ke x Vd = ${fmt(cl)} L/h"),
            ExplanationStep("7", "AUC24", "AUC24 = ${fmt(auc24, 1)} mg*h/L"),
            ExplanationStep("8", "Dose recommendation", "Suggested dose ~$targetDose mg"),
        )
        return TdmResult(Workflow.PRE_POST, i, intermediate, pk, recommendation, explanation)
    }
}
