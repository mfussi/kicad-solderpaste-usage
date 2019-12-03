# kicad-solderpaste-usage
Calculate the amount of solder paste usage based on kicad board files. Additionally you can provide the stencil thickness and percentual amount of metal in your solder paste. The calculation is based on following site: https://www.indium.com/blog/calculating-solder-paste-usage-1.php

Custom shaped pads are ignored, only rectangle, rounded rectangle, circle and oval pads are used for the calculation.

## Usage:
``` java -jar solder-paste-usage-1.0.jar -h ```

``` java -jar solder-paste-usage-1.0.jar -f 'path/to/kicad/pcb/file.kicad_pcb' ```

## Sample Output:
```
Metal Amount: 			87.75 %
Stencil Thickness: 		0.12 mm
Solder Paste Gravity: 	4.15 g/cm3
------------------------------------
Total Number of Pads: 	262
Pad Area - Front: 		152.89 mm2
Pad Area - Back: 		356.70 mm2
------------------------------------
Solder Paste - Front: 	0.76 g
Solder Paste - Back: 	1.78 g
Solder Paste - Total: 	2.54 g
------------------------------------
```

## Binary:
https://github.com/mfussi/kicad-solderpaste-usage/tree/master/release

License
=======

    Copyright 2019 Markus Fußenegger

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
