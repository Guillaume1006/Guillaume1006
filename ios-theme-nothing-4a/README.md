# Thème style iPhone pour Nothing Phone (4a)

Nothing OS n'a pas de système de « thèmes » installables en un clic. Le look iOS s'obtient en combinant 5 éléments. Ce dossier fournit les fonds d'écran ; le reste se télécharge sur le Play Store.

## 1. Fonds d'écran (fournis)
Dossier `wallpapers/` : `ios-bleu`, `ios-violet`, `ios-aurore`, `ios-clair`, `ios-noir` (1084×2392, dégradés flous façon iOS).
Pour en régénérer ou changer la résolution : `python3 generate_wallpapers.py` (modifier `W, H`).

Transférez-les sur le téléphone, puis : appui long sur l'écran d'accueil → Fond d'écran.

## 2. Launcher (écran d'accueil)
Installez **Lawnchair** ou **Nova Launcher** (ou « Launcher iOS » / « Phone 16 Launcher » si vous voulez du clé en main, mais avec pubs).
Réglages recommandés :
- Grille : 4 colonnes × 6 lignes
- Taille des icônes : ~100 %, étiquettes en blanc avec légère ombre
- Dock : 4 icônes, fond translucide arrondi
- Animations : « Ressort » / rapides
- Dossiers : fond translucide, aperçu en grille 3×3

## 3. Pack d'icônes
Au choix sur le Play Store : **iOS 17 Icon Pack**, **One UI → iOS**, **Delta Icon Pack**, **Whicons**.
Lawnchair/Nova → Réglages → Pack d'icônes → sélectionner.
Pour les icônes manquantes, activez « Icônes adaptatives » ou « Forme : squircle ».

## 4. Widgets
- **KWGT** + pack de presets iOS (horloge, météo, batterie, calendrier).
- Ou **Widgetsmith**-like : « iOS 17 Widgets ».
- Désactivez les widgets Nothing (points) pour rester cohérent.

## 5. Centre de contrôle, notifications, police
- **Centre de contrôle** : app « Control Center iOS » ou « Cuberto Control Center ». Accordez l'accessibilité demandée.
- **Police** : Nothing OS → Réglages → Affichage → Style de police ; sinon via **SF Pro** avec un launcher compatible (Nova ne change pas la police système).
- **Écran de verrouillage** : Réglages → Écran de verrouillage → horloge centrée, style fin ; ajoutez un widget météo.
- **Geste** : Réglages → Système → Navigation → Gestes (barre en bas, comme iOS).
- **Mode sombre** : cohérent avec `ios-noir` ou `ios-clair` selon le mode.

## 6. Pour aller plus loin
Voulez-vous un vrai APK (icon pack personnalisé, preset KWGT) ? Ça demande plus de travail (créer ~100 icônes) ; dites-le-moi et je peux générer le projet Android.
