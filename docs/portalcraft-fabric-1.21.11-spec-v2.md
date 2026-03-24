# Portalcraft — Spécification technique du mod

**Plateforme cible** : Minecraft Java Edition **1.21.11**  
**Loader** : **Fabric**  
**Type de document** : spécification technique de base (MVP → V1)  
**Statut** : brouillon de travail

---

## 1. Résumé

**Portalcraft** est un mod Fabric dans lequel les joueurs construisent des **portails-algorithmes**.

Le principe central est le suivant :

- le joueur construit un cadre de portail de forme **Nether-like** ;
- le cadre est composé de **blocs cubiques autorisés** ;
- le joueur active le portail avec un **briquet** ;
- le mod analyse la composition du cadre ;
- cette composition devient une **signature procédurale** ;
- la signature détermine la **couleur du portail**, ses **paramètres**, puis la **dimension générée** ou la destination associée.

Le mod ne dépend pas techniquement de Blocodex, mais peut réutiliser sa réflexion taxonomique comme base de design pour classer les matériaux et orienter la génération.

---

## 2. Objectifs du mod

### 2.1 Objectif principal

Permettre aux joueurs de créer des portails personnalisés à partir de blocs du jeu, de façon intuitive, lisible et cohérente avec le langage visuel de Minecraft.

### 2.2 Objectifs techniques

- reproduire un comportement d’activation **proche du portail du Nether** ;
- permettre l’utilisation de **nombreux blocs cubiques** sans coder un portail spécifique pour chaque bloc ;
- faire reposer la logique sur une **détection de structure** et non sur un bloc unique spécial ;
- rendre le système **data-driven** autant que possible ;
- séparer clairement :
  - la validation du cadre,
  - l’activation,
  - la lecture des matériaux,
  - le calcul de signature,
  - la résolution de destination,
  - la génération dimensionnelle.

### 2.3 Objectifs de design

- garder une sensation **vanilla-compatible** ;
- éviter la dépendance dure à Blocodex ;
- permettre un futur équilibrage par familles de matériaux ;
- ouvrir la voie à des dimensions uniques, mais commencer par une base contrôlée.

---

## 3. Non-objectifs pour le MVP

Le MVP ne cherche pas à :

- supporter immédiatement tous les blocs vanilla et moddés ;
- générer une infinité de dimensions totalement uniques dès la première version ;
- intégrer Blocodex comme dépendance obligatoire ;
- résoudre immédiatement tous les cas spéciaux de compatibilité inter-mods ;
- ajouter une UI complexe de configuration en jeu.

---

## 4. Version cible et socle technique

Le document vise **Fabric pour Minecraft 1.21.11**. Fabric prend bien en charge cette version, et la documentation Fabric actuelle contient aussi des pages explicitement écrites pour 1.21.11. citeturn731918search0turn731918search17turn731918search19

Le projet reposera sur le socle standard suivant :

- **Fabric Loader** ;
- **Fabric Loom** pour l’environnement de dev ;
- **Fabric API** ;
- mappings Fabric/Yarn ou mappings officiels selon la stratégie du projet.

Fabric Loom est bien l’outil officiel de build/dev recommandé pour préparer un environnement de modding Fabric. citeturn731918search3turn731918search16

---

## 5. Philosophie d’architecture

Le mod doit être construit autour d’un pipeline simple :

1. **détection du cadre** ;
2. **validation de la structure** ;
3. **lecture du matériau** ;
4. **calcul de signature** ;
5. **résolution du type de portail** ;
6. **allumage du portail** ;
7. **transport inter-dimensionnel** ;
8. **génération/résolution de la dimension cible**.

Chaque étape doit être isolée.

Le mod ne doit pas mélanger dans une seule classe :

- la logique d’input,
- la logique de structure,
- la logique de rendu,
- la logique de dimension,
- la logique de taxonomie des blocs.

---

## 6. Boucle de gameplay visée

### 6.1 Boucle de base

1. Le joueur choisit des blocs de construction.
2. Il construit un cadre de portail rectangulaire.
3. Il utilise un briquet dans l’ouverture du cadre.
4. Le mod détecte si le cadre est valide.
5. Si le cadre est valide, le portail s’allume avec une apparence calculée à partir des matériaux.
6. Le joueur entre dans le portail.
7. Le mod l’envoie vers une destination correspondant à la signature du cadre.

