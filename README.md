# Dawn of Mankind

A co-operative browser card game for 1–6 players. Together you build a civilization and try to see it through the
ages. This first version covers the **Prehistoric** and **Ancient** eras.

Java 21 · Spring Boot 4.1.2 · Maven · plain-JS page, no build step · games kept in memory.

## Running it

    run.ps1          (or run.bat)
    open http://localhost:8080/dawn/

Or build one jar and run it:

    mvn package
    java -jar dawn-app/target/dawn-of-mankind.jar

Create a room, share the 5-letter code (or the invite link), and start when everyone is in. The host can add
**AI players** in the lobby: they make simple moves on their own (about one per second), handy for testing. A player who closes the
page can come back to the same seat from the same browser; after 60 seconds away their turns are passed for them.

## Modules

| Module | What |
|---|---|
| `dawn-core` | The rules engine and all card data. Plain Java, no Spring. `Game` holds every rule; `View` is what one player may see. |
| `dawn-web` | Rooms, the REST API (`/dawn/api/…`), the WebSocket (`/dawn/ws`) and the page (`/dawn/`). Add this jar to any Spring Boot 4 app. |
| `dawn-app` | A small launcher to run the game on its own. |

To put the game into conlang-web, see [MERGING.md](MERGING.md).

## Changing the game

All cards, wonders, buildings and rule numbers are in
[`dawn-core/src/main/resources/dawn/content.yaml`](dawn-core/src/main/resources/dawn/content.yaml). New cards that
use the existing effects need no code. The effect vocabulary is listed at the top of that file and in `Effects.java`.

`mvn package` runs the tests, including 240 complete games played by simple bots (1–6 players), so a content change
that breaks a game shows up there. The bots' win rate is printed; it is a rough difficulty gauge, not a target.

## The rules as built

**Each player** starts with a Farm (2 farmers) and a Mine (1 miner): 3 Population. Every building has 2 worker
slots. Building a type you already have adds a level (2 more slots) instead of a second building, and each level
costs 1 production more than the last. Events that destroy a building take a level away. Growing costs 3 food,
+1 for every 4 people beyond the first.

