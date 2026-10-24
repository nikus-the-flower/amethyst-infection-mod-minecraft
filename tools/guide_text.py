"""Text of the Amethyst Compendium (guide book): id -> (title_en, title_ru, text_en, text_ru)."""

CATEGORIES = {
    "world": ("The World", "Мир"),
    "creatures": ("Creatures", "Твари"),
    "bosses": ("Bosses", "Боссы"),
    "weapons": ("Weapons", "Оружие"),
    "armor": ("Armor", "Броня"),
    "artifacts": ("Artifacts", "Артефакты"),
}

UI = {
    "subtitle": ("Everything about the Amethyst", "Всё об Аметисте"),
    "back": ("Back", "Назад"),
    "recipe": ("Recipe", "Рецепт"),
    "nextrecipe": ("Next recipe", "Др. рецепт"),
}

GUIDE = {
    "world": (
        "The Infection", "Заражение",
        "The Amethyst infection grows from cores: crystals scattered over the world, more and older the further you go from the tower. Around each core its land turns violet: grass, dirt, sand and trees first; older cores take bare stone too. Crystals sprout and grow on it: a bud becomes a crystal, then a large crystal. The land of a core keeps growing while the core lives; destroy the core and its land stops growing, but stays infected. The tower at 0, 0 stands in its own land, infected from the start. Only the fall of the Heart of the Amethyst makes the infection fade, very slowly. Speed: /gamerule amethystInfectionSpeed 0-1000 (0 stops it). The infection only works near players and with a fixed budget, so it never slows the game. Purified armor, the Purifier's Blade and wards burn the infection away.",
        "Аметистовое заражение растёт из ядер: кристаллов, разбросанных по миру; чем дальше от башни, тем их больше и тем они старше. Вокруг каждого ядра земля становится фиолетовой: сначала трава, земля, песок и деревья, у старых ядер и голый камень. На ней прорастают и растут кристаллы: бутон становится кристаллом, затем крупным кристаллом. Земля ядра растёт, пока ядро живо; уничтожьте ядро, и его земля перестанет расти, но останется заражённой. Башня на 0, 0 стоит на собственной земле, заражённой с самого начала. Только гибель Сердца Аметиста заставит заражение отступить, очень медленно. Скорость: /gamerule amethystInfectionSpeed 0-1000 (0 останавливает). Заражение работает только рядом с игроками и в строгих рамках, поэтому не тормозит игру. Заражение выжигают броня очищения, Клинок очищения и обереги."),
    "cores": (
        "Infection Cores", "Ядра заражения",
        "A core grows through three phases with age (6 and 15 in-game days by default). Phase 1: a lone crystal about a block tall; one or two blows break it; its land is soil, sand and trees. Phase 2: two blocks tall; it calls weak creatures of the Amethyst around itself, its land grows faster, takes stone, raises crystal pikes and turns into the Crystal Wastes. Phase 3: a three-block monolith that shoots shards and raises crystals under your feet, calls dangerous creatures and hides behind a barrier that only purified amethyst can pierce: purified weapons and armor pieces held in hand, the purified spear and its light. The barrier grows back if you stop. From phase 2 on, cores throw crystal geodes now and then: each lands somewhere free and becomes a new core of phase 1. After the Heart's fall every core fades away within a few days. Ops: /amethyst core list, spawn <phase>, upgrade [all|radius <r>], set <phase>, kill [all|radius <r>], geode <phase>, throw, stats; /gamerule amethystCorePhase2Days and amethystCorePhase3Days.",
        "Ядро растёт с возрастом через три фазы (по умолчанию 6 и 15 игровых дней). Фаза 1: одинокий кристалл высотой около блока, ломается с одного-двух ударов; его земля — земля, песок и деревья. Фаза 2: высотой в два блока; призывает вокруг слабых тварей Аметиста, его земля растёт быстрее, захватывает камень, на ней поднимаются кристальные пики, и она становится Кристальными пустошами. Фаза 3: трёхблочный монолит, стреляет осколками и выращивает кристаллы у вас под ногами, призывает опасных тварей и прячется за барьером, который пробивает только очищенный аметист: очищенное оружие и части очищенной брони в руке, очищенное копьё и его свет. Если остановиться, барьер восстанавливается. С фазы 2 ядра время от времени бросают кристальные жеоды: каждая падает на свободное место и становится новым ядром фазы 1. После гибели Сердца все ядра угасают за несколько дней. Операторы: /amethyst core list, spawn <фаза>, upgrade [all|radius <r>], set <фаза>, kill [all|radius <r>], geode <фаза>, throw, stats; /gamerule amethystCorePhase2Days и amethystCorePhase3Days."),
    "blocks": (
        "Crystals and Blocks", "Кристаллы и блоки",
        "Crystals come in violet, rose and azure, and three sizes. Break them for amethyst essence. From essence you craft: the Amethyst Crystal Block (4 essence), Amethyst Bricks (4 blocks make 4 bricks), Brilliant Amethyst (a crystal block with glowstone dust, light 15), Clear Amethyst (smelt infected sand), Clear Amethyst Panes (6 clear blocks make 16) and Brick Slabs (3 bricks make 6).",
        "Кристаллы бывают фиолетовые, розовые и лазурные, трёх размеров. Разбейте их, чтобы получить аметистовую эссенцию. Из эссенции делают: блок аметистового кристалла (4 эссенции), аметистовые кирпичи (4 блока дают 4 кирпича), сияющий аметист (блок кристалла и светящаяся пыль, свет 15), прозрачный аметист (переплавьте заражённый песок), панели из него (6 блоков дают 16) и плиты из кирпичей (3 кирпича дают 6)."),
    "materials": (
        "Crystals of Power", "Кристаллы силы",
        "Flame Crystal: amethyst essence ringed by four blaze rods. Frost Crystal: essence ringed by eight ice blocks. Purified Amethyst: four essence, a diamond and a nether quartz make two. The flame and frost crystals power the staffs, the berserker core and the stasis crystal; purified amethyst makes the Purifier's armor.",
        "Кристалл огня: эссенция в окружении четырёх стержней ифрита. Кристалл холода: эссенция в окружении восьми блоков льда. Очищенный аметист: четыре эссенции, алмаз и кварц дают два. Кристаллы огня и холода питают посохи, ядро берсерка и кристалл стазиса; из очищенного аметиста делают броню очищения."),
    "tower": (
        'The Amethyst Tower', 'Аметистовая башня',
        'At world coordinates 0, 0 stands the Amethyst Tower, a giant amethyst cluster: an eight-sided crystal shaft with huge crystals bursting out of the ground and its walls, crowned by a crystal point. The door faces south. Five floors are linked by a spiral stair of half-steps along the wall, with one guard on each: stalkers (1), a lurker (2), a shard-walker (3), weavers (4), a geode colossus (5). Every floor has a chest, better ones higher up; the purity amulet can turn up in them. At the top, in a hall of crystal pillars, waits Amitariel. Ops: /amethyst tower build rebuilds the tower, /amethyst tower tp takes you to its door.',
        'На координатах 0, 0 стоит Аметистовая башня, огромный аметистовый кластер: восьмигранный кристальный ствол, из земли и стен которого вырываются гигантские кристаллы, а наверху кристальный шпиль. Дверь смотрит на юг. Пять этажей соединяет винтовая лестница из полуступеней вдоль стены, на каждом по одному стражу: сталкеры (1), лурк (2), осколочный ходок (3), ткачи (4), геодовый колосс (5). На каждом этаже сундук, чем выше, тем лучше; в них может найтись амулет чистоты. Наверху, в зале с кристальными колоннами, ждёт Амитариэль. Операторы: /amethyst tower build перестраивает башню, /amethyst tower tp переносит к её двери.'),
    "resonant_shard": (
        'Resonant Shard', 'Резонирующий осколок',
        'A shard that still hums with the song of the Amethyst. Any creature of the Amethyst can drop one: stalkers and weavers rarely, lurkers and walkers more often, a geode colossus always (2-3), Amitariel 3-5. Tower chests hold them too. Good gear needs them: every piece of amethyst and purified armor, the crystal hammer and the amethyst shield.',
        'Осколок, в котором ещё звучит песнь Аметиста. Его может уронить любая тварь Аметиста: сталкеры и ткачи редко, лурки и ходоки чаще, геодовый колосс всегда (2-3), Амитариэль 3-5. Попадается и в сундуках башни. Он нужен для хорошего снаряжения: каждой части аметистовой брони и брони очищения, кристального молота и аметистового щита.'),
    "food": (
        'Food', 'Еда',
        'Purified Crystal Apple (an apple ringed by four purified amethyst): Regeneration II for 10 seconds and Absorption for 2 minutes, cures the blight, can be eaten when full. Purified Crystal Bread (three wheat and a purified amethyst): very filling, a little regeneration, cures the blight. Country Potatoes with Sauce (three baked potatoes, a beetroot and a bowl): Strength and Resistance for 2 minutes. Mmm, what tasty potatoes! Cereal with Milk: the best breakfast there is, with speed, regeneration, haste and high jumps for a minute; it cannot be crafted, it is sold by a traveller you may meet one day.',
        'Очищенное кристальное яблоко (яблоко в окружении четырёх очищенных аметистов): Регенерация II на 10 секунд и Поглощение на 2 минуты, лечит порчу, можно есть на сытый желудок. Очищенный кристальный хлеб (три пшеницы и очищенный аметист): очень сытный, немного регенерации, лечит порчу. Картошка по-деревенски с соусом (три печёные картофелины, свёкла и миска): Сила и Сопротивление на 2 минуты. Ммм, какая вкусная картошка! Хлопья с молоком: лучший завтрак на свете, скорость, регенерация, спешка и высокие прыжки на минуту; скрафтить нельзя, их продаёт путник, которого вы однажды встретите.'),
    "commands": (
        "Tips and Commands", "Советы и команды",
        "Artifacts work from any inventory slot; right-click them to use. Staffs cut off after a few seconds and need to cool down. Armor sets give a bonus only when all four pieces are worn. Commands: /gamerule amethystInfectionSpeed 0-1000, /amethyst tower build, /amethyst tower tp, /amethyst core (see Infection Cores). Spawn eggs of every creature are in the creative tab.",
        "Артефакты работают из любого слота инвентаря, нажмите ПКМ, чтобы применить. Посохи отключаются через несколько секунд и перезаряжаются. Бонусы брони действуют только при всех четырёх частях. Команды: /gamerule amethystInfectionSpeed 0-1000, /amethyst tower build, /amethyst tower tp, /amethyst core (см. «Ядра заражения»). Яйца призыва всех тварей лежат в творческой вкладке."),

    "stalker": (
        "Amethyst Stalker", "Аметистовый сталкер",
        "A crystal wolf. 34 health, bites for 7 and pounces. Its bite spreads the amethyst blight. Spawns only on infected ground, on the surface and underground, and in the first floor of the tower. Drops 0-2 amethyst essence.",
        "Кристальный волк. 34 здоровья, кусает на 7 и прыгает на жертву. Укус вызывает аметистовую порчу. Появляется только на заражённой земле, на поверхности и под землёй, а также на первом этаже башни. Роняет 0-2 эссенции."),
    "weaver": (
        "Crystal Weaver", "Кристальный ткач",
        "A crystal spider that climbs walls and ceilings. 26 health, bites for 5 and spreads the blight. Lives on infected ground and on the first floor of the tower. Drops essence and string.",
        "Кристальный паук, лазает по стенам и потолкам. 26 здоровья, кусает на 5 и вызывает порчу. Живёт на заражённой земле и на первом этаже башни. Роняет эссенцию и нити."),
    "walker": (
        "Shard-Walker", "Осколочный ходок",
        "A tall humanoid of the tower. 80 health, heavy blows of 11. From range it throws a fan of three crystal shards, then closes in. Found on floors 3 and 6. Drops 2-4 essence and sometimes purified amethyst.",
        "Высокий гуманоид башни. 80 здоровья, тяжёлые удары по 11. Издалека бросает веер из трёх кристальных осколков, потом подходит вплотную. Встречается на этажах 3 и 6. Роняет 2-4 эссенции и иногда очищенный аметист."),
    "lurker": (
        "Void-Crystal Lurker", "Кристальный лурк пустоты",
        "A fast raptor with sickle claws. 46 health, bites for 9 and leaps onto its prey from a distance. Found on floors 2, 4 and 7. Drops 1-2 essence.",
        "Быстрый раптор с серповидными когтями. 46 здоровья, кусает на 9 и прыгает на жертву издалека. Встречается на этажах 2, 4 и 7. Роняет 1-2 эссенции."),
    "colossus": (
        "Geode Colossus", "Колосс-геода",
        "A huge, slow golem you cannot knock back. 240 health, hits for 22. When you come close it raises its arms and slams the ground: a shockwave of radius 9 for 16 damage that throws you into the air. Found on floors 5 and 7. Drops 6-12 essence and sometimes flame or frost crystals.",
        "Огромный медленный голем, которого не отбросить. 240 здоровья, бьёт на 22. Когда вы рядом, он поднимает руки и бьёт по земле: ударная волна радиусом 9, 16 урона, подбрасывает вас в воздух. Встречается на этажах 5 и 7. Роняет 6-12 эссенции и иногда кристаллы огня или холода."),
    "drifter": (
        "Celestial Drifter", "Небесный дрейфер",
        "A winged creature with tentacles that floats above you at a distance. 100 health. It fires volleys of five crystal shards, so close in fast or use a bow. Found on floors 4 and 6. Drops 3-6 essence and sometimes a frost crystal.",
        "Крылатое существо с щупальцами, парит над вами на расстоянии. 100 здоровья. Выпускает залпы из пяти кристальных осколков, так что сближайтесь быстро или берите лук. Встречается на этажах 4 и 6. Роняет 3-6 эссенции и иногда кристалл холода."),

    "amitariel": (
        "Amitariel", "Амитариэль",
        "The apostle of the Amethyst waits at the top of the tower. He speaks first and offers you to join the Amethyst: answer \"yes\" in chat and you die, answer \"no\" (or stay silent) and the fight begins. Phase 1: lunges, shard barrages, crystal pillars under every player, guards at low health. Below half health he transforms and adds a shard storm and blink strikes. 450 health, more with more players. He whirls with his spear (step out of the circle), leaps and smashes the floor where you stood (run from the rune circle, crystals grow there), and summons spears of light that aim at you one by one (a thin line shows each one's path: step aside). When he falls, he drops his spear and a rift into the world of the Amethyst tears open in his hall.",
        "Апостол Аметиста ждёт на вершине башни. Сначала он говорит и предлагает вам присоединиться к Аметисту: напишите в чат «да», и вы умрёте, напишите «нет» (или промолчите), и начнётся бой. Фаза 1: рывки, залпы осколков, кристальные столбы под каждым игроком, стражи при низком здоровье. Ниже половины здоровья он превращается и добавляет град осколков и удары из телепорта. 450 здоровья, больше при нескольких игроках. Он вращается с копьём (выйдите из круга), прыгает и бьёт копьём в пол там, где вы стояли (убегайте из рунного круга, там вырастут кристаллы), и призывает копья света, которые по одному нацеливаются на вас (тонкая линия показывает путь каждого: отойдите в сторону). Когда он падёт, он уронит своё копьё, а в его зале разорвётся разлом в мир Аметиста."),
    "heart": (
        "The Amethyst Heart", "Сердце Аметиста",
        'The final boss, a living crystal heart over ten blocks across, in the temple at the centre of the Amethyst world. The fight has five stages. 1: five crystals around the arena shield it and shoot at you: break them all. Then four phases, each with its own health bar, larger every time: empty one and the next begins. Each phase exposes new weak points and closes the old ones. 2 Awakening: the nodes at its roots. 3 Resonance: the lower armour plates break off, strike the glowing wounds on its chest and back. 4 Crystallization: the armour falls away, strike the crystal veins under its belly. 5 Agony: no defence left, strike it anywhere; below 8% a 30 second countdown to a blast over the whole arena. Weak points are sealed and open only for a few seconds after each attack, then they take double damage; the rest of the body takes little. Watch the rune circles: they show where crystals will burst or fall. Its attacks include crystal spirals, crystals that chase you (keep running), zones of growing and falling crystals, a pentagon of giant crystals, glowing rings and floor beams (jump over them) and the death beam: a thin line aims at someone, then fires (step aside!). Later phases drop the weak attacks and combine several at once. If everyone dies or leaves, it heals and the shield returns. Victory tears open a rift home and stops the infection in the normal world.',
        'Финальный босс, живое кристальное сердце больше десяти блоков в ширину, в храме в центре мира Аметиста. Бой идёт в пять этапов. 1: пять кристаллов вокруг арены защищают его и стреляют в вас: разбейте их все. Затем четыре фазы, у каждой своя полоска здоровья, и каждая больше прежней: опустошите одну, и начнётся следующая. Каждая фаза открывает новые слабые точки и закрывает старые. 2 Пробуждение: узлы у корней. 3 Резонанс: нижние пластины брони отваливаются, бейте светящиеся раны на груди и спине. 4 Кристаллизация: броня спадает, бейте кристальные жилы под брюхом. 5 Агония: защиты больше нет, бейте куда угодно; ниже 8% начнётся отсчёт 30 секунд до взрыва на всю арену. Слабые точки закрыты и открываются лишь на несколько секунд после каждой атаки, тогда они получают двойной урон; остальное тело почти не ранится. Следите за рунными кругами: они показывают, где вырвутся или упадут кристаллы. Среди атак: кристальные спирали, кристаллы, преследующие вас (не останавливайтесь), зоны растущих и падающих кристаллов, пентаграмма гигантских кристаллов, светящиеся кольца и лучи по полу (перепрыгивайте) и луч смерти: тонкая линия наводится на кого-то, затем стреляет (отойдите в сторону!). В поздних фазах слабые атаки исчезают, а несколько атак сливаются в одну. Если все погибнут или уйдут, Сердце исцелится и щит вернётся. Победа откроет разлом домой и остановит заражение в обычном мире.'),
    "portal": (
        'The Rift', 'Разлом',
        "When Amitariel falls, space tears open in the middle of his hall: a tall rift of light. Stand in it for a moment and it carries you into the world of the Amethyst; you land somewhere 150-500 blocks from its temple. The way back is a second rift, which opens in the temple when the Heart falls and leads back to Amitariel's hall.",
        'Когда Амитариэль падёт, в середине его зала разрывается пространство: высокий разлом света. Постойте в нём мгновение, и он перенесёт вас в мир Аметиста, в 150-500 блоках от его храма. Обратный путь — второй разлом, который откроется в храме, когда падёт Сердце, и приведёт в зал Амитариэля.'),
    "amethyst_world": (
        "The World of the Amethyst", "Мир Аметиста",
        "Eternal violet twilight under the stars. Islands of crystal float over the void: crystal groves with giant crystals, deep amethyst canyons, quiet meadows, clear amethyst lakes and the shard wastes, broken into small floating islands. Sometimes crystal rain falls. Only creatures of the Amethyst live here, and they do not need darkness. Beds work: set your spawn before the fight.",
        "Вечные фиолетовые сумерки под звёздами. Над пустотой парят кристальные острова: кристальные рощи с огромными кристаллами, глубокие аметистовые каньоны, тихие луга, озёра прозрачного аметиста и осколочные пустоши из мелких летающих островов. Иногда идёт кристальный дождь. Здесь живут только твари Аметиста, и им не нужна темнота. Кровати работают: поставьте точку возрождения перед боем."),
    "temple": (
        "The Temple of the Amethyst", "Храм Аметиста",
        "At the centre of the world (0, 0) a cathedral under a crystal dome floats on its own island. Inside: a round arena with columns along the edge to hide behind, five crystal pedestals and, in the middle, the Heart. When the Heart falls, a rift home opens in the middle of the arena.",
        "В центре мира (0, 0) на собственном острове стоит собор под кристальным куполом. Внутри круглая арена с колоннами по краю, за которыми можно укрыться, пять кристальных пьедесталов и в центре Сердце. Когда Сердце падёт, посреди арены откроется разлом домой."),

    "amethyst_tools": (
        "Amethyst Tools", "Аметистовые инструменты",
        "Pickaxe, axe, shovel and hoe of amethyst: mining level above iron, 2200 durability, high enchantability. Repaired with amethyst essence.",
        "Кирка, топор, лопата и мотыга из аметиста: уровень выше железа, 2200 прочности, высокая зачаровываемость. Чинятся аметистовой эссенцией."),
    "amethyst_sword": (
        "Amethyst Blade", "Аметистовый клинок",
        "35% of blows burst into crystal shards that hit everything around the target. Right-click: a crystal dash through enemies in a line (6 seconds cooldown).",
        "35% ударов взрываются осколками, которые ранят всех рядом с целью. ПКМ: кристальный рывок сквозь врагов (перезарядка 6 секунд)."),
    "flame_brand": (
        "Flame Brand", "Пламенный клинок",
        "Every blow sets the target ablaze. Right-click: a wide burst of fire that burns everything in front of you for 9 damage (8 seconds cooldown).",
        "Каждый удар поджигает цель. ПКМ: широкая вспышка огня, сжигающая всё перед вами на 9 урона (перезарядка 8 секунд)."),
    "frost_edge": (
        "Frost Edge", "Ледяной клинок",
        "Blows slow and chill the target. Right-click: a nova of cold around you that damages, slows and weakens everything within 6 blocks (11 seconds cooldown).",
        "Удары замедляют и охлаждают цель. ПКМ: волна холода вокруг вас, которая ранит, замедляет и ослабляет всех в радиусе 6 блоков (перезарядка 11 секунд)."),
    "storm_blade": (
        "Storm Blade", "Громовой клинок",
        "30% of blows call down lightning on the target. Right-click: chain lightning that leaps from enemy to enemy, up to six of them, for 9 damage each (10 seconds cooldown).",
        "30% ударов вызывают молнию на цель. ПКМ: цепная молния перескакивает от врага к врагу, до шести целей, по 9 урона (перезарядка 10 секунд)."),
    "void_edge": (
        "Void Edge", "Клинок пустоты",
        "Blows weaken the target and make it glow, and heal you a little. Right-click: slip through space up to 10 blocks forward, cutting everything on the way for 11 damage (5 seconds cooldown).",
        "Удары ослабляют цель, подсвечивают её и немного лечат вас. ПКМ: шаг сквозь пространство на 10 блоков вперёд, режет всё на пути на 11 урона (перезарядка 5 секунд)."),
    "purifier_blade": (
        "Purifier's Blade", "Клинок очищения",
        "Deals 10 extra damage to creatures of the Amethyst and cleanses your blight. Right-click: a flash that burns the infection from the ground around you, scorches creatures of the Amethyst within 8 blocks for 18 and heals you (7 seconds cooldown).",
        "Наносит 10 дополнительного урона тварям Аметиста и снимает с вас порчу. ПКМ: вспышка выжигает заражение вокруг, обжигает тварей Аметиста в радиусе 8 блоков на 18 и лечит вас (перезарядка 7 секунд)."),
    "flame_staff": (
        "Flame Staff", "Посох пламени",
        "Hold right-click for a flamethrower that sets everything in a cone on fire. It cuts off after 4 seconds and cools down, and enemies are not pushed back.",
        "Зажмите ПКМ: огнемёт поджигает всё в конусе перед вами. Отключается через 4 секунды и перезаряжается, врагов не отбрасывает."),
    "frost_staff": (
        "Frost Staff", "Ледяной посох",
        "Right-click: a wave of cold that freezes and slows enemies in a cone and turns water to ice. 5 seconds cooldown.",
        "ПКМ: волна холода замораживает и замедляет врагов в конусе и превращает воду в лёд. Перезарядка 5 секунд."),
    "prism_scepter": (
        "Prism Scepter", "Призматический скипетр",
        "Hold right-click for a piercing beam of crystal light that hits everything on its line. It cuts off after 3 seconds and cools down.",
        "Зажмите ПКМ: пронзающий луч кристального света бьёт всё на своей линии. Отключается через 3 секунды и перезаряжается."),
    "crescent_blade": (
        'Crescent Blade', 'Клинок полумесяцев',
        'A curved crystal saber. Hold right-click and let go: crescents of light fly from the blade, pass through enemies (each is cut once) and break on walls. Each half second of holding adds one (up to 5 at three seconds), and they grow bigger and hit harder (6 to 12 damage each). The cooldown grows with the charge. Made from an amethyst sword, four purified amethyst and two resonant shards.',
        'Изогнутая кристальная сабля. Зажмите ПКМ и отпустите: из клинка вылетают полумесяцы света, пролетают сквозь врагов (каждого режут один раз) и разбиваются о стены. Каждые полсекунды удержания добавляют один (до 5 за три секунды), и они растут и бьют сильнее (от 6 до 12 урона каждый). Перезарядка растёт с зарядом. Делается из аметистового меча, четырёх очищенных аметистов и двух резонирующих осколков.'),
    "purified_spear": (
        'Purified Crystal Spear', 'Очищенное кристальное копьё',
        "Amitariel's spear, made pure: craft it from his spear, four purified amethyst and a resonant shard. Hold right-click: a spear of white light appears over you about three times a second and flies to the point you were looking at when it appeared; turn to aim the next ones, the ones already flying keep their course (6 damage each). Keep holding for ten seconds and it bursts into a volley: 18 spears fire one after another, like a machine gun, at whatever you look at (7 damage each). After the volley it rests for 15 seconds.",
        'Копьё Амитариэля, ставшее чистым: делается из его копья, четырёх очищенных аметистов и резонирующего осколка. Зажмите ПКМ: примерно три раза в секунду над вами появляется копьё белого света и летит в точку, куда вы смотрели в момент его появления; поворачивайтесь, чтобы навести следующие, уже летящие не меняют курс (6 урона каждое). Держите десять секунд, и начнётся шквал: 18 копий одно за другим, как пулемёт, бьют туда, куда вы смотрите (7 урона каждое). После шквала копьё отдыхает 15 секунд.'),
    "crystal_hammer": (
        "Crystal Hammer", "Кристальный молот",
        "A heavy hammer. Hold right-click: you leap up and hang in the air while the blow charges for 3 seconds, then dive into the ground. Enemies within 7 blocks take up to 18 damage and are hurled away, crystals burst around you, and every enemy hit heals you (4 health plus 2 per enemy). Let go early and you just drop. No fall damage from the dive. Cooldown 8 seconds.",
        "Тяжёлый молот. Зажмите ПКМ: вы подпрыгиваете и зависаете в воздухе, пока 3 секунды заряжается удар, затем обрушиваетесь на землю. Враги в радиусе 7 блоков получают до 18 урона и отлетают, вокруг вырываются кристаллы, а каждый задетый враг лечит вас (4 здоровья и ещё 2 за врага). Отпустите раньше, и вы просто упадёте. Урона от падения нет. Перезарядка 8 секунд."),
    "amitariel_spear": (
        'Spear of Amitariel', 'Копьё Амитариэля',
        "The apostle's own spear, which he drops when he falls. A strong melee weapon. Tap right-click: you whirl for 3 seconds with the spear held out, cutting everything around you every half second (7 damage). Hold right-click a moment (until it chimes) and let go: you dash forward spinning, and the dash ends in a sweep that deals 8 damage around you and throws enemies back. No fall damage during the dash. Cooldowns: 6 seconds after the whirl, 3.5 after the dash.",
        'Собственное копьё апостола, которое он роняет, когда падает. Сильное оружие ближнего боя. Нажмите ПКМ: 3 секунды вы вращаетесь, выставив копьё, и каждые полсекунды рубите всё вокруг (7 урона). Зажмите ПКМ на мгновение (пока не прозвенит) и отпустите: вы с вращением делаете рывок вперёд, а в конце рывка удар по кругу наносит 8 урона и отбрасывает врагов. Во время рывка нет урона от падения. Перезарядка: 6 секунд после вихря, 3,5 после рывка.'),
    "talis_hammer": (
        "Jaybee Talis's Hammer", "Молот Джейби Талиса",
        "Hold right-click to charge for up to 5 seconds: the hammer crackles with lightning. Release to smash the floor: thunder strikes, everything around takes electric damage and is hurled away. A full charge hits a radius of 12 blocks for up to 40 damage. Cooldown grows with the charge.",
        "Зажмите ПКМ, чтобы зарядить молот до 5 секунд: он трещит молниями. Отпустите, и молот ударит о пол: грянет гром, всё вокруг получит электрический урон и отлетит. Полный заряд бьёт в радиусе 12 блоков до 40 урона. Перезарядка растёт с зарядом."),
    "crystal_bow": (
        "Crystal Bow and Arrows", "Кристальный лук и стрелы",
        "A bow of crystal and string, 900 durability. Crystal arrows shatter on impact: everything within 3 blocks of the target takes 5 damage and glows. Craft four arrows from essence, a stick and a feather. Normal arrows work too.",
        "Лук из кристалла и нитей, 900 прочности. Кристальные стрелы разбиваются при попадании: всё в радиусе 3 блоков от цели получает 5 урона и подсвечивается. Четыре стрелы делаются из эссенции, палки и пера. Обычные стрелы тоже подходят."),
    "amethyst_shield": (
        "Amethyst Shield", "Аметистовый щит",
        "Blocks like a normal shield, and whatever it stops takes 5 damage and is pushed back by a crystal burst. 750 durability, repaired with essence.",
        "Блокирует как обычный щит, а тот, чей удар он остановил, получает 5 урона и отлетает от кристального взрыва. 750 прочности, чинится эссенцией."),

    "amethyst_armor": (
        "Amethyst Armor", "Аметистовая броня",
        "Strong armor of crystal with crystal crests, shoulder spikes and a glowing back. The full set grants Crystal Resonance: more damage, faster strikes and extra speed. Each piece is made of amethyst crystal blocks with a resonant shard.",
        "Прочная кристальная броня с гребнями, шипами на плечах и светящейся спиной. Полный комплект даёт Кристальный резонанс: больше урона, быстрее удары и выше скорость. Каждая часть делается из блоков аметистового кристалла и резонирующего осколка."),
    "purified_armor": (
        "Purifier's Armor", "Броня очищения",
        "Made of purified amethyst, with a halo and wings of light. Each piece takes 15% off the damage from creatures of the Amethyst. The full set grants the Anti-Amethyst Ward: 80% less damage from them, immunity to the blight, healing on infected ground and a cleansing aura that burns the infection away. Each piece is made of purified amethyst with a resonant shard.",
        "Сделана из очищенного аметиста, с нимбом и крыльями света. Каждая часть снижает урон от тварей Аметиста на 15%. Полный комплект даёт Антиаметистовую защиту: на 80% меньше урона от них, иммунитет к порче, лечение на заражённой земле и очищающую ауру, выжигающую заражение. Каждая часть делается из очищенного аметиста и резонирующего осколка."),

    "aegis_heart": (
        "Aegis Heart", "Сердце эгиды",
        "In the inventory: constant resistance. Right-click: a thick crystal shell of absorption and resistance for 15-20 seconds. Cooldown 90 seconds.",
        "В инвентаре: постоянное сопротивление урону. ПКМ: плотный кристальный щит с поглощением и защитой на 15-20 секунд. Перезарядка 90 секунд."),
    "amethyst_wings": (
        "Amethyst Wings", "Аметистовые крылья",
        "Right-click: you are thrown up and can fly freely for 25 seconds, leaving a trail of sparks. Slow falling keeps you safe afterward. Cooldown 60 seconds.",
        "ПКМ: вас подбрасывает, и 25 секунд вы летите свободно, оставляя след из искр. Потом вас страхует плавное падение. Перезарядка 60 секунд."),
    "blink_ring": (
        "Blink Ring", "Кольцо мерцания",
        "In the inventory: extra speed. Right-click: teleport up to 14 blocks along your gaze. Cooldown 5 seconds.",
        "В инвентаре: повышенная скорость. ПКМ: телепорт на 14 блоков по направлению взгляда. Перезарядка 5 секунд."),
    "vampiric_shard": (
        "Vampiric Shard", "Вампирический осколок",
        "In the inventory: every enemy you kill restores a quarter of its maximum health to you (up to 10).",
        "В инвентаре: каждый убитый вами враг возвращает вам четверть своего максимального здоровья (до 10)."),
    "berserker_core": (
        "Berserker Core", "Ядро берсерка",
        "Right-click: 20 seconds of Strength III, Speed II and Haste III. Afterwards a crash: weakness, slowness and hunger for 12 seconds. Cooldown 2 minutes.",
        "ПКМ: 20 секунд Силы III, Скорости II и Спешки III. Потом упадок сил: слабость, замедление и голод на 12 секунд. Перезарядка 2 минуты."),
    "stasis_crystal": (
        "Stasis Crystal", "Кристалл стазиса",
        "Right-click: every enemy within 12 blocks is frozen in time for 8 seconds and glows. Cooldown 45 seconds.",
        "ПКМ: все враги в радиусе 12 блоков замирают во времени на 8 секунд и подсвечиваются. Перезарядка 45 секунд."),
    "gravity_orb": (
        "Gravity Orb", "Сфера гравитации",
        "Right-click: pulls every enemy within 16 blocks to you and batters them. Cooldown 30 seconds.",
        "ПКМ: стягивает всех врагов в радиусе 16 блоков к вам и бьёт их. Перезарядка 30 секунд."),
    "crystal_totem": (
        "Crystal Totem", "Кристальный тотем",
        "In the inventory: once, it saves you from death, restores health and releases a shockwave that damages and pushes back enemies. It is used up.",
        "В инвентаре: один раз спасает от смерти, восстанавливает здоровье и выпускает ударную волну, ранящую и отбрасывающую врагов. Расходуется."),
    "thunder_shard": (
        "Thunder Shard", "Осколок грома",
        "Right-click: three lightning strikes land where you look, up to 48 blocks away. Cooldown 10 seconds.",
        "ПКМ: три удара молнии туда, куда вы смотрите, на расстоянии до 48 блоков. Перезарядка 10 секунд."),
    "purity_aegis": (
        'Aegis of Purity', 'Эгида чистоты',
        'A shield of light you carry in your inventory. Charge it there: pick up purified amethyst with the mouse and right-click the aegis with it (each piece is 10%, ten for a full charge; the bar under the icon shows the charge). While it is charged, a see-through dome surrounds you and you get a row of up to eight white hearts over your health: they take damage first and regrow on their own when you are not hit for three seconds. Every point they take or regrow uses up 1% of the charge; when it runs out, the dome and the white hearts disappear. Made from seven purified amethyst, a resonant shard and a diamond.',
        'Щит света, который носят в инвентаре. Заряжается там же: возьмите мышкой очищенный аметист и нажмите им ПКМ по эгиде (каждый кусок даёт 10%, десять для полного заряда; полоска под значком показывает заряд). Пока эгида заряжена, вас окружает прозрачный купол, а над здоровьем появляется ряд до восьми белых сердец: они принимают урон первыми и сами восстанавливаются, если вас три секунды не били. Каждая принятая или восстановленная единица тратит 1% заряда; когда он кончится, купол и белые сердца исчезнут. Делается из семи очищенных аметистов, резонирующего осколка и алмаза.'),
    "purity_amulet": (
        'Purity Amulet', 'Амулет чистоты',
        'A pale crystal on a silver chain. Carry it in your inventory: the amethyst blight cannot take hold of you, and creatures of the Amethyst deal 25% less damage (on top of purified armor). It cannot be crafted: look for it in the chests of the tower.',
        'Бледный кристалл на серебряной цепочке. Носите его в инвентаре: аметистовая порча не может вас поразить, а твари Аметиста наносят на 25% меньше урона (вдобавок к броне очищения). Скрафтить его нельзя: ищите в сундуках башни.'),
}