### 6.2 Promesse système

Le portail n’est pas seulement une porte.  
Le portail est une **recette spatiale**.  
Sa composition encode une destination.

---

## 7. Portée du MVP

### 7.1 Ce que le MVP doit faire

- détecter un cadre de type portail du Nether ;
- accepter une **whitelist de blocs cubiques** ;
- activer le portail au **flint and steel** ;
- remplir l’intérieur avec un bloc de portail custom ;
- calculer une signature simple à partir des matériaux ;
- associer cette signature à une destination de test ;
- téléporter le joueur correctement ;
- rester stable en multijoueur.

### 7.2 Ce que le MVP peut simplifier

- dimensions de test limitées ;
- palette de couleurs simple ;
- pas de génération infinie complexe au début ;
- taxonomie de matériaux limitée à quelques familles.

---

## 8. Définition d’un portail valide

### 8.1 Forme

Un portail valide doit reprendre les contraintes générales du portail du Nether :

- cadre rectangulaire ;
- orientation verticale ;
- plan aligné sur X ou Z ;
- intérieur vide avant activation ;
- dimensions min/max fixées par le mod.

### 8.2 Règles structurelles minimales

Le détecteur doit vérifier :

- présence d’un contour fermé ;
- continuité du cadre ;
- absence de trous dans le cadre ;
- intérieur composé d’air ou d’états remplaçables autorisés ;
- blocs du cadre appartenant au tag autorisé.

### 8.3 Taille

Pour le MVP, recommander une plage compatible avec l’imaginaire du Nether :

- largeur interne minimale : **2**
- hauteur interne minimale : **3**
- largeur interne maximale : **21**
- hauteur interne maximale : **21**

Ces bornes peuvent être configurables plus tard.

---

## 9. Règle fondamentale : blocs de cadre autorisés

### 9.1 Principe

Le mod ne doit pas deviner naïvement “tout bloc visuellement cubique”.  
Il doit s’appuyer d’abord sur une **whitelist par tag**.

### 9.2 Tag principal

Créer un tag de blocs :

`portalcraft:valid_frame_blocks`

Ce tag contiendra les blocs autorisés comme matériaux de cadre.

### 9.3 Pourquoi les tags

Les tags sont l’outil adapté pour exprimer des familles de blocs et rendre le mod extensible via data packs ou compatibilité future. Fabric/vanilla reposent largement sur ce système de données pour grouper des contenus. citeturn731918search5

### 9.4 Première whitelist recommandée

Pour le MVP, commencer avec des blocs simples et stables, par exemple :

- stone
- cobblestone
- stone bricks
- deepslate
- cobbled deepslate
- sandstone
- red sandstone
- terracotta
- glazed terracotta seulement plus tard si désiré
- blackstone
- basalt seulement si validé visuellement
- quartz block
- purpur block
- prismarine
- dark prismarine
- bricks
- mud bricks
- packed mud
- bone block
- end stone bricks

### 9.5 Exclusions évidentes

Exclure dans le MVP :

- portes
- trappes
- barrières
- murets
- vitres et blocs de verre, sauf règle spéciale des portails colorés définie plus bas
- panes
- dalles
- escaliers
- blocs non pleins spéciaux
- blocs avec entité complexe si cela crée des effets indésirables
- blocs à gravité si cela casse la stabilité du portail

---

## 10. Activation du portail

### 10.1 Déclencheur

Le portail doit être activé à l’usage du **flint and steel**, comme pour le portail du Nether.

### 10.2 Séquence d’activation

Quand le joueur utilise le briquet :

1. récupérer la position ciblée ;
2. déterminer si la case visée ou adjacente peut être l’intérieur d’un portail ;
3. lancer la détection de cadre ;
4. si cadre valide, lire le matériau du cadre ;
5. calculer la signature ;
6. déterminer la variante de portail ;
7. placer les blocs de portail dans la zone interne ;
8. jouer effets sonores/visuels ;
9. enregistrer les métadonnées nécessaires.

### 10.3 Échec

Si le cadre est invalide, le briquet se comporte normalement ou échoue proprement sans allumer de portail.

---



## 10.4 Règle spéciale — portails colorés “April Fool”

### 10.4.1 Intention de design

Le mod introduit une règle spéciale dédiée aux **portails de couleur**.

