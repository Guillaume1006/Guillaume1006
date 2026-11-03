// Coque pour Nothing Phone (3a) — modèle paramétrique OpenSCAD
// Unités : mm. Repère : X = largeur, Y = longueur (0 = bas, côté USB-C),
// Z = épaisseur (0 = dos du téléphone).
// Cotes du téléphone issues des fiches techniques ; positions des boutons et
// de la caméra APPROXIMATIVES -> à vérifier au pied à coulisse avant impression.
// Deux pièces : la coque en TPU 95A et le support Quad Lock en PETG (insert).
// L'insert se pose à l'intérieur de la coque : sa plaque se loge dans un creux
// du dos, le plot sort par un trou, le téléphone le bloque. Cotes Quad Lock ESTIMÉES.

part = "coque";     // "coque" = coque TPU, "support" = support Quad Lock PETG

/* [Téléphone] */
phone_w   = 77.50;  // largeur (officiel Nothing)
phone_l   = 163.52; // hauteur (officiel Nothing)
phone_t   = 8.35;   // épaisseur (officiel Nothing)
phone_r   = 9;      // rayon des coins (vue de face)

/* [Coque] */
clearance = 0.3;    // jeu autour du téléphone
wall      = 1.6;    // épaisseur des bords
back      = 1.6;    // épaisseur du dos
lip       = 1.2;    // rebord qui recouvre l'écran (1.2 en TPU 95A, 0.8 en PETG)
lip_h     = 1.0;    // épaisseur du rebord (au-dessus de l'écran)

/* [Caméra (dos)] — mesuré sur photo officielle */
cam_w        = 51.8;   // ouverture qui épouse le bloc caméra noir (51 × 23 mm + jeu)
cam_h        = 23.8;
cam_from_top = 35.9;   // du haut du téléphone au centre du bloc caméra
flash_d      = 7.0;    // trou du flash
flash_x      = 6.7;    // décalage du flash (vers la gauche, vu de dos)
flash_from_top = 17.0; // du haut du téléphone au centre du flash
disk_from_top  = 36.6; // centre du disque autour duquel sont les arcs Glyph

/* [Glyph] — 3 arcs lumineux autour du disque */
glyph_open = true;     // false = dos fermé autour du disque
glyph_r    = 32.9;     // rayon des arcs (au centre de la fente)
glyph_band = 3.4;      // largeur des fentes
// angles de début/fin de chaque arc, vus depuis l'intérieur (0 = côté droit de l'écran, 90 = haut)
glyph_arcs = [[14, 53], [161, 212], [309, 338]];

/* [Quad Lock] — support quart de tour, pièce séparée en PETG */
quadlock   = true;   // false = dos lisse, sans trou ni logement
ins_w      = 52;     // côté de la plaque carrée de l'insert (empêche la rotation)
ins_r      = 6;      // rayon des coins de la plaque
ins_t      = 0.8;    // épaisseur de la plaque = profondeur du logement dans le dos
ins_clr    = 0.2;    // jeu entre insert PETG et coque TPU
ql_from_bottom = 70; // du bas du téléphone au centre de la fixation
ql_od      = 44;     // diamètre extérieur du plot
ql_hgt     = 4.5;    // hauteur du plot au-dessus du dos
ql_bore    = 31;     // diamètre de l'ouverture centrale (tête du support)
ql_arm_r   = 18.5;   // rayon extérieur des 4 ergots du support
ql_arm_w   = 8.5;    // largeur des ergots
ql_lip_t   = 1.5;    // épaisseur du rebord qui retient les ergots
ql_chan_h  = 2.2;    // hauteur de la gorge où tournent les ergots
ql_clr     = 0.25;   // jeu
ql_entry   = 45;     // angle d'entrée des ergots (on tourne de 45° pour verrouiller)

/* [Boutons] — distances depuis le haut du téléphone */
right_from = 59;  right_to = 101;  // Power + Essential Key (côté droit)
left_from  = 53;  left_to  = 86;   // Volume (côté gauche)
btn_h      = 4.5;                  // hauteur des fentes

