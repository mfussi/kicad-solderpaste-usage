# kicad-solderpaste-usage
Calculate the amount of solder paste usage based on kicad board files. Additionally you can provide the stencil thickness and percentual amount of metal in your solder paste. The calculation is based on following site: https://www.indium.com/blog/calculating-solder-paste-usage-1.php

Custom shaped pads are ignored, only rectangle, rounded rectangle, circle and oval pads are used for the calculation.

## Example:

```bash 
java -jar solder-paste-usage-1.1.jar -h 
```

```bash
java -jar solder-paste-usage-1.1.jar \
  -f path/to/board.kicad_pcb \
  -s 0.12 \
  -m 88 \
  -a 8.74 \
  -x 1.0
```

## Arguments

| Option | Long form | Description | Default |
|--------|------------|-------------|----------|
| `-f` | `--file` | Path to the KiCad `.kicad_pcb` board file (**required**) | — |
| `-s` | `--stencil` | Stencil thickness in millimeters | `0.12 mm` |
| `-m` | `--metal` | Metal content of the solder paste (can be percent or fraction, e.g. `88` or `0.88`) | `87.75 %` |
| `-a` | `--alloy-density` | Alloy density in g/cm³ | `8.74 g/cm³` |
| `-x` | `--flux-density` | Flux density in g/cm³ | `1.00 g/cm³` |
| `-h` | `--help` | Print help and exit | — |

## Sample Output:
```
Metal Fraction:          87.75 %
Stencil Thickness:       0.120 mm
Alloy Density:           8.740 g/cm3
Flux Density:            1.000 g/cm3
Paste Density (mix):     4.486 g/cm3
------------------------------------
Total Number of Pads:    2733
Pad Area - Front:        2693.11 mm2
Pad Area - Back:         4348.80 mm2
------------------------------------
Solder Paste - Front:    1.45 g
Solder Paste - Back:     2.34 g
Solder Paste - Total:    3.79 g
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
