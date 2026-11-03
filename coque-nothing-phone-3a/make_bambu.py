"""Crée des fichiers .3mf prêts à ouvrir dans Bambu Studio, déjà orientés pour l'impression.
Usage : python make_bambu.py   (après build_stl.py)
- bambu_1_support_quadlock_PETG.3mf : support Quad Lock, plaque sur le plateau, plot vers le haut
- bambu_2_coque_TPU.3mf             : coque TPU 95A, dos sur le plateau (aucun support nécessaire)
"""
import zipfile
import numpy as np
import trimesh

BED = 128.0  # centre du plateau (Bambu A1 / P1 / X1 : 256 mm ; l'A1 mini se recentre tout seul)


def prepare(path, flip):
    m = trimesh.load(path)
    if flip:  # retourne la pièce : l'écran (le haut du modèle) se pose sur le plateau
        m.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [0, 1, 0]))
    lo, hi = m.bounds
    m.apply_translation([BED - (lo[0] + hi[0]) / 2, BED - (lo[1] + hi[1]) / 2, -lo[2]])
    return m


def write_3mf(mesh, name, out):
    v = "\n".join(f'<vertex x="{x:.4f}" y="{y:.4f}" z="{z:.4f}"/>' for x, y, z in mesh.vertices)
    t = "\n".join(f'<triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in mesh.faces)
    model = f'''<?xml version="1.0" encoding="UTF-8"?>
<model unit="millimeter" xml:lang="fr-FR" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02">
<metadata name="Title">{name}</metadata>
<metadata name="Designer">Guillaume</metadata>
<resources>
<object id="1" name="{name}" type="model">
<mesh>
<vertices>
{v}
</vertices>
<triangles>
{t}
</triangles>
</mesh>
</object>
</resources>
<build>
<item objectid="1"/>
</build>
</model>
'''
    ct = '''<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>
</Types>
'''
    rels = '''<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Target="/3D/3dmodel.model" Id="rel0" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
</Relationships>
'''
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", ct)
        z.writestr("_rels/.rels", rels)
        z.writestr("3D/3dmodel.model", model)
    print(f"{out} : {mesh.extents.round(1)} mm, étanche={mesh.is_watertight}")


write_3mf(prepare("support_quadlock.stl", flip=True), "Support Quad Lock PETG", "bambu_1_support_quadlock_PETG.3mf")
write_3mf(prepare("coque.stl", flip=False), "Coque Nothing Phone 3a TPU", "bambu_2_coque_TPU.3mf")
