# Putting Dawn of Mankind into conlang-web

`dawn-web` is a normal jar: its controllers, WebSocket and page sit under `/dawn/`, in the package
`com.dawnofmankind.web`, so nothing clashes with `/rune/`, `/weather/` or `/api/`. Both projects use the same
Spring Boot parent (4.1.2) and Java 21.

## 1. Make the jar available

conlang-web is built by GitHub Actions, so the simplest way is to build Dawn of Mankind first in the same job.
In `.github/workflows/build.yml` and `deploy.yml`, before `mvn -B package`:

```yaml
      - uses: actions/checkout@v4
        with:
          repository: gemstone86/dawn-of-mankind
          path: dawn
      - run: mvn -B -f dawn/pom.xml install -DskipTests
```

Locally, run `mvn install` once in the dawn-of-mankind folder (and again after pulling changes).

(Later, if you prefer: publish to GitHub Packages, or move the two modules into conlang-web as Maven modules.)

## 2. conlang-web's pom.xml

```xml
<dependency>
  <groupId>com.dawnofmankind</groupId>
  <artifactId>dawn-web</artifactId>
  <version>0.1.0</version>
</dependency>
```

This brings `spring-boot-starter-websocket` with it.

## 3. Let Spring find it

In `com.hub.HubApplication`:

```java
@SpringBootApplication(scanBasePackages = {"com.hub", "com.conlangs.web", "com.eonweather.web", "com.dawnofmankind.web"})
```

## 4. Use the shared login (optional, recommended)

Without this everyone plays as a guest and types a name. With it, logged-in players get their account name.
Add a bean anywhere in `com.hub`, e.g. in `HubConfig`:

```java
@Bean
public com.dawnofmankind.web.DawnIdentity dawnIdentity(com.conlangs.web.Auth auth) {
    return req -> {
        var user = auth.current(req.getSession(false));
        return java.util.Optional.ofNullable(user == null ? null : String.valueOf(user.get("username")));
    };
}
```

## 5. Protect the API like the others

In `com.hub.SameOriginFilter`, add `/dawn/api/` to the paths it checks:

```java
boolean api = path.startsWith("/api/") || path.startsWith("/rune/api/") || path.startsWith("/weather/api/")
        || path.startsWith("/dawn/api/");
```

The WebSocket already accepts only pages from the same site.

## 6. Link it from the start page

Add a third app tile on `static/index.html` pointing to `/dawn/`.

## 7. Azure

Turn on **Web sockets** for the App Service (Configuration → General settings → Web sockets: On). Without it the
page loads but rooms never connect.

Running games live in memory: every deploy (each merge to main) ends the games in progress.

## Settings

| Property | Default | |
|---|---|---|
| `dawn.away-grace-seconds` | 60 | after this long disconnected, a player's turns and responses are passed for them |
| `dawn.idle-room-minutes` | 30 | a room nobody is connected to is removed after this long |
| `dawn.finished-room-minutes` | 10 | the same, for a finished game |
| `dawn.ai-move-millis` | 900 | pause before each move of an AI player |