Quand un cadre valide est construit **uniquement** à partir de :

- laine (`minecraft:*_wool`) ;
- verre plein (`minecraft:glass`, `minecraft:*_stained_glass`) ;

le portail ne suit pas la résolution dimensionnelle standard du MVP.
Il bascule vers la famille spéciale :

`portalcraft:april_fool_portals`

Cette famille représente des dimensions inspirées de l’esprit des dimensions du snapshot April Fools 2020 de Minecraft Java.

### 10.4.2 Règle stricte de matériaux

Pour entrer dans cette famille spéciale :

- tous les blocs du cadre doivent appartenir au tag `portalcraft:color_portal_frame_blocks` ;
- ce tag ne doit contenir que des **blocs cubiques pleins** de type laine et verre plein ;
- les panes, vitres minces, barrières, portes, trappes, dalles et escaliers restent exclus ;
- un seul bloc hors-règle dans le cadre annule la règle spéciale.

### 10.4.3 Tags recommandés

Créer :

- `portalcraft:color_portal_frame_blocks`
- `portalcraft:wool_portal_blocks`
- `portalcraft:glass_portal_blocks`
- `portalcraft:april_fool_portal_blocks`

Le tag `portalcraft:april_fool_portal_blocks` peut être la réunion logique de laine + verre plein.

### 10.4.4 Détection

La détection suit cet ordre :

1. valider la forme Nether-like ;
2. vérifier que tous les blocs du cadre sont dans `valid_frame_blocks` ;
3. vérifier ensuite si tous les blocs du cadre sont aussi dans `april_fool_portal_blocks` ;
4. si oui, appliquer la règle spéciale “color portal” ;
5. sinon, revenir à la logique normale de signature procédurale.

### 10.4.5 Logique de destination

Un portail coloré ne doit pas nécessairement reproduire à l’identique le snapshot 20w14∞.
Pour le MVP, il doit plutôt :

- pointer vers une **famille de dimensions spéciales** ;
- utiliser la couleur dominante ou le mélange de couleurs comme graine ;
- générer des variantes plus étranges, plus libres et plus surréalistes que les portails standards ;
- assumer explicitement une inspiration “April Fool” dans le ton visuel et procédural.

### 10.4.6 Signature de couleur

Pour ces portails, la signature procédurale doit privilégier :

- couleur dominante ;
- nombre de couleurs distinctes ;
- contraste laine / verre ;
- symétrie du cadre ;
- présence de verre clair versus verre teinté ;
- taille du portail.

Exemples de lecture :

- laine rouge + orange + jaune → monde absurde chaud / saturé ;
- laine bleue + cyan + verre clair → monde froid / irréel / lumineux ;
- multicolore très varié → monde chaotique, très “infinite snapshot” ;
- verre teinté monochrome → monde plus minimal, étrange, spectral.

### 10.4.7 Rendu du portail

Les portails colorés doivent recevoir un rendu distinct des portails standards :

- gradient ou oscillation de teinte ;
- particules plus vives ;
- sonorité plus rare / plus étrange ;
- possible bruit visuel ou shimmer particulier.

Le joueur doit comprendre immédiatement qu’il a ouvert une classe de portail exceptionnelle.

### 10.4.8 Règle produit

Les portails de laine et de verre deviennent donc une **voie spéciale vers les dimensions fantaisie / April Fool**.

Cela crée une hiérarchie claire :

- portails minéraux / naturels → dimensions cohérentes et classables ;
- portails colorés → dimensions spéciales, instables, ludiques, inspirées du snapshot infini.


## 11. Signature procédurale du portail

### 11.1 Rôle

La signature procédurale transforme une structure concrète en données exploitables.

Elle ne doit pas dépendre d’un identifiant Blocodex.  
Elle doit dépendre du **cadre réellement construit dans le monde**.

### 11.2 Entrées minimales

Pour le MVP, la signature peut prendre en compte :

- la liste des blocs du cadre ;
- la fréquence de chaque bloc ;
- le bloc dominant ;
- la taille du portail ;
- l’orientation ;
- éventuellement la répartition coins/bords.

### 11.3 Sorties minimales

La signature doit pouvoir fournir :

- une **couleur de portail** ;
- une **famille de portail** ;
- une **clé de destination** ;
- une **seed** stable pour la destination.

