# Rendre Portalcraft data-driven : générer des dimensions et des biomes procéduraux à partir des blocs du portail

## Vision et contraintes produit

L’idée de faire dépendre la destination d’un portail du **“matériau”** choisi (et plus généralement de la *composition* du cadre) s’inscrit très bien dans la philosophie “exploration par contraintes” popularisée par la snapshot du poisson d’avril 2020 : un simple **input** (un livre jeté dans un portail) se transforme en un **monde généré** avec une identité propre. citeturn10search0turn10search1

Le pivot “data-driven” que tu décris implique généralement trois exigences techniques, si tu veux que ça devienne un *outil d’exploration* plutôt qu’une liste de portails hardcodés :

1) **Déterminisme / reproductibilité** : une même “recette de portail” (multiset de blocs) doit générer la même dimension (mêmes biomes, mêmes règles de surface, mêmes couleurs, etc.), idéalement sans dépendre d’un état caché. citeturn10search0  
2) **Généralité** : ça doit marcher avec *n’importe quel bloc* (vanilla + moddé), donc ton système doit combiner des **règles explicites** (ex : obsidienne → Nether) et des **heuristiques / inférences** (ex : “blocs très lumineux” → îles flottantes). citeturn3search1turn3search7  
3) **Génération “vraiment Minecraft”** : pour que l’exploration soit riche, il faut piloter les leviers de worldgen existants (dimension_type, noise_settings, biome_source, biomes, features, carvers…), plutôt que de faire uniquement un “swap de blocs” superficiel. citeturn3search5turn3search10turn3search8

Enfin, tu as déjà une contrainte forte implicite : tu veux **créer / choisir la dimension au moment où le joueur active le portail** (et pas uniquement à la création du monde). Or, vanilla n’est pas fait pour “ajouter des dimensions en continu” via simple datapack reload : les dimensions sont chargées au démarrage du monde, et les modifications requièrent un redémarrage du monde (pas juste `/reload`). citeturn3search0

image_group{"layout":"carousel","aspect_ratio":"16:9","query":["Minecraft 20w14 infinite portal book dimension screenshot","Minecraft snapshot 20w14∞ infinite dimensions portal","Minecraft Infinite Dimensions mod portal color change"] ,"num_per_query":1}

## Ce que Minecraft expose vraiment côté génération de dimensions

### Les briques de base côté datapacks

En Java Edition, une dimension “data-driven” se décrit via des fichiers JSON dans un datapack :  
- `data/<namespace>/dimension/<name>.json` pour la **dimension** (son générateur) citeturn3search0turn3search1  
- `data/<namespace>/dimension_type/<name>.json` pour le **type de dimension** (règles de lumière, ciel, coord scale, etc.) citeturn3search1turn3search8  

Le générateur (dans la dimension) supporte typiquement `noise`, `flat`, `debug`. Pour `minecraft:noise`, tu relies :  
- des **noise settings** (terrain + caves + blocs par défaut + surface rules)  
- une **source de biomes** (*biome_source*) qui contrôle comment les biomes sont distribués. citeturn3search7turn3search5  

Côté *biome_source*, vanilla propose (au minimum) : `fixed`, `checkerboard`, `the_end`, `multi_noise`. `multi_noise` est le mode “climat” (très utilisé, notamment au Nether et en Overworld moderne) et peut être défini via un preset ou une liste de biomes + points cibles. citeturn3search7

### Noise settings, surface rules, features, carvers : où se joue “l’identité” du monde

Les **noise settings** sont décrits comme le cœur de la forme du terrain (incluant les **noise caves**) et de “ce avec quoi le terrain est généré”, et ils incluent aussi les **surface rules** (qui changent les blocs selon conditions). citeturn3search5turn3search9  

Les **biomes** (définis par JSON) portent non seulement des paramètres de climat, mais aussi : carvers, features, règles de spawn, couleurs (ciel/eau/fog/feuillage), ambiance sonore, etc. citeturn3search5turn3search10  

Et la granularité “décor” suit une séparation importante :  
- **features** : décorations locales (arbres, fleurs, minerais…), placées par chunk, typiquement “petites” citeturn3search5  
- **carvers** : grottes/canions “carvés” (en plus des noise caves) citeturn3search5  