/* [Bas] */
usb_w = 14;  usb_h = 7.5;          // ouverture USB-C (centrée)
spk_w = 58;  spk_h = 4.0;          // fente haut-parleur / micro (centrée)

$fn = 64;
eps = 0.01;

W = phone_w + 2*clearance;
L = phone_l + 2*clearance;
H = back + phone_t + clearance + lip_h;     // hauteur totale

module glyph_cut() {   // fentes en arc à bouts ronds
    for (arc = glyph_arcs)
        for (t = [arc[0] : 3 : arc[1] - 3])
            hull() for (u = [t, min(t + 3, arc[1])])
                translate([glyph_r*cos(u), glyph_r*sin(u), 0]) cylinder(d = glyph_band, h = back + 2*eps, $fn = 24);
}

// Plot Quad Lock, centré à l'origine, base en z = 0, sortant vers -Z
module quadlock_boss() {
    translate([0, 0, -ql_hgt]) cylinder(h = ql_hgt + eps, d = ql_od, $fn = 96);
}
// Insert PETG dans le repère de la coque (face intérieure du dos en z = 0)
module quadlock_insert() {
    difference() {
        union() {
            translate([0, 0, -ins_t]) rrect(ins_w, ins_w, ins_r, ins_t);          // plaque
            translate([0, 0, -back - ql_hgt]) cylinder(h = ql_hgt + back - ins_t + eps, d = ql_od, $fn = 96);
        }
        translate([0, 0, -back]) quadlock_cut();
    }
}
module quadlock_cut() {
    z0 = -ql_hgt;                       // face du plot
    // Ouverture centrale + gorge de rotation
    translate([0, 0, z0 - eps]) cylinder(h = ql_lip_t + ql_chan_h + eps, d = ql_bore, $fn = 96);
    translate([0, 0, z0 + ql_lip_t]) cylinder(h = ql_chan_h, r = ql_arm_r + ql_clr, $fn = 96);
    // 4 entrées pour les ergots
    for (k = [0:3]) rotate([0, 0, ql_entry + 90*k])
        translate([0, -(ql_arm_w/2 + ql_clr), z0 - eps])
            cube([ql_arm_r + ql_clr, ql_arm_w + 2*ql_clr, ql_lip_t + 2*eps]);
}

module rrect(w, l, r, h) {
    linear_extrude(h)
        offset(r = r) square([w - 2*r, l - 2*r], center = true);
}
module stadium(w, h, depth) {   // pastille horizontale
    hull() for (s = [-1, 1]) translate([s*(w - h)/2, 0, 0]) cylinder(d = h, h = depth);
}

if (part == "support") {
    quadlock_insert();
} else difference() {
    // Coque extérieure, centrée sur (0, L/2)
    translate([0, L/2, -back]) rrect(W + 2*wall, L + 2*wall, phone_r + clearance + wall, H);

    // Logement de l'insert Quad Lock (creux intérieur + trou pour le plot)
    if (quadlock) translate([0, clearance + ql_from_bottom, 0]) {
        translate([0, 0, -ins_t]) rrect(ins_w + 2*ins_clr, ins_w + 2*ins_clr, ins_r + ins_clr, ins_t + eps);
        translate([0, 0, -back - eps]) cylinder(h = back + 2*eps, d = ql_od + 2*ins_clr, $fn = 96);
    }

    // Logement du téléphone
    translate([0, L/2, 0]) rrect(W, L, phone_r + clearance, phone_t + clearance + eps);
    // Ouverture de l'écran (rebord de "lip" mm)
    translate([0, L/2, phone_t]) rrect(W - 2*lip, L - 2*lip, phone_r - lip, H);

    // Caméra (contour du bloc noir)
    translate([0, L - clearance - cam_from_top, -back - eps]) stadium(cam_w, cam_h, back + 2*eps);
    // Flash
    translate([flash_x, L - clearance - flash_from_top, -back - eps]) cylinder(d = flash_d, h = back + 2*eps, $fn = 48);

    // Ouvertures Glyph
    if (glyph_open)
        translate([0, L - clearance - disk_from_top, -back - eps]) glyph_cut();

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