**The realm** is the team's shared land, drawn from a **world deck** of territories (Plains, Mountains, Steppe,
Jungle, Coast, Forest, Ocean): 2 at the start. Each territory gives every player worker slots for land-bound
buildings (Plains: +2 farm slots, Mountains: +2 mine slots, …) and a bonus (Steppe: +2 military strength, Jungle:
+1 research per turn, Coast: +1 colonization value, …). Farms and mines can only grow as far as the land allows
(each player also has 2 home slots of each). Lands are settled by paying
their whole cost, bit by bit: resources (per player 2 food and 2 production, +1 production for each territory beyond
the first two, minus the realm's colonization value), troops equal to the land's **enemy strength** (half of the troops
sent are lost) and, for some lands, settlers. Two lands lie **face up** from the start (Send Scouts turns up more); they stay until settled or
removed by an event (Rivals Claim the Land), and are paid toward with **Settle** cards: each card lets its player pay
what they can now, and payments stay on the land. A settled one is replaced from the world deck. Lands **found by events** (Scouts Return, Explorers, Great Expedition, and the special Rich Land,
Fortified Land and Fertile Delta, which cost more) must be settled at once: everyone can pay toward them, free, in
that event's response window; if the cost isn't met when the event resolves, the land is lost and so is what was
sent. Hostile Tribes and A Warlord Rises raise the enemy strength of lands waiting to be
settled. **Fortify the Heartland** makes a territory a **core**: a broken invasion that would take it only removes its
core status. Farmers make 2 food, miners 2 production, temple workers 1 culture, village workers 1 research and 1
culture. Farm and Mine cost 2 production, Temple and Village 3. Each Population eats 1 food per round.

**A round**

1. **Production** for everyone; then each Population eats 1 food (unfed people starve, Stability −1).
2. **Turns**, in seat order from the First Player. A turn starts with a free blind card from the era deck, then
   **4 actions**: move a worker · build · pick a card from the queue · play a card · grow (+1 Population for 3 food)
   · train a troop (2 production) · contribute to a great wonder · lift a lasting event. Giving resources or cards
   to a teammate, discarding and using the shared pool are free. Hand limit 7.
3. **Events**: draw as many events from the event deck as the future deck holds, and resolve them one by one.
   Before each, anyone can play response cards; against an invasion everyone chooses how many troops to send.
   Resolved events go to the past events deck; then the future deck is shuffled into the event deck.
4. **End of round**: 1 / 2 / 3 cards (for 1–2 / 3–4 / 5–6 players) leave the bottom of the queue, the queue is
   refilled, and the First Player passes on.

**The queue** holds players + 4 cards. **Events never stay in a hand.** An event picked from the queue goes to the
(face-down) future deck and the picker gets the face-down card under it (another event there goes to the future deck
too, and they draw again). Blind-drawn events also go face down to the future deck. Events that leave the bottom of the queue go to the future deck and their face-down card
is discarded. The starting event deck holds 2 Prehistoric events per player.

**Raids** strike one player, who defends alone (army + raid defense + response cards). **Invasions** strike
everyone; their strength is players × territories plus the **enemies massing**, and each player decides how many
troops to send. **Hostility cards** (Prehistoric: 3 × +1, 3 × +2; Ancient: 2 × +2, 2 × +3, 1 × +5) trigger the moment
they are revealed from the era deck, adding to the enemies massing, which everyone can see; the next invasion brings
them all and the count starts again. An invasion
that breaks through takes a random territory: every player's buildings shrink to what the remaining land allows, and
a great wonder standing there is lost. **Crisis cards** (Black Plague, Supervolcano Eruption,
Meteor Impact, Climate Change) are added to the future deck by certain events and when a player's Population hits
0 (which also costs 1 Stability). Technologies can soften them.

**Eras** end when the era deck runs out or the era's trigger event resolves. Unfinished great wonders are lost then
(half is returned).

**The game** is lost if Stability reaches 0, everyone's Population is gone or the realm has no territory left, and
won by finishing the last era.
The score is the victory points of the **great wonders** built (Stonehenge, the Pyramids, the Hanging Gardens, the
Great Library, the Great Lighthouse, the Colossus), plus 1 per territory of the realm. Great wonders are cards in the era decks: whoever picks or draws one puts it on the
board, and everyone can contribute to it. Each costs about 4–5 rounds of the whole team's output (its cost is per
player) and, when finished, stands in a territory of the team's choice. The Epic (a minor wonder) is a card a player plays for themselves. Wonder
bonuses and points in content.yaml are placeholders.

### Choices made where the requirements were open (all easy to change)

- Grow, train a troop, contribute to a wonder and lift a lasting event are actions too (1 action each).
- Temple and Village produce **per worker**, like Farm and Mine.
- New Population goes straight into a free worker slot, otherwise it is idle.
- Raids and one-player events strike a random player.
- Great wonder bonuses apply to every player; Stonehenge's +1 action is for everyone. The Pyramids and the Great
  Lighthouse core the territory they stand in (or another one, if that is already a core). The Lighthouse's
  −2 production is off each land's whole settling cost.
- Fights: the troops that fight (up to the enemy's strength) must reach its strength, with response cards and
  defense bonuses adding to it; half of the troops spent are lost, win or lose. A lost raid also costs 1 Stability,
  a broken invasion 2 Stability and a territory.
- Curses (Bad Omen, Wrath of the Gods) also cost 1 Stability per round until lifted.
- Events left in the event decks carry over into the next era; an old era's trigger event is retired.
- Happiness is not in the game yet.
- Territory bonuses go to every player. Forest and Ocean bonuses are placeholders (you named those lands without
  bonuses). Steppe's +2 military strength counts for every raid defense and for the united army in invasions.
- Face-up lands stay across eras; enemy strength starts at 0 and only events raise it. A found land that isn't
  settled in time is discarded.