### Dimension type : ciel, plafond, règles de respawn, échelle des coordonnées

Le *dimension_type* contrôle des aspects essentiels pour ton design Portalcraft :  
- `has_skylight`, `has_ceiling`, `ambient_light`, `fixed_time` (ressenti visuel) citeturn3search8  
- `coordinate_scale` (équivalent du facteur Overworld ↔ Nether si tu veux) citeturn3search8  
- règles “Nether-like” : `piglin_safe`, `bed_works`, `respawn_anchor_works`, et le champ `effects` (overworld / the_nether / the_end) qui influence fortement le rendu (fog/ciel). citeturn3search8  
- `min_y` et `height` : pour contrôler la verticalité “caves profondes”, “monde compact”, etc. citeturn3search8  

### Deux implications techniques cruciales pour un système “dimension = recette”

1) Dans les versions modernes, la graine du monde est utilisée pour toutes les dimensions et certains champs de seed dédiés ont été retirés (ex : retrait du `seed` du generator `noise` / `the_end` biome source, et “world seed always used”). Ça veut dire que si tu veux que *chaque recette* ait sa propre “graine”, tu dois **salter** ta génération autrement (ex : via ton chunk generator moddé / tes noises / tes mappings), pas simplement via un champ JSON de seed. citeturn2search1  
2) Les dimensions “datapack” ne se prêtent pas bien à l’ajout “à chaud” en gameplay (cf. redémarrage requis). Donc ton pivot data-driven doit probablement être : **data-driven pour les règles + templates**, mais **runtime pour la création/activation des dimensions**. citeturn3search0turn3search1  

## Architecture data-driven recommandée pour Portalcraft

L’approche la plus robuste (et modpack-friendly) est de séparer ton système en deux couches : **(A) une spec data-driven** et **(B) un compilateur runtime**.

### La spec : “Portal Recipe → DimensionSpec”

Au moment où un portail est activé, tu extrais une **signature canonique** du cadre :

- liste des blocs utilisés + quantités (multiset)  
- éventuellement des métadonnées : “blocs dominants”, “palette de couleurs”, “luminosité moyenne”, “catégories via tags”, etc. (tout ce qui te sert à déduire un style)  

Puis tu produis une `DimensionSpec` (objet interne) contenant typiquement :

- `dimensionKey` (un ID stable dérivé par hash)  
- `dimensionTypeTemplate` (Nether-like / End-like / Overworld-like) citeturn3search8  
- `generatorTemplate` (noise settings de base, style de biomes : fixed vs multi_noise…) citeturn3search7turn3search5  
- `palette` (pools de blocs : terrain, surface, décor, végétation, minerais, fluides)  
- `coherenceScore` (pour décider si tu tends vers “monde cohérent” ou “monde très aléatoire”)  

Le **data-driven** peut porter deux types de contenus :

- des **règles explicites** (matchers) : “si 100% obsidian → dimension vanilla Nether”, “si 100% glowstone → floating islands lumineuses”, etc. (c’est ton équivalent propre et extensible du hardcode, mais en JSON)  
- des **templates** paramétrables : presets de terrain, presets de biomes, politiques de palette, etc. citeturn3search5turn3search7turn3search8  

### Pourquoi il faut un compilateur runtime

Si tu veux générer potentiellement un grand nombre de dimensions “à la demande”, tu vas vite rencontrer les limites du datapack-only (pas pratique à créer, pas reloadable comme tu veux, etc.). citeturn3search0  

Donc le pattern le plus réaliste est :

- les JSON de ton mod définissent règles & templates  
- ton mod compile `DimensionSpec` → objets worldgen (DimensionType, ChunkGenerator, BiomeSource, Biomes) et crée/branche la dimension.

Côté écosystème, il existe déjà des briques réutilisables :  
- entity["organization","Infiniverse","forge api mod"] (par entity["people","Commoble","minecraft mod developer"]) est présenté comme un API mod Forge permettant d’ajouter/enlever des dimensions **pendant l’exécution serveur**. citeturn9search0  
- entity["organization","DynamicDimensions","runtime dimensions library"] (par entity["organization","TeamGalacticraft","modding team"]) expose une API pour créer des dimensions dynamiques, avec une note importante : la lib **ne mémorise pas** automatiquement ce qui a été créé après reboot, donc tu dois persister ta liste/ton mapping et restaurer au démarrage. citeturn9search2  

