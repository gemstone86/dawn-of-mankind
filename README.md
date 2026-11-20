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

Create a room, share the 5-letter code (or the invite link), and start when everyone is in. A player who closes the
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
slots. Farmers make 2 food, miners 2 production, temple workers 1 culture, village workers 1 research and 1
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

**Events never stay in a hand.** An event picked from the queue goes to the future deck and the picker gets the
face-down card under it (another event there goes to the future deck too, and they draw again). Blind-drawn events
also go to the future deck. Events that leave the bottom of the queue go to the future deck and their face-down card
is discarded. The starting event deck holds 2 Prehistoric events per player.

**Raids** strike one player, who defends alone (army + raid defense + response cards). **Invasions** strike
everyone; each player decides how many troops to send. **Crisis cards** (Black Plague, Supervolcano Eruption,
Meteor Impact, Climate Change) are added to the future deck by certain events and when a player's Population hits
0 (which also costs 1 Stability). Technologies can soften them.

**Eras** end when the era deck runs out or the era's trigger event resolves. Unfinished great wonders are lost then
(half is returned).

**The game** is lost if Stability reaches 0 or everyone's Population is gone, and won by finishing the last era.
The score is the victory points of the **great wonders** built (Stonehenge, the Pyramids, the Hanging Gardens, the
Great Library, the Colossus). Their bonuses and points in content.yaml are placeholders.

### Choices made where the requirements were open (all easy to change)

- Grow, train a troop, contribute to a wonder and lift a lasting event are actions too (1 action each).
- Temple and Village produce **per worker**, like Farm and Mine.
- New Population goes straight into a free worker slot, otherwise it is idle.
- Raids and one-player events strike a random player.
- Great wonder bonuses apply to every player; Stonehenge's +1 action is for everyone.
- A repelled raid of strength S costs S/3 troops; a lost one costs half the defenders and 1 Stability.
  A repelled invasion costs S/2 troops split between senders; a lost one costs every sent troop and 2 Stability.
- Curses (Bad Omen, Wrath of the Gods) also cost 1 Stability per round until lifted.
- Events left in the event decks carry over into the next era; an old era's trigger event is retired.
- Happiness is not in the game yet.
