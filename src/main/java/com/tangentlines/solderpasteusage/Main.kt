package com.tangentlines.solderpasteusage

import de.tudresden.inf.lat.jsexp.Sexp
import de.tudresden.inf.lat.jsexp.SexpFactory
import de.tudresden.inf.lat.jsexp.SexpList
import de.tudresden.inf.lat.jsexp.SexpString
import org.apache.commons.cli.*
import org.apache.commons.cli.help.HelpFormatter
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

// https://www.indium.com/blog/calculating-solder-paste-usage.php#ixzz246lOB9HY
// All linear units are mm, areas mm^2, volumes mm^3.
// Densities are g/cm^3. Conversion: 1 cm^3 = 1000 mm^3.
//
// Mixture density with volume additivity:
//   1/ρ_mix = w/ρ_alloy + (1-w)/ρ_flux
//   ρ_mix   = (ρ_alloy * ρ_flux) / (w*ρ_flux + (1-w)*ρ_alloy)
//
// Default alloy density is set for Sn42/Bi58 (~8.7 g/cm^3).
object Main {

    @JvmStatic
    fun main(args: Array<String>) {

        val options = Options()
            .addOption(Option.builder("f")
                .desc("KiCad PCB file")
                .hasArg()
                .argName("FILE")
                .longOpt("file")
                .get())
            .addOption(Option.builder("s")
                .desc("Stencil thickness in mm (default 0.12)")
                .hasArg()
                .argName("THICKNESS_MM")
                .longOpt("stencil")
                .get())
            .addOption(Option.builder("m")
                .desc("Metal fraction as % or fraction (e.g. 88 or 0.88). Default 87.75%")
                .hasArg()
                .argName("METAL")
                .longOpt("metal")
                .get())
            .addOption(Option.builder("a")
                .desc("Alloy density g/cm^3 (default 8.74 for Sn42/Bi58)")
                .hasArg()
                .argName("G_PER_CM3")
                .longOpt("alloy-density")
                .get())
            .addOption(Option.builder("x")
                .desc("Flux density g/cm^3 (default 1.00)")
                .hasArg()
                .argName("G_PER_CM3")
                .longOpt("flux-density")
                .get())
            .addOption("h", "help", false, "Print help")

        try {
            val parser = DefaultParser()
            val cmd = parser.parse(options, args)

            if (cmd.hasOption("h")) {
                val header = "Calculates solder paste usage from a KiCad .kicad_pcb\n\n"
                HelpFormatter.builder().get().printHelp("solderpasteusage", header, options, "", true)
                return
            }

            if (!cmd.hasOption("f")) {
                System.err.println("KiCad PCB file required (-f/--file)")
                return
            }

            val file = File(cmd.getOptionValue("f"))
            if (!file.exists()) {
                System.err.println("KiCad PCB file not found - ${file.absolutePath}")
                return
            }

            val inputStream: InputStream = BufferedInputStream(FileInputStream(file))
            val data = SexpFactory.parse(inputStream)

            // --- Inputs / defaults ---
            val metalInput = if (cmd.hasOption("m")) cmd.getOptionValue("m").toDouble() else 87.75
            val amountOfMetal = normalizeMetalFraction(metalInput) // 0..1

            val stencilThickness = if (cmd.hasOption("s")) cmd.getOptionValue("s").toDouble() else 0.12

            val alloyDensity = if (cmd.hasOption("a")) cmd.getOptionValue("a").toDouble() else 8.74   // g/cm^3 (Sn42/Bi58)
            val fluxDensity  = if (cmd.hasOption("x")) cmd.getOptionValue("x").toDouble() else 1.00   // g/cm^3

            // Mixture density (g/cm^3)
            val pasteDensity = (alloyDensity * fluxDensity) /
                    (amountOfMetal * fluxDensity + (1.0 - amountOfMetal) * alloyDensity)

            // --- Geometry extraction ---
            val allPads = filterByType(data, "module").flatMap { filterByType(it, "pad") }
            val totalAreaFront = allPads.sumOf { getArea(it, "F.Paste") } // mm^2
            val totalAreaBack  = allPads.sumOf { getArea(it, "B.Paste") } // mm^2

            val totalVolumeFront = totalAreaFront * stencilThickness      // mm^3
            val totalVolumeBack  = totalAreaBack  * stencilThickness      // mm^3

            // Convert mm^3 -> cm^3 (÷1000), then * density → grams
            val solderPasteFront = (totalVolumeFront / 1000.0) * pasteDensity
            val solderPasteBack  = (totalVolumeBack  / 1000.0) * pasteDensity

            // --- Output ---
            println("Metal Fraction:          ${String.format("%.2f", amountOfMetal * 100)} %")
            println("Stencil Thickness:       ${String.format("%.3f", stencilThickness)} mm")
            println("Alloy Density:           ${String.format("%.3f", alloyDensity)} g/cm3")
            println("Flux Density:            ${String.format("%.3f", fluxDensity)} g/cm3")
            println("Paste Density (mix):     ${String.format("%.3f", pasteDensity)} g/cm3")
            println("------------------------------------")
            println("Total Number of Pads:    ${allPads.size}")
            println("Pad Area - Front:        ${String.format("%.2f", totalAreaFront)} mm2")
            println("Pad Area - Back:         ${String.format("%.2f", totalAreaBack)} mm2")
            println("------------------------------------")
            println("Solder Paste - Front:    ${String.format("%.2f", solderPasteFront)} g")
            println("Solder Paste - Back:     ${String.format("%.2f", solderPasteBack)} g")
            println("Solder Paste - Total:    ${String.format("%.2f", (solderPasteFront + solderPasteBack))} g")
            println("------------------------------------")

        } catch (e: Exception) {
            System.err.println("Unable to calculate solder paste amount")
            e.printStackTrace()
        }
    }