Même si tu n’utilises pas ces libs telles quelles, elles indiquent la direction correcte : la “dimension infinie” en gameplay est un problème connu, et l’API runtime (et la persistance) sont les points durs. citeturn9search0turn9search2  

## Génération du terrain à partir des blocs : templates, presets, et “salting”

### Exploiter les presets de noise settings pour aller vite

Minecraft fournit des noise settings vanilla adaptés à des archétypes utiles à Portalcraft : `minecraft:overworld`, `minecraft:nether`, `minecraft:end`, mais aussi des variantes dont tu peux tirer des mondes très typés comme `minecraft:floating_islands` ou `minecraft:caves` (décrit comme “Nether-like generation but with Overworld terrain features”). citeturn3search5  

C’est une aubaine pour tes exemples :

- **Glowstone-only → île flottante** : partir de `minecraft:floating_islands`, puis appliquer tes règles de palette (surface/terrain glowstone + décor cohérent). citeturn3search5turn3search8  
- **Stone(+ores) → monde cavernicole** : partir de `minecraft:caves` ou d’un terrain “nether-like” + forcer un `dimension_type` à plafond (`has_ceiling`) et un rendu “nether-ish” via `effects = minecraft:the_nether` si tu veux le même ressenti (fog épais). citeturn3search5turn3search8  
- **Obsidian-only → Nether** : cas spécial : au lieu de générer, tu peux router directement vers la dimension Nether existante (ton système de règles explicites). (Tu conserves l’option de *générer* un Nether “obsidian-thématisé” si tu veux un mode “exploration”). citeturn3search8turn3search1  

### “Monde couleur” : définir la couleur d’un bloc de façon stable

Pour un “monde monochrome” ou “palette de couleurs”, il te faut une notion de couleur *robuste* (pas dépendante d’un resource pack). Le candidat le plus stable côté moteur, en Java, est la **MapColor** : elle représente la couleur de surface d’un bloc telle qu’utilisée par le rendu de carte (MapRenderer). citeturn4search5turn4search0  

Même si la table détaillée des couleurs de carte peut varier/être mise à jour selon versions, le concept “les cartes utilisent une table de couleurs et les blocs sont colorés par matériau / base color” est documenté. citeturn4search0  

Concrètement, ton pipeline “couleur” peut être :

- dériver une **palette de MapColors** depuis les blocs du portail  
- construire un pool de blocs candidats dont la MapColor ∈ palette  
- utiliser ce pool pour : surface blocks, décorations, voire remplacement de blocs “décor” post-gen.

### Salting : rendre chaque recette “vraiment unique”

Comme mentionné plus haut, l’ancien champ de seed dédié au generator `noise` n’est plus utilisable de la même manière dans les versions modernes (le monde seed est utilisé). citeturn2search1  

Donc, pour obtenir un monde *unique par recette*, tu dois injecter une source de différenciation :

- soit via une **dimensionKey** différente (dimension ID différent) + ton propre “salt” interne dans la sélection des blocs/features  
- soit via un **ChunkGenerator custom** (ou wrappers) qui utilise `hash(dimensionKey, chunkX, chunkZ)` pour choisir : palette, variations de surface, densité de features, etc.

C’est exactement l’esprit des systèmes “infinite dimensions” réimplémentés en mod : par exemple le mod “Infinite Dimensions” annonce des dimensions générées à partir d’un input (book), avec terrain/ciels/“flore faite de blocs aléatoires” et un système de poids configurables pour la sélection des blocs (donc une “policy” data-driven de randomisation). citeturn9search5turn10search21  

## Génération de biomes à partir de matériaux : du “monobiome” à la multi-cohérence

### Deux niveaux de solution : biome-light vs biome-full