### 11.4 Exemples de logique de haut niveau

- majorité sandstone / terracotta chaude → portail sec/chaud ;
- majorité ice / snow → portail froid ;
- majorité prismarine / marine blocks → portail océanique ;
- majorité end stone / purpur → portail étrange / end-like ;
- majorité moss / mud / végétal → portail humide / luxuriant.

---

## 12. Taxonomie des matériaux

### 12.1 Positionnement

Blocodex peut inspirer la taxonomie, mais Portalcraft doit garder sa propre couche de classification.

### 12.2 Couche conceptuelle recommandée

Créer un système de **familles de matériaux** propre à Portalcraft.

Exemples :

- stone
- sand
- clay
- ice
- marine
- infernal
- end
- vegetal
- precious
- dark
- light
- ancient

### 12.3 Avantage

Cela permet :

- d’éviter de traiter chaque bloc comme un cas unique ;
- de stabiliser le design ;
- de rendre la génération plus cohérente ;
- d’ouvrir une compatibilité future avec des blocs moddés classés dans les mêmes familles.

---

## 13. Destination et dimensions

### 13.1 Stratégie MVP

Au début, une signature ne doit pas forcément créer une dimension entièrement nouvelle à chaque fois.

Le MVP peut utiliser une stratégie intermédiaire :

- plusieurs **familles de destinations** ;
- chaque famille pointe vers une dimension ou une variante de génération test ;
- la seed du portail affine cette destination.

### 13.2 Stratégies possibles

#### Option A — mapping simple

Une famille de matériaux → une destination fixe.

Avantage : très simple, robuste.  
Inconvénient : moins magique.

#### Option B — mapping + seed

Une famille de matériaux → un type de monde, puis la seed du portail modifie le monde.

Avantage : bon compromis pour V1.

#### Option C — dimension à la demande

Chaque portail génère sa propre dimension paramétrée.

Avantage : vision maximale.  
Inconvénient : plus coûteux, plus risqué pour le MVP.

### 13.3 Recommandation

Pour le MVP : **Option B**.

---

## 14. Génération procédurale

### 14.1 Ambition long terme

Le cadre du portail ne choisit pas seulement une destination, il influence les règles de génération.

### 14.2 Paramètres possibles

- biome dominant ;
- température ;
- humidité ;
- relief ;
- densité végétale ;
- présence de structures ;
- palette de blocs ;
- luminosité du ciel ;
- brouillard ;
- couleur ambiante.

### 14.3 MVP

Pour le MVP, limiter la génération à :

- quelques presets ;
- quelques palettes ;
- un lien simple entre matériaux et biome principal.

---

## 15. Bloc de portail custom

### 15.1 Rôle

Le mod doit définir son propre bloc de portail, analogue au bloc de portail du Nether.

### 15.2 Responsabilités

Le bloc de portail gère :

- le rendu visuel ;
- la collision spécifique ;
- la détection d’entité traversante ;
- l’accès aux métadonnées du portail ;
- les effets visuels/sonores ;
- la stabilité du portail dans le monde.

### 15.3 Données associées

Selon la stratégie choisie, il faudra soit :

- reconstruire les données du portail depuis le cadre ;
- soit stocker/attacher une structure de données associée à l’emplacement du portail.

Pour les données custom côté item et objets, la logique moderne de **Data Components** existe bien dans les versions récentes, y compris la doc Fabric 1.21.11. citeturn731918search17

---

## 16. Téléportation

### 16.1 Exigences

La téléportation doit :

- être stable côté serveur ;
- retrouver un point d’arrivée valide ;
- éviter les spawn piégés ;
- gérer correctement le retour ;
- gérer plusieurs joueurs.

### 16.2 Points de vigilance

- cooldown de portail ;
- boucle infinie de téléportation ;
- recherche d’une zone sûre ;
- création éventuelle d’un portail de sortie ;
- persistance correcte des liens si nécessaire.

---

## 17. Architecture logicielle recommandée

### 17.1 Packages suggérés

