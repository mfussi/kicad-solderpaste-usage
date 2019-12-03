# kicad-solderpaste-usage
Calculate the amount of solder paste usage based on kicad board files. Additionally you can provide the stencil thickness and percentual amount of metal in your solder paste. The calculation is based on following site: https://www.indium.com/blog/calculating-solder-paste-usage-1.php

Custom shaped pads are ignored, only rectangle, rounded rectangle, cirle and oval pads are used for the calculation.

## Usage:
``` java -jar solder-paste-usage-1.0.jar -h ```

``` java -jar solder-paste-usage-1.0.jar -f 'path/to/kicad/pcb/file.kicad_pcb' ```

## Sample Output:
```
Metal Amount: 			87.75 %
Stencil Thickness: 		0.12 mm
Solder Paste Gravity: 	4.15 g/cm3
------------------------------------
Total Number of Pads: 	2733
Pad Area - Front: 		2693.11 mm2
Pad Area - Back: 		4348.80 mm2
------------------------------------
Solder Paste - Front: 	13.41 g
Solder Paste - Back: 	21.65 g
Solder Paste - Total: 	35.05 g
------------------------------------
```