**Biome-light (recommandé pour une V1 solide)**  
Tu crées la dimension avec une distribution de biomes simple (souvent `fixed` ou une petite liste `multi_noise`) mais tu mets l’essentiel de la “signature matériaux” dans :  
- les noise settings (default blocks + surface rules) citeturn3search5turn3search9  
- une couche de “block palette policy” (remplacement / sélection)  

Avantage : tu évites de générer des centaines de nouveaux biomes et tu gardes la compatibilité et la sobriété. Les biomes côté JSON sont puissants mais très riches et donc coûteux à générer “massivement” si tu en crées un par recette. citeturn3search10turn3search5  

**Biome-full (V2/V3, pour le mode “outil d’exploration”)**  
Tu génères de “vrais” biomes uniques par recette, en clonant un biome template et en patchant :  
- couleurs, précipitations, température (et donc parfois teintes herbe/feuillage) citeturn3search10  
- liste de features / carvers / mobs (en remplaçant les providers de blocs) citeturn3search5turn3search10  

### Comment implémenter “si un seul bloc → biome only”

Tu peux directement mapper ce comportement au biome_source `fixed` : `fixed` signifie “un seul biome”. citeturn3search7  

En pratique :

- recette = `{blockA: 100%}`  
- `DimensionSpec` choisit :  
  - un seul biome (custom ou template)  
  - terrain preset dépendant de blockA (ex : `caves` si stone, `floating_islands` si glowstone…) citeturn3search5turn3search7  

### Comment traiter “un bloc correspond à plusieurs biomes” (ex : stone)

C’est le point clé que tu soulèves : certains blocs sont **peu informatifs** car omniprésents (stone, dirt), et d’autres sont très informatifs (corail, nylium, end stone, etc.). citeturn3search5turn10search1  

Le pattern classique (et très efficace) est d’utiliser un scoring “informatif” :

- associer à chaque bloc un ensemble d’“affinités” (biomes/dimensions/thèmes)  
- pondérer ces affinités par une mesure de rareté / spécificité (type IDF : plus un bloc apparaît “partout”, moins il doit peser)  
- produire une distribution finale de thèmes/biomes.

Même si Minecraft vanilla ne te fournit pas directement “block → biomes”, tu peux construire ta propre base **data-driven** de deux façons :

- **Curated rules** (JSON) pour les blocs “spéciaux” (obsidienne, glowstone, nylium, end stone, coral…)  
- **Heuristiques** pour tous les autres : tags, MapColor, luminance, résistance, transparence, “est-ce une plante”, etc. (critères runtime côté BlockState)

Ce type de logique “scanner tout le contenu et appliquer des règles” existe déjà dans des mods orientés dimensions : par exemple la doc de RFTools Dimensions décrit que le mod scanne blocs/fluids/mobs/biomes et cherche des règles qui matchent chaque concept (logique de “filters”). citeturn9search25  

### “Monde presque aléatoire si matériaux incohérents” : formaliser la cohérence

Pour reproduire ton exemple “comme 2020 April Fools” quand les matériaux ne “se correspondent pas”, tu peux définir un `coherenceScore` :

- score élevé si les blocs partagent : même famille (tags), même MapColor, même “niveau de naturalité” (naturel vs industriel), mêmes affinités dimensionnelles  
- score faible si tu as une distribution très dispersée.

Ensuite :

- **cohérent** → tu privilégies des presets stables (overworld/caves/nether) + une palette contrainte  
- **incohérent** → tu actives un mode “whacky” : mélange de biomes plus extrême (`multi_noise` avec targets très variées), palettes plus larges, substitutions plus agressives, couleurs de ciel/bris plus exotiques, etc. citeturn3search7turn10search0turn9search5  

## Feuille de route d’implémentation et pièges à anticiper

### Étape fondation : règles + templates data-driven

Commence par remplacer ton hardcode par un moteur de règles, même si les dimensions générées restent simples :

- format JSON “PortalRule” : conditions sur la recette (exact blocks, tags, “single block only”, “all luminous”, “palette MapColor”)  
- résultat :  
  - soit “link vers dimension existante” (Nether / End / Overworld)  
  - soit “Template” (caves/floating_islands/overworld) + policy de palette.

Ça te donne immédiatement la flexibilité modpack : ajouter la règle “portal en X → monde Y” sans recompiler. citeturn3search1turn3search7  