```text
com.portalcraft
├── PortalcraftMod.java
├── block
│   ├── PortalBlock.java
│   └── ModBlocks.java
├── item
│   └── ModItems.java
├── portal
│   ├── PortalFrameDetector.java
│   ├── PortalFrame.java
│   ├── PortalFrameValidator.java
│   ├── PortalActivationService.java
│   ├── PortalSignature.java
│   ├── PortalSignatureCalculator.java
│   ├── PortalFamily.java
│   ├── PortalResolver.java
│   ├── PortalDestinationKey.java
│   └── PortalRegistry.java
├── dimension
│   ├── PortalDimensionService.java
│   ├── PortalTeleportService.java
│   └── PortalSpawnLocator.java
├── taxonomy
│   ├── MaterialFamily.java
│   ├── BlockMaterialClassifier.java
│   └── MaterialRules.java
├── data
│   ├── PortalData.java
│   └── PortalDataStore.java
└── util
    ├── BlockUtil.java
    ├── HashUtil.java
    └── PortalDebug.java
```

### 17.2 Responsabilités

**PortalFrameDetector**  
Trouve un rectangle potentiel à partir d’un point d’activation.

**PortalFrameValidator**  
Vérifie que la structure est conforme.

**PortalSignatureCalculator**  
Convertit le cadre en signature procédurale.

**BlockMaterialClassifier**  
Associe les blocs à des familles de matériaux.

**PortalResolver**  
Associe une signature à une famille de portail et à une destination.

**PortalTeleportService**  
Gère la traversée inter-dimensionnelle.

**PortalDataStore**  
Conserve les données nécessaires sur les portails actifs.

---

## 18. Données et fichiers ressources

### 18.1 Tags

Créer au minimum :

- `portalcraft:valid_frame_blocks`
- `portalcraft:hot_materials`
- `portalcraft:cold_materials`
- `portalcraft:wet_materials`
- `portalcraft:end_materials`
- `portalcraft:dark_materials`
- `portalcraft:color_portal_frame_blocks`
- `portalcraft:wool_portal_blocks`
- `portalcraft:glass_portal_blocks`
- `portalcraft:april_fool_portal_blocks`

### 18.2 JSON de config/data

Prévoir à terme :

- un registre de familles de matériaux ;
- un registre de règles de portail ;
- un registre de destinations ;
- un registre de palettes visuelles.

### 18.3 Exemple conceptuel

```json
{
  "family": "hot",
  "blocks": [
    "minecraft:sandstone",
    "minecraft:red_sandstone",
    "minecraft:terracotta",
    "minecraft:orange_terracotta",
    "minecraft:yellow_terracotta"
  ],
  "portal_color": "amber",
  "destination": "portalcraft:hot_realm"
}
```

---

## 19. Compatibilité avec Blocodex

### 19.1 Règle

Blocodex ne doit pas être requis pour exécuter Portalcraft.

### 19.2 Usage recommandé

Blocodex peut servir :

- d’outil de réflexion taxonomique ;
- de catalogue de blocs ;
- de support de balancing ;
- de base pour concevoir les familles de matériaux.

### 19.3 Compatibilité douce future

Plus tard, Portalcraft pourrait :

- lire des catégories inspirées de Blocodex ;
- afficher des informations enrichies si Blocodex est installé ;
- partager des tags ou conventions ;
- proposer des intégrations facultatives.

---

## 20. Contraintes multijoueur

Le mod doit être pensé serveur d’abord.

### 20.1 Exigences

- validation côté serveur ;
- activation côté serveur ;
- téléportation côté serveur ;
- synchronisation visuelle correcte côté client ;
- gestion correcte des accès concurrents au même portail.

### 20.2 Cas à tester

- deux joueurs activent le même portail en même temps ;
- un joueur casse un bloc du cadre pendant l’activation ;
- un joueur traverse pendant qu’un autre modifie la structure ;
- destruction d’un portail actif ;
- retour depuis une dimension cible.

---

## 21. UX et feedback

### 21.1 Feedback minimal

Quand un portail s’allume, fournir :

- son d’activation ;
- particules ;
- teinte correspondant à la signature ;
- comportement visuel lisible.

### 21.2 Lisibilité

Le joueur doit comprendre intuitivement que :

- la forme compte ;
- le matériau compte ;
- le portail a une identité ;
- le portail ouvert est le résultat de sa construction.

---

## 22. Règles d’évolution du système

### 22.1 Progression de complexité recommandée

**Phase 1**  
Portails mono-matériau.

**Phase 2**  
Portails à matériau dominant + matériau secondaire.

