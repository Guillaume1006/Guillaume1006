Scripts utilisés pour préparer les rendus de la voiture (non fournis ici, usage personnel) :

- `recolor2.py` : repeint la carrosserie en gris quasi noir métallisé, vitres arrière teintées, verre avant atténué.
- `plate.py` : pose la plaque française en perspective.

Usage : `python recolor2.py front34|top34 entrée_détourée.png sortie.png`
Les images obtenues vont dans `res/drawable-nodpi/car_home.png` et `car_open.png`
(elles peuvent aussi être extraites d'un APK existant : `unzip A3Cockpit.apk 'res/drawable-nodpi-v4/car_*'`).