### Étape runtime : création de dimensions en jeu + persistance

Comme les dimensions datapack sont chargées à l’ouverture du monde (pas au `/reload`), tu as besoin d’un mécanisme runtime si tu veux créer des dimensions à la volée. citeturn3search0  

Deux options réalistes :

- Intégrer une lib type entity["organization","Infiniverse","forge api mod"] (Forge) qui annonce explicitement add/remove dimensions “during server runtime”. citeturn9search0  
- Intégrer entity["organization","DynamicDimensions","runtime dimensions library"] (Fabric/NeoForge) qui donne une API `createDynamicDimension`, mais te demande de gérer la restauration après redémarrage. citeturn9search2  

Dans les deux cas, tu dois implémenter :
- une sauvegarde de ton mapping `dimensionKey → DimensionSpec` (SavedData/NBT/fichier)  
- une restauration au chargement serveur (recréer/reattacher les dimensions nécessaires). citeturn9search2turn9search0  

### Étape worldgen : commencer “simple mais robuste”, puis monter en complexité

Une progression pragmatique (qui évite de te perdre dans la complexité des density functions/surface rules dès la première itération) :

- **V1 jouable** :  
  - 3–5 presets de terrain basés sur les noise settings vanilla (`overworld`, `caves`, `floating_islands`, `nether`, `end`) citeturn3search5  
  - `fixed` biome_source pour monobiome et une petite config `multi_noise` pour multi-biomes (ex : 3–7 biomes). citeturn3search7  
  - une palette de blocs par catégories (terrain/surface/décor)  
- **V2** :  
  - surface rules plus riches (ex : couches, transitions, conditions “au-dessus de la mer”, etc.), puisque la surface_rule est explicitement un levier des noise settings. citeturn3search5turn3search9  
  - carvers/features custom pour renforcer les thèmes (ex : dimension purement cavernicole). citeturn3search5  
- **V3 “outil d’exploration”** :  
  - génération de biomes custom (couleurs + features + spawns) à partir de templates. citeturn3search10turn3search5  

### Pièges concrets à anticiper

- **Compatibilité versions** : des règles ont changé au fil des versions (ex : seed dédié retiré, dimension_type à référencer séparément, etc.). Si tu veux supporter plusieurs versions, garde une couche d’abstraction et documente “versions supportées”. citeturn2search1turn3search1  
- **Coût/perf** : si tu scannes tout le registre des blocs à chaque activation, cache les résultats (ex : MapColor → liste de blocs) et ne recompute que sur resource reload. Le système de registries dynamiques est un concept de base côté modding (registries “file-loaded”, et côté client “server-sent dynamic registries”), ce qui justifie un design “caches + invalidation”. citeturn0search30turn0search24  
- **Gestion du cycle de vie** : lib runtime = gestion de suppression / nettoyage. DynamicDimensions prévient explicitement sur la persistance (ne sauvegarde pas l’historique des dimensions créées après restart) et sur les effets d’une création “overwrite level data”. Ça impose une politique produit : “les dimensions Portalcraft sont-elles permanentes ? supprimables ? recyclées ?”. citeturn9search2  

### Résultat attendu si tu appliques ce plan

Tu obtiens un Portalcraft où :

- les **cas spéciaux** (obsidienne→Nether, glowstone→îles flottantes, etc.) sont juste des règles data-driven  
- le **fallback** couvre tous les blocs (même moddés) via heuristiques + templates de terrain  
- les “mondes couleur” sont faisables via MapColor, et extensibles aux palettes multi-couleurs citeturn4search5turn4search0  
- le passage “biome only” (monobiome) est directement représentable via `fixed` biome_source citeturn3search7  
- les mondes “incohérents” peuvent basculer vers un mode “whacky/infinite” inspiré de l’esprit 20w14∞ (mais contrôlé par des policies configurables) citeturn10search0turn9search5  

Et surtout, tu construis une base où *la créativité est dans les données* (règles/templates) et *la robustesse est dans ton compilateur runtime* (dimension creation + persistance), ce qui est la combinaison la plus réaliste vu les contraintes de chargement des dimensions en vanilla. citeturn3search0turn9search2turn9search0