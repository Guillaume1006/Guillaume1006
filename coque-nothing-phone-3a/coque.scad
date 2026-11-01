// Coque pour Nothing Phone (3a) — modèle paramétrique OpenSCAD
// Unités : mm. Repère : X = largeur, Y = longueur (0 = bas, côté USB-C),
// Z = épaisseur (0 = dos du téléphone).
// Cotes du téléphone issues des fiches techniques ; positions des boutons et
// de la caméra APPROXIMATIVES -> à vérifier au pied à coulisse avant impression.
// Impression conseillée : TPU 95A, couche 0.2 mm, dos à plat sur le plateau.

/* [Téléphone] */
phone_w   = 77.5;   // largeur
phone_l   = 163.5;  // hauteur
phone_t   = 8.4;    // épaisseur
phone_r   = 9;      // rayon des coins (vue de face)

/* [Coque] */
clearance = 0.3;    // jeu autour du téléphone
wall      = 1.6;    // épaisseur des bords
back      = 1.6;    // épaisseur du dos
lip       = 1.2;    // rebord qui recouvre l'écran
lip_h     = 1.0;    // épaisseur du rebord (au-dessus de l'écran)

/* [Caméra (dos)] — îlot centré en haut */
cam_w     = 44;     // largeur de l'ouverture
cam_h     = 28;     // hauteur de l'ouverture
cam_from_top = 26;  // du haut du téléphone au centre de l'ouverture

/* [Glyph] — ouvertures autour de la caméra pour laisser passer la lumière */
glyph_open   = true;            // false = dos plein
glyph_gap    = 2.0;             // matière laissée autour de l'ouverture caméra
glyph_band   = 6.0;             // largeur de la bande ouverte
bridge_w     = 4.0;             // largeur des ponts de matière
bridge_angles = [90, 210, 330]; // position des ponts (degrés, 0 = droite, 90 = haut)

/* [Boutons] — distances depuis le haut du téléphone */
right_from = 33;  right_to = 82;   // Power + Essential Key (côté droit)
left_from  = 33;  left_to  = 78;   // Volume (côté gauche)
btn_h      = 4.5;                  // hauteur des fentes

/* [Bas] */
usb_w = 14;  usb_h = 7.5;          // ouverture USB-C (centrée)
spk_w = 58;  spk_h = 4.0;          // fente haut-parleur / micro (centrée)

$fn = 64;
eps = 0.01;

W = phone_w + 2*clearance;
L = phone_l + 2*clearance;
H = back + phone_t + clearance + lip_h;     // hauteur totale

module glyph_cut() {
    difference() {
        stadium(cam_w + 2*(glyph_gap + glyph_band), cam_h + 2*(glyph_gap + glyph_band), back + 2*eps);
        translate([0, 0, -eps]) stadium(cam_w + 2*glyph_gap, cam_h + 2*glyph_gap, back + 4*eps);
        for (a = bridge_angles)
            rotate([0, 0, a]) translate([0, -bridge_w/2, -eps]) cube([100, bridge_w, back + 4*eps]);
    }
}

module rrect(w, l, r, h) {
    linear_extrude(h)
        offset(r = r) square([w - 2*r, l - 2*r], center = true);
}
module stadium(w, h, depth) {   // pastille horizontale
    hull() for (s = [-1, 1]) translate([s*(w - h)/2, 0, 0]) cylinder(d = h, h = depth);
}

difference() {
    // Coque extérieure, centrée sur (0, L/2)
    translate([0, L/2, -back]) rrect(W + 2*wall, L + 2*wall, phone_r + clearance + wall, H);

    // Logement du téléphone
    translate([0, L/2, 0]) rrect(W, L, phone_r + clearance, phone_t + clearance + eps);
    // Ouverture de l'écran (rebord de "lip" mm)
    translate([0, L/2, phone_t]) rrect(W - 2*lip, L - 2*lip, phone_r - lip, H);

    // Caméra
    translate([0, L - clearance - cam_from_top, -back - eps]) stadium(cam_w, cam_h, back + 2*eps);

    // Ouvertures Glyph
    if (glyph_open)
        translate([0, L - clearance - cam_from_top, -back - eps]) glyph_cut();

    zc = phone_t/2;
    // Côté droit (+X) : Power + Essential Key
    translate([W/2 - 1, L - clearance - right_to, zc - btn_h/2])
        cube([wall + 2, right_to - right_from, btn_h]);
    // Côté gauche (-X) : volume
    translate([-W/2 - wall - 1, L - clearance - left_to, zc - btn_h/2])
        cube([wall + 2, left_to - left_from, btn_h]);
    // Bas : USB-C + haut-parleur/micros
    translate([0, 0, zc]) rotate([90, 0, 0]) translate([0, 0, -1]) stadium(usb_w, usb_h, wall + 2);
    translate([0, 0, zc]) rotate([90, 0, 0]) translate([0, 0, -1]) stadium(spk_w, spk_h, wall + 2);
}