    // Accepts either percentage (e.g., 88) or fraction (0.88). Clamps to [0,1].
    private fun normalizeMetalFraction(input: Double): Double {
        val frac = if (input > 1.0) input / 100.0 else input
        return min(1.0, max(0.0, frac))
    }

    private fun getArea(input: Sexp, layer: String): Double {
        if (getString(input, 0) == "pad") {
            val type = getString(input, 2)
            val form = getString(input, 3)
            val size = getDoubleList(input, 5)
            val layers = getStringList(input, 6)

            if (type == "smd" && layers?.contains(layer) == true && size != null) {
                return when (form) {
                    "rect", "roundrect" -> size[0] * size[1]
                    "circle", "oval"    -> (size[0] / 2.0) * (size[1] / 2.0) * PI
                    else -> {
                        println("Ignored Pad (unsupported form): $input")
                        0.0
                    }
                }
            }
        }
        return 0.0
    }

    private fun getString(input: Sexp, index: Int): String? {
        if (input is SexpList && input.length > index && input[index] is SexpString) {
            return (input[index] as SexpString).toIndentedString()
        }
        return null
    }

    private fun getStringList(input: Sexp, index: Int): List<String>? {
        if (input is SexpList && input.length > index && input[index] is SexpList) {
            val children = (input[index] as SexpList).toList()
            return children.subList(1, children.size)
                .filter { it is SexpString }
                .map { (it as SexpString).toIndentedString() }
        }
        return null
    }

    private fun getDoubleList(input: Sexp, index: Int): List<Double>? =
        getStringList(input, index)?.map { it.toDouble() }

    private fun filterByType(input: Sexp, type: String): List<Sexp> =
        input.toList().filter {
            it is SexpList && it.length > 0 && (it[0] is SexpString) &&
                    (it[0] as SexpString).toIndentedString() == type
        }

    private fun Sexp.toList(): List<Sexp> {
        val list = mutableListOf<Sexp>()
        for (i in 0 until this.length) list.add(this.get(i))
        return list
    }

}