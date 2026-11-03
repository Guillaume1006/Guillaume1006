# Imprimer avec Bambu Lab (Bambu Studio)

Deux pièces, deux matières. Fichiers déjà orientés et centrés sur le plateau :

| Ordre | Fichier | Matière |
|---|---|---|
| 1 | `bambu_1_support_quadlock_PETG.3mf` : support Quad Lock (plaque au plateau, plot en haut) | **PETG** |
| 2 | `bambu_2_coque_TPU.3mf` : coque (dos au plateau, ouverture vers le haut) | **TPU 95A** |

Bambu Studio affiche le temps d'impression et le poids de filament après le tranchage.
Les deux tiennent sur tous les plateaux Bambu Lab (A1 mini compris).

## Ouvrir
Bambu Studio → **Fichier > Importer > Importer 3MF/STL** (ou glisser le fichier).
Si Bambu Studio demande de charger « la géométrie uniquement », réponds **oui**.
Ne retourne pas les pièces : elles sont déjà dans le bon sens.

## 1. Support Quad Lock — PETG
- Filament : **Bambu PETG HF** (ou Generic PETG), depuis l'AMS ou en externe
- Plaque : Textured PEI (sur Smooth PEI : colle en bâton)
- Process : 0.20mm Standard, **murs : 4**, couches dessus/dessous : 5, **remplissage 100 %**
- **Pas de supports, pas de bordure**
- Essaie-le tout de suite sur ton support Quad Lock : présente-le à 45°, appuie, tourne d'un
  quart de tour, tire fort. Ajustements dans le tableau du `README.md`.

## 2. Coque — TPU 95A (Elegoo TPU 95A Translucent)

### Profil de filament
Fichier fourni : `bambu_filament_Elegoo_TPU_95A.json` (valeurs de l'étiquette Elegoo).
- Bambu Studio → **Fichier > Importer > Importer des configurations…** → choisir le fichier.
  Le filament « Elegoo TPU 95A Translucent » apparaît dans la liste des filaments.
- Si l'import est refusé, crée-le à la main :
  1. Dans **Filament**, choisis **Generic TPU**, clique sur l'icône **Modifier** (crayon)
  2. Onglet **Filament** : buse **230 °C** (première couche **235 °C**, plage 220–240 °C),
     plateau **40 °C** (Textured PEI et première couche), **débit volumétrique max 3,2 mm³/s**
  3. Onglet **Refroidissement** : ventilateur 80–100 %
  4. **Enregistrer sous** → « Elegoo TPU 95A Translucent »

### Bobine
- Le TPU **ne passe pas dans l'AMS / AMS lite** : bobine sur le **support externe**.
  Sur l'écran de l'imprimante ou dans Bambu Studio (onglet Appareil), règle l'emplacement
  **Ext** sur ce filament (type TPU, couleur transparente).
- **Sèche-la avant : 70 °C pendant 8 h** (valeurs Elegoo).

### Impression
- Plaque : Textured PEI
- Process : 0.20mm Standard, **murs : 4**, couches dessus/dessous : 5, remplissage 40 % (Gyroïde)
- **Pas de supports** : le dos est à plat et le rebord d'écran (1,2 mm) s'imprime sans
- Si ça file ou que le fil se tord dans l'extrudeur : baisse le débit max à 2,5 mm³/s
- Bonus : en TPU translucide, la lumière Glyph diffuse aussi à travers la coque.

## 3. Assemblage
1. Pose le support PETG **à l'intérieur** de la coque : la plaque carrée se loge dans le creux
   du dos, le plot sort par le trou rond.
2. Clipse le téléphone : il plaque le support en place. La forme carrée l'empêche de tourner.
3. Optionnel : une goutte de colle souple (type colle néoprène / contact) entre la plaque et le TPU.