**Phase 3**  
Coins spéciaux et variations de composition.

**Phase 4**  
Cadres libres plus expressifs.

### 22.2 Pourquoi

Cela évite de créer trop tôt un système impossible à équilibrer ou à déboguer.

---

## 23. Roadmap technique conseillée

### Étape 1 — fondation projet

- initialiser le projet Fabric 1.21.11 ;
- configurer Loom ;
- enregistrer les blocs/items de base ;
- préparer les tags.

### Étape 2 — détection du cadre

- détecteur de rectangle ;
- validation des dimensions ;
- vérification du tag `valid_frame_blocks`.

### Étape 3 — activation

- hook du flint and steel ;
- activation conditionnelle ;
- pose du bloc de portail.

### Étape 4 — signature

- lecture du cadre ;
- calcul matériau dominant ;
- calcul famille de portail ;
- calcul couleur.

### Étape 5 — destination test

- 3 à 5 destinations prototypes ;
- téléportation ;
- retour sécurisé.

### Étape 6 — stabilité serveur

- tests multi-joueurs ;
- destruction/réparation ;
- cooldown ;
- synchronisation.

### Étape 7 — extension procédurale

- règles de génération ;
- seeds ;
- presets avancés ;
- data-driven registry.

---

## 24. Risques principaux

### 24.1 Risques techniques

- détection de cadre trop permissive ;
- trop de cas spéciaux de blocs ;
- téléportation instable ;
- complexité excessive de génération ;
- persistance compliquée des portails.

### 24.2 Risques de design

- système trop opaque pour le joueur ;
- trop de matériaux donnant des résultats indiscernables ;
- trop de liberté trop tôt ;
- dépendance implicite à Blocodex.

### 24.3 Réponse

- commencer strict ;
- utiliser tags + familles ;
- limiter les destinations au début ;
- renforcer le feedback visuel.

---

## 25. Décisions de spec pour le MVP

### Décision 1

Le portail est une **structure détectée**, pas un bloc spécial placé à la main.

### Décision 2

Le portail s’active au **briquet**.

### Décision 3

Les blocs du cadre doivent appartenir à une **whitelist par tag**.

### Décision 4

Blocodex n’est **pas une dépendance obligatoire**.

### Décision 5

Le système repose sur une **signature procédurale** dérivée du cadre.

### Décision 6

Le MVP utilise une logique **famille de matériaux → destination/preset + seed**.

### Décision 7

Les cadres composés uniquement de laine et/ou de verre plein activent une famille spéciale de **portails colorés** menant à des dimensions inspirées du snapshot April Fools.

### Décision 8

L’architecture doit rester **modulaire**, lisible, et orientée serveur.

---

## 26. Annexes

### 26.1 Exemple de tag

`data/portalcraft/tags/blocks/valid_frame_blocks.json`

```json
{
  "replace": false,
  "values": [
    "minecraft:stone",
    "minecraft:cobblestone",
    "minecraft:stone_bricks",
    "minecraft:sandstone",
    "minecraft:red_sandstone",
    "minecraft:terracotta",
    "minecraft:deepslate",
    "minecraft:cobbled_deepslate",
    "minecraft:prismarine",
    "minecraft:quartz_block",
    "minecraft:bricks",
    "minecraft:mud_bricks",
    "minecraft:packed_mud",
    "minecraft:bone_block",
    "minecraft:purpur_block",
    "minecraft:end_stone_bricks"
  ]
}
```

### 26.2 Exemple de pseudo-flux

```text
Player uses FlintAndSteel
→ detect candidate inner cell
→ detect rectangular frame
→ validate frame blocks via tag
→ build frame model
→ classify materials
→ compute portal signature
→ resolve portal family and destination
→ place portal blocks
→ enter portal
→ teleport to resolved target
```

### 26.3 Position de spec

Le mod doit être pensé comme un système de **construction algorithmique de dimensions**, avec un premier noyau simple, robuste, lisible et extensible.

---

## 27. Conclusion

Portalcraft doit démarrer avec une base très solide :

- un vrai portail Nether-like,
- une validation stricte,
- une whitelist claire,
- une signature procédurale simple,
- quelques destinations prototypes,
- une architecture propre.

C’est cette base qui permettra ensuite d’aller vers la vraie vision du projet :  
**faire de chaque portail une formule de monde**.
