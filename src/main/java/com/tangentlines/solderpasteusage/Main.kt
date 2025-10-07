package com.tangentlines.solderpasteusage

import de.tudresden.inf.lat.jsexp.Sexp
import de.tudresden.inf.lat.jsexp.SexpFactory
import de.tudresden.inf.lat.jsexp.SexpList
import de.tudresden.inf.lat.jsexp.SexpString
import org.apache.commons.cli.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


// all units in mm
// https://www.indium.com/blog/calculating-solder-paste-usage.php#ixzz246lOB9HY
// (Alloy Specific Gravity + Flux Specific Gravity) / (Flux % * Alloy Specific Gravity) + (Metal % * Flux Specific Gravity)

private const val ALLOY_SPECIFIC_GRAVITY = 7.4      // g/cm3
private const val FLUX_SPECIFIC_GRAVITY = 1.0       // g/cm3

object Main {

    @JvmStatic
    fun main(args: Array<String>) {

        val options = Options()

        options.addOption(Option.builder("f")
                .desc("Kicad PCB File")
                .hasArg()
                .argName("FILE")
                .longOpt("file")
                .build())

        options.addOption(Option.builder("s")
                .desc("Stencil thickness (mm)")
                .hasArg()
                .argName("THICKNESS")
                .longOpt("stencil")
                .build())

        options.addOption(Option.builder("m")
                .desc("Amount of metal in solder paste (%)")
                .hasArg()
                .argName("AMOUNT")
                .longOpt("metal")
                .build())

        options.addOption("h", "help", false,"Print Help")

        try {

            val parser = DefaultParser()
            val cmd = parser.parse(options, args)

            if(cmd.hasOption("h")){
                val formatter = HelpFormatter()

                val header = "Extracts the amount of solder paste used for the input board\n\n"
                val footer = ""

                formatter.printHelp("solderpasteusage", header, options, footer, true)
                return

            }

            if(!cmd.hasOption("f")){
                System.err.println("Kicad PCB file required")
                return
            }

            val file = File(cmd.getOptionValue("f"))
            if(!file.exists()){
                System.err.println("Kicad PCB file not found - ${file.absolutePath}")
                return
            }

            val inputStream: InputStream = BufferedInputStream(FileInputStream(file))
            val data = SexpFactory.parse(inputStream)

            val amountOfMetal = if(cmd.hasOption("m")) cmd.getOptionValue("m").toDouble() else 0.8775                  // %
            val stencilThickness = if(cmd.hasOption("s")) cmd.getOptionValue("s").toDouble() else 0.12
            val solderPasteGravity = (ALLOY_SPECIFIC_GRAVITY * FLUX_SPECIFIC_GRAVITY) / (((1.0 - amountOfMetal) * ALLOY_SPECIFIC_GRAVITY) + (amountOfMetal * FLUX_SPECIFIC_GRAVITY))

            val allPads = filterByType(data, "module").map { filterByType(it, "pad") }.flatten()
            val totalAreaFront = allPads.map { getArea(it, "F.Paste") }.sum()         //mm2
            val totalAreaBack = allPads.map { getArea(it, "B.Paste") }.sum()          //mm2
            val totalVolumeFront = totalAreaFront * stencilThickness                        //mm3
            val totalVolumeBack = totalAreaBack * stencilThickness                          //mm3

            val solderPasteFront = (totalVolumeFront / 1000.0) * solderPasteGravity  // g
            val solderPasteBack  = (totalVolumeBack  / 1000.0) * solderPasteGravity  // g

            System.out.println("Metal Amount: \t\t\t${String.format("%.2f", amountOfMetal * 100)} %")
            System.out.println("Stencil Thickness: \t\t${String.format("%.2f", stencilThickness)} mm")
            System.out.println("Solder Paste Gravity: \t${String.format("%.2f", solderPasteGravity)} g/cm3")

            System.out.println("------------------------------------")

            System.out.println("Total Number of Pads: \t${allPads.size}")
            System.out.println("Pad Area - Front: \t\t${String.format("%.2f", totalAreaFront)} mm2")
            System.out.println("Pad Area - Back: \t\t${String.format("%.2f", totalAreaBack)} mm2")

            System.out.println("------------------------------------")

            System.out.println("Solder Paste - Front: \t${String.format("%.2f", solderPasteFront)} g")
            System.out.println("Solder Paste - Back: \t${String.format("%.2f", solderPasteBack)} g")
            System.out.println("Solder Paste - Total: \t${String.format("%.2f", (solderPasteFront + solderPasteBack))} g")

            System.out.println("------------------------------------")

        } catch (e: Exception){
            System.err.println("Unable to calculate solder paste amount")
            e.printStackTrace()
        }

    }

    private fun getArea(input : Sexp, layer : String) : Double {

        if(getString(input, 0) == "pad" ){

            val type = getString(input, 2)
            val form = getString(input, 3)
            val size = getDoubleList(input, 5)
            val layers = getStringList(input, 6)

            if(type == "smd" && layers?.contains(layer) == true && size != null) {

                when(form){
                    "rect", "roundrect" -> return size[0] * size[1]
                    "circle", "oval" -> return ((size[0] / 2) * (size[1] / 2) * Math.PI)
                }

                System.out.println("Ignored Pad: " + input)
                return 0.0

            }

        }

        return 0.0

    }

    private fun getString(input : Sexp, index : Int) : String? {

        if(input is SexpList && input.length > index && input[index] is SexpString){
            return (input[index] as SexpString).toIndentedString()
        }

        return null

    }

    private fun getStringList(input : Sexp, index : Int) : List<String>? {

        if(input is SexpList && input.length > index && input[index] is SexpList){

            val children = (input[index] as SexpList).toList()
            return children.subList(1, children.size).filter { it is SexpString }.map { (it as SexpString).toIndentedString() }

        }

        return null

    }

    private fun getDoubleList(input : Sexp, index : Int) : List<Double>? {
        return getStringList(input, index)?.map { it.toDouble() }
    }

    private fun filterByType(input : Sexp, type : String) : List<Sexp>{
        return input.toList().filter { it is SexpList && it.length > 0 && (it[0] is SexpString) && (it[0] as SexpString).toIndentedString() == type }
    }

    private fun Sexp.toList() : List<Sexp> {

        val list = mutableListOf<Sexp>()
        for (i in 0 until this.length) {
            list.add(this.get(i))
        }

        return list

    }

}