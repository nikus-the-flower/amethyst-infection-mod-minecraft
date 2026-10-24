"""The story in the normal world: the altar's block models, the loot of the house and the ruins, and every
text of it (relics, pages of the story, the note, Ricardo's dialogue, the guide pages) in English, Russian
and Ukrainian. Writes en/ru into the language files and tools/_story_uk.json for lang_uk.py.
Run after gen_content.py and gen_dimension.py (gen_all.py does)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/amethystmod"
DATA = ROOT / "src/main/resources/data"
NS = "amethystmod"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def box(f, t, tex):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in ("north", "south", "east", "west", "up", "down")}}


# ---- the altar's blocks: carved stone ----------------------------------------------------------------------
pedestal = {"parent": "minecraft:block/block", "textures": {"particle": "minecraft:block/chiseled_stone_bricks",
                                                            "base": "minecraft:block/polished_andesite",
                                                            "pillar": "minecraft:block/chiseled_stone_bricks",
                                                            "top": "minecraft:block/smooth_stone"},
            "elements": [box([2, 0, 2], [14, 3, 14], "#base"), box([4, 3, 4], [12, 11, 12], "#pillar"),
                         box([3, 11, 3], [13, 14, 13], "#top")]}
altar = {"parent": "minecraft:block/block", "textures": {"particle": "minecraft:block/chiseled_stone_bricks",
                                                         "base": "minecraft:block/polished_andesite",
                                                         "pillar": "minecraft:block/chiseled_stone_bricks",
                                                         "top": "minecraft:block/calcite", "gold": "minecraft:block/gold_block"},
         "elements": [box([0, 0, 0], [16, 4, 16], "#base"), box([2, 4, 2], [14, 10, 14], "#pillar"),
                      box([1, 10, 1], [15, 14, 15], "#top"), box([0.5, 10, 7], [15.5, 11, 9], "#gold"),
                      box([7, 10, 0.5], [9, 11, 15.5], "#gold")]}
for name, model in (("altar_pedestal", pedestal), ("purification_altar", altar)):
    write(ASSETS / "models/block" / f"{name}.json", model)
    write(ASSETS / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    write(ASSETS / "models/item" / f"{name}.json", {"parent": f"{NS}:block/{name}"})


for kind in ("scroll", "ancient_parchment", "ancient_book", "diary", "torn_page", "letter"):
    write(ASSETS / "models/item" / f"{kind}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{kind}"}})


# ---- loot of the house and the ruins -----------------------------------------------------------------------
def entry(item, weight, lo=None, hi=None):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if lo is not None:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"min": lo, "max": hi}}]
    return e


def chest(name, pools):
    write(DATA / NS / "loot_table/chests" / f"{name}.json", {"type": "minecraft:chest", "pools": pools})


mc = lambda i: f"minecraft:{i}"
n = lambda i: f"{NS}:{i}"
chest("house", [
    {"rolls": {"min": 4, "max": 7}, "entries": [
        entry(mc("bread"), 10, 2, 5), entry(mc("baked_potato"), 10, 2, 6), entry(mc("apple"), 8, 1, 4),
        entry(mc("carrot"), 6, 2, 5), entry(mc("torch"), 8, 4, 12), entry(mc("coal"), 6, 2, 6),
        entry(mc("iron_ingot"), 3, 1, 3), entry(mc("wheat_seeds"), 4, 2, 6), entry(mc("string"), 3, 1, 4),
        entry(mc("stone_pickaxe"), 2), entry(mc("stone_sword"), 2), entry(mc("leather_chestplate"), 1),
        entry(mc("leather_boots"), 1), entry(n("country_potatoes"), 4, 1, 2)]}])
chest("ruins", [
    {"rolls": {"min": 3, "max": 6}, "entries": [
        entry(mc("bone"), 8, 1, 4), entry(mc("iron_ingot"), 6, 1, 4), entry(mc("gold_nugget"), 6, 2, 8),
        entry(mc("paper"), 6, 1, 5), entry(mc("book"), 3, 1, 2), entry(mc("arrow"), 5, 4, 12),
        entry(n("amethyst_essence"), 8, 2, 8), entry(n("resonant_shard"), 2), entry(n("purified_amethyst"), 2, 1, 2),
        entry(mc("bread"), 4, 1, 3), entry(mc("emerald"), 2, 1, 3)]}])
chest("crater", [
    {"rolls": {"min": 4, "max": 7}, "entries": [
        entry(n("amethyst_essence"), 10, 4, 12), entry(n("resonant_shard"), 4, 1, 2), entry(n("purified_amethyst"), 4, 1, 3),
        entry(mc("golden_apple"), 2), entry(n("crystal_arrow"), 5, 4, 12), entry(mc("gold_ingot"), 5, 1, 4),
        entry(mc("experience_bottle"), 3, 1, 4), entry(n("purified_crystal_bread"), 4, 1, 3), entry(mc("book"), 3, 1, 3)]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.25}],
     "entries": [entry(n(i), 1) for i in ("blink_ring", "vampiric_shard", "gravity_orb", "stasis_crystal")]}])
chest("castle", [
    {"rolls": {"min": 4, "max": 8}, "entries": [
        entry(mc("gold_ingot"), 8, 2, 6), entry(mc("diamond"), 4, 1, 3), entry(mc("iron_ingot"), 8, 2, 6),
        entry(n("amethyst_essence"), 8, 4, 12), entry(n("resonant_shard"), 5, 1, 3), entry(n("purified_amethyst"), 4, 1, 3),
        entry(mc("golden_apple"), 3), entry(mc("enchanted_golden_apple"), 1), entry(mc("emerald"), 4, 2, 6)]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.4}],
     "entries": [entry(n(i), 1) for i in ("aegis_heart", "amethyst_wings", "berserker_core", "thunder_shard", "crystal_totem",
                                          "flame_brand", "frost_edge", "storm_blade", "void_edge")]}])

# ---- texts: key -> (English, Russian, Ukrainian) -------------------------------------------------------------
T = {}


def t(key, en, ru, uk):
    T[key] = (en, ru, uk)


for item, en, ru, uk, ten, tru, tuk in (
        ("purity_shard", "Shard of Purity", "Осколок чистоты", "Уламок чистоти",
         "One of the three relics of the old altar.", "Одна из трёх реликвий древнего алтаря.", "Одна з трьох реліквій давнього вівтаря."),
        ("emperor_crown", "The Emperor's Crown", "Корона императора", "Корона імператора",
         "One of the three relics of the old altar.", "Одна из трёх реликвий древнего алтаря.", "Одна з трьох реліквій давнього вівтаря."),
        ("chaos_flask", "Flask of Chaos", "Колба хаоса", "Колба хаосу",
         "One of the three relics of the old altar.", "Одна из трёх реликвий древнего алтаря.", "Одна з трьох реліквій давнього вівтаря.")):
    t(f"item.{NS}.{item}", en, ru, uk)
    t(f"item.{NS}.{item}.tooltip", ten, tru, tuk)
for item, en, ru, uk in (("scroll", "Scroll", "Свиток", "Сувій"), ("ancient_parchment", "Ancient Parchment", "Древний пергамент", "Давній пергамент"),
                         ("ancient_book", "Ancient Book", "Древняя книга", "Давня книга"), ("diary", "Diary", "Дневник", "Щоденник"),
                         ("torn_page", "Torn Page", "Оторванная страница", "Відірвана сторінка"), ("letter", "Letter", "Письмо", "Лист")):
    t(f"item.{NS}.{item}", en, ru, uk)
t(f"block.{NS}.altar_pedestal", "Altar Pedestal", "Пьедестал алтаря", "П'єдестал вівтаря")
t(f"block.{NS}.purification_altar", "Altar of Purification", "Алтарь очищения", "Вівтар очищення")
t(f"entity.{NS}.ricardo", "Ricardo", "Рикардо", "Рікардо")
t(f"lore.{NS}.read", "Right-click to read", "ПКМ — читать", "ПКМ — читати")

FADED = ("The ink has faded with the years... only a few words can still be read.\n(The story of this page will be written soon.)",
         "Чернила выцвели от времени... можно разобрать лишь несколько слов.\n(История этой страницы скоро будет написана.)",
         "Чорнило вицвіло від часу... можна розібрати лише кілька слів.\n(Історію цієї сторінки скоро буде написано.)")
for lid, en, ru, uk in (
        ("ruins_tower", "Note from the Tower", "Записка из башни", "Записка з вежі"),
        ("ruins_house", "Page from a Chronicle", "Страница из летописи", "Сторінка з літопису"),
        ("crater_room", "Scroll of the Watchers", "Свиток стражей", "Сувій вартових"),
        ("crater_library", "The Book of Crystals", "Книга кристаллов", "Книга кристалів"),
        ("crater_diary", "Diary of an Explorer", "Дневник исследователя", "Щоденник дослідника"),
        ("crater_secret", "The Sealed Tome", "Запечатанный том", "Запечатаний том"),
        ("castle_throne", "Chronicle of the Emperor", "Хроника императора", "Хроніка імператора"),
        ("castle_letter", "A Letter to the Emperor", "Письмо императору", "Лист імператорові"),
        ("castle_parchment", "Decree of the Last Emperor", "Указ последнего императора", "Указ останнього імператора"),
        ("ricardo_journal", "Ricardo's Journal", "Дневник Рикардо", "Щоденник Рікардо")):
    t(f"lore.{NS}.{lid}.title", en, ru, uk)
    t(f"lore.{NS}.{lid}.text", *FADED)

# the note in the starting house
t(f"note.{NS}.title", "A Note", "Записка", "Записка")
t(f"note.{NS}.page1",
  "Traveller,\n\nif you are reading this, the Amethyst has come to this land too. I could not stay. Take what you need from this house: food, tools, a bed.\n\nThe answers lie in the old ruins.",
  "Путник,\n\nесли ты читаешь это, Аметист пришёл и в эти края. Я не смог остаться. Бери из этого дома всё, что нужно: еду, инструменты, кровать.\n\nОтветы лежат в старых руинах.",
  "Подорожній,\n\nякщо ти читаєш це, Аметист прийшов і в ці краї. Я не зміг лишитися. Бери з цього дому все, що треба: їжу, інструменти, ліжко.\n\nВідповіді лежать у старих руїнах.")
t(f"note.{NS}.page2",
  "Three places remember the old days:\n\nThe old square with the broken tower:\nX %s, Z %s\n\nThe sunken ruins and the altar:\nX %s, Z %s\n\nThe ruined castle:\nX %s, Z %s",
  "Три места помнят былые дни:\n\nСтарая площадь с разбитой башней:\nX %s, Z %s\n\nПодземные руины и алтарь:\nX %s, Z %s\n\nРазрушенный замок:\nX %s, Z %s",
  "Три місця пам'ятають давні дні:\n\nСтара площа з розбитою вежею:\nX %s, Z %s\n\nПідземні руїни й вівтар:\nX %s, Z %s\n\nЗруйнований замок:\nX %s, Z %s")
t(f"note.{NS}.page3",
  "Look for the three relics.\n\nAnd if you meet an old man with a lantern in the sunken ruins, tell him I sent you.\n\n- R.",
  "Ищи три реликвии.\n\nА если встретишь в подземных руинах старика с фонарём, скажи, что тебя прислал я.\n\n- Р.",
  "Шукай три реліквії.\n\nА якщо зустрінеш у підземних руїнах старого з ліхтарем, скажи, що тебе надіслав я.\n\n- Р.")

# the altar
t(f"message.{NS}.altar.pedestal", "Only an old relic can stand on this pedestal.", "На этот пьедестал можно поставить только древнюю реликвию.",
  "На цей п'єдестал можна поставити лише давню реліквію.")
t(f"message.{NS}.altar.cannot", "The altar cannot purify this.", "Алтарь не может очистить это.", "Вівтар не може очистити це.")
t(f"message.{NS}.altar.asleep", "The altar is silent: three relics must stand on its pedestals.",
  "Алтарь молчит: на его пьедесталах должны стоять три реликвии.", "Вівтар мовчить: на його п'єдесталах мають стояти три реліквії.")
t(f"message.{NS}.altar.awake", "The altar awakens! Lay an amethyst thing upon it.", "Алтарь пробуждается! Положите на него аметистовую вещь.",
  "Вівтар пробуджується! Покладіть на нього аметистову річ.")
t(f"message.{NS}.altar.awakened", "The altar has purified something for the first time. Ricardo will want to hear of it.",
  "Алтарь впервые что-то очистил. Рикардо захочет об этом услышать.", "Вівтар уперше щось очистив. Рікардо захоче про це почути.")

# Ricardo's dialogue
R = f"dialog.{NS}.ricardo."
t(R + "greet",
  "Ah, a visitor! Come in, come in, mind the rubble. I am Ricardo, a collector of old stories and older stones. What brings you down into this hole?",
  "А, гость! Заходи, заходи, осторожно, тут обломки. Я Рикардо, собиратель старых историй и ещё более старых камней. Что привело тебя в эту яму?",
  "А, гість! Заходь, заходь, обережно, тут уламки. Я Рікардо, збирач старих історій і ще старіших каменів. Що привело тебе в цю яму?")
t(R + "back", "Ask away, my friend. An old man has time.", "Спрашивай, друг мой. У старика есть время.", "Питай, друже. У старого є час.")
t(R + "who",
  "I was a scholar once, in a city that is now only crystal. When the Amethyst came, I took my books and my lantern and walked. I have walked for a very long time.",
  "Когда-то я был учёным, в городе, от которого теперь остался только кристалл. Когда пришёл Аметист, я взял книги, фонарь и пошёл. Иду я уже очень давно.",
  "Колись я був ученим, у місті, від якого тепер лишився тільки кристал. Коли прийшов Аметист, я взяв книжки, ліхтар і пішов. Іду я вже дуже давно.")
t(R + "who_more",
  "Why here? Because this altar was built long ago to fight the Amethyst. I read of it in a book older than my grandfather. If it still works, perhaps this world can be healed.",
  "Почему здесь? Потому что этот алтарь построили давным-давно, чтобы бороться с Аметистом. Я читал о нём в книге старше моего деда. Если он ещё работает, может, этот мир ещё можно исцелить.",
  "Чому тут? Бо цей вівтар збудували давним-давно, щоб боротися з Аметистом. Я читав про нього в книжці, старшій за мого діда. Якщо він ще працює, може, цей світ ще можна зцілити.")
t(R + "place",
  "These are the sunken ruins: once a temple of the Watchers, who studied the crystal and tried to tame it. The crater came later, when something fell from the sky... or rose from below. The rooms around still keep their books.",
  "Это подземные руины: когда-то храм Стражей, которые изучали кристалл и пытались его приручить. Кратер появился позже, когда что-то упало с неба... или поднялось снизу. В комнатах вокруг ещё хранятся их книги.",
  "Це підземні руїни: колись храм Вартових, які вивчали кристал і намагалися його приборкати. Кратер з'явився пізніше, коли щось упало з неба... чи піднялося знизу. У кімнатах довкола ще зберігаються їхні книжки.")
t(R + "altar_quest",
  "The altar needs three relics: the Shard of Purity, the Emperor's Crown and the Flask of Chaos. One lies in the old square with the broken tower, one in the castle, and one somewhere here, behind a wall that is not quite a wall. Set them on the pedestals and lay something amethyst on the altar. Do that, and I will have a gift for you.",
  "Алтарю нужны три реликвии: Осколок чистоты, Корона императора и Колба хаоса. Одна лежит на старой площади с разбитой башней, другая в замке, а третья где-то здесь, за стеной, которая не совсем стена. Поставь их на пьедесталы и положи на алтарь что-нибудь аметистовое. Сделаешь это, и у меня будет для тебя подарок.",
  "Вівтареві потрібні три реліквії: Уламок чистоти, Корона імператора й Колба хаосу. Одна лежить на старій площі з розбитою вежею, друга в замку, а третя десь тут, за стіною, яка не зовсім стіна. Постав їх на п'єдестали й поклади на вівтар щось аметистове. Зробиш це, і в мене буде для тебе подарунок.")
t(R + "altar_done",
  "You did it! I felt it from my tent: the altar sings again. For years I dreamed of this. Here, take this, it is the least I can give you.",
  "Ты сделал это! Я почувствовал это даже в палатке: алтарь снова поёт. Годами я мечтал об этом. Вот, возьми, это меньшее, чем я могу тебя отблагодарить.",
  "Ти зробив це! Я відчув це навіть у наметі: вівтар знову співає. Роками я мріяв про це. Ось, візьми, це найменше, чим я можу тобі віддячити.")
t(R + "gift",
  "This amulet kept me alive on the road: the blight cannot touch whoever carries it. And the cereal... well, an old man's weakness. Eat it with milk!",
  "Этот амулет хранил меня в дороге: порча не тронет того, кто его носит. А хлопья... ну, слабость старика. Ешь их с молоком!",
  "Цей амулет беріг мене в дорозі: порча не торкнеться того, хто його носить. А пластівці... ну, слабкість старого. Їж їх із молоком!")
t(R + "altar_thanks",
  "The altar works and the blight cannot touch you. What more could an old scholar wish for? Well... perhaps another bowl of cereal.",
  "Алтарь работает, и порча тебе не страшна. Чего ещё желать старому учёному? Ну... разве что ещё одну миску хлопьев.",
  "Вівтар працює, і порча тобі не страшна. Чого ще бажати старому вченому? Ну... хіба що ще одну миску пластівців.")
for opt, en, ru, uk in (("who", "Who are you?", "Кто ты?", "Хто ти?"), ("who_more", "Why are you here?", "Почему ты здесь?", "Чому ти тут?"),
                        ("place", "What is this place?", "Что это за место?", "Що це за місце?"), ("altar", "About the altar", "Про алтарь", "Про вівтар"),
                        ("trade", "Let's trade", "Давай торговать", "Поторгуймо"), ("bye", "Goodbye", "До свидания", "До побачення"),
                        ("back", "Something else...", "Ещё вопрос...", "Ще питання..."), ("accept", "Accept the gift", "Принять подарок", "Прийняти подарунок"),
                        ("thanks", "Thank you!", "Спасибо!", "Дякую!")):
    t(R + "opt." + opt, en, ru, uk)

# the guide
G = f"guide.{NS}."
t(G + "ruins.title", "Ruins of the Old World", "Руины старого мира", "Руїни старого світу")
t(G + "ruins.text",
  "A new world begins in a small village house, between 250 and 1200 blocks from the tower, like all the places of the story. On its lectern lies a note with the coordinates of three ruins. The old square has a broken tower and ruined houses; the Shard of Purity lies in the tower. The sunken ruins are a crater ten blocks deep: go down the spiral stair along its wall, or jump from the broken bridge into the pool below. Under the ground are ruined rooms, and in the middle stands the old altar, where Ricardo camps. Somewhere a cracked wall hides the Flask of Chaos. The ruined castle keeps the Emperor's Crown in its throne room, guarded by a colossus. The ruins are built when you first come near. Pages of the story lie in their chests: right-click to read them. Ops: /amethyst sites lists the places, /amethyst sites tp <house|ruins|crater|castle> goes there.",
  "Новый мир начинается в небольшом деревенском доме, в 250–1200 блоках от башни, как и все места истории. На его кафедре лежит записка с координатами трёх руин. На старой площади разбитая башня и разрушенные дома; в башне лежит Осколок чистоты. Подземные руины это кратер глубиной десять блоков: спуститесь по винтовой лестнице вдоль его стены или прыгните с разрушенного моста в пруд внизу. Под землёй разрушенные комнаты, а в центре стоит древний алтарь, у которого живёт Рикардо. Где-то треснувшая стена прячет Колбу хаоса. Разрушенный замок хранит Корону императора в тронном зале под охраной колосса. Руины строятся, когда вы впервые подходите к ним. В их сундуках лежат страницы истории: ПКМ, чтобы прочитать. Операторы: /amethyst sites показывает места, /amethyst sites tp <house|ruins|crater|castle> переносит туда.",
  "Новий світ починається в невеликому сільському будинку, за 250–1200 блоків від вежі, як і всі місця історії. На його кафедрі лежить записка з координатами трьох руїн. На старій площі розбита вежа й зруйновані будинки; у вежі лежить Уламок чистоти. Підземні руїни це кратер завглибшки десять блоків: спустіться гвинтовими сходами вздовж його стіни або стрибніть зі зруйнованого мосту в ставок унизу. Під землею зруйновані кімнати, а посередині стоїть давній вівтар, біля якого живе Рікардо. Десь тріснута стіна ховає Колбу хаосу. Зруйнований замок зберігає Корону імператора в тронній залі під охороною колоса. Руїни будуються, коли ви вперше підходите до них. У їхніх скринях лежать сторінки історії: ПКМ, щоб прочитати. Оператори: /amethyst sites показує місця, /amethyst sites tp <house|ruins|crater|castle> переносить туди.")
t(G + "altar.title", "The Altar of Purification", "Алтарь очищения", "Вівтар очищення")
t(G + "altar.text",
  "In the middle of the sunken ruins stands the old altar with three pedestals around it. Right-click a pedestal with a relic to set it there (the Shard of Purity, the Emperor's Crown, the Flask of Chaos); with an empty hand to take it back. When all three stand in place, light runs from them to the altar and it wakes. Then right-click the altar with an amethyst thing: it rises, spins, and after a few seconds it is purified. The relics are not used up. It purifies: amethyst essence into purified amethyst (a whole stack at once), each piece of amethyst armor into purifier's armor, the amethyst sword into the Purifier's Blade, the Spear of Amitariel into the Purified Crystal Spear. Enchantments stay.",
  "В центре подземных руин стоит древний алтарь с тремя пьедесталами вокруг. ПКМ по пьедесталу с реликвией в руке ставит её туда (Осколок чистоты, Корона императора, Колба хаоса); пустой рукой её можно забрать. Когда все три на месте, от них к алтарю бежит свет, и он пробуждается. Тогда ПКМ по алтарю с аметистовой вещью: она поднимается, вращается и через несколько секунд становится очищенной. Реликвии не тратятся. Алтарь очищает: аметистовую эссенцию в очищенный аметист (сразу целую стопку), каждую часть аметистовой брони в броню очищения, аметистовый меч в Клинок очищения, Копьё Амитариэля в Очищенное кристальное копьё. Зачарования сохраняются.",
  "Посеред підземних руїн стоїть давній вівтар із трьома п'єдесталами довкола. ПКМ по п'єдесталу з реліквією в руці ставить її туди (Уламок чистоти, Корона імператора, Колба хаосу); порожньою рукою її можна забрати. Коли всі три на місці, від них до вівтаря біжить світло, і він пробуджується. Тоді ПКМ по вівтарю з аметистовою річчю: вона піднімається, обертається й за кілька секунд стає очищеною. Реліквії не витрачаються. Вівтар очищує: аметистову есенцію в очищений аметист (одразу цілу стопку), кожну частину аметистової броні в броню очищення, аметистовий меч у Клинок очищення, Спис Амітаріеля в Очищений кристальний спис. Зачарування зберігаються.")
t(G + "ricardo.title", "Ricardo", "Рикардо", "Рікардо")
t(G + "ricardo.text",
  "An old travelling scholar who camps by the altar in the sunken ruins. Right-click him to talk: choose your answers in the dialogue window. He trades: amethyst essence for gold or diamonds, and food, crystal arrows, the guide, his own journal and the simpler artifacts (blink ring, vampiric shard, gravity orb, stasis crystal) for essence. Wake the altar and talk to him again: he gives you the Purity Amulet. He cannot be hurt.",
  "Старый странствующий учёный, который живёт у алтаря в подземных руинах. ПКМ, чтобы поговорить: ответы выбираются в окне диалога. Он торгует: аметистовую эссенцию за золото или алмазы, а за эссенцию еду, кристальные стрелы, справочник, свой дневник и простые артефакты (кольцо мерцания, вампирический осколок, сферу гравитации, кристалл стазиса). Пробудите алтарь и поговорите с ним снова: он подарит Амулет чистоты. Ранить его нельзя.",
  "Старий мандрівний учений, який живе біля вівтаря в підземних руїнах. ПКМ, щоб поговорити: відповіді обираються у вікні діалогу. Він торгує: аметистову есенцію за золото чи алмази, а за есенцію їжу, кристальні стріли, довідник, свій щоденник і прості артефакти (перстень мерехтіння, вампірський уламок, сферу гравітації, кристал стазису). Пробудіть вівтар і поговоріть із ним знову: він подарує Амулет чистоти. Поранити його не можна.")

# ---- write ----
LANG = ASSETS / "lang"
for name, idx in (("en_us.json", 0), ("ru_ru.json", 1)):
    path = LANG / name
    lang = json.loads(path.read_text(encoding="utf-8"))
    for k, v in T.items():
        lang[k] = v[idx]
    write(path, lang)
write(ROOT / "tools/_story_uk.json", {k: v[2] for k, v in T.items()})
print("story written")
